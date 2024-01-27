package com.vk.compiler.plugin.composable.skippability.checker.ir

import com.vk.compiler.plugin.composable.skippability.checker.COMPOSE_PACKAGE_NAME
import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassName.Composable
import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassName.Composer
import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassName.ExplicitGroupsComposable
import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassName.NonRestartableComposable
import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassName.NonSkippableComposable
import com.vk.compiler.plugin.composable.skippability.checker.Keys.NON_SKIPPABLE_COMPOSABLE
import com.vk.compiler.plugin.composable.skippability.checker.Keys.SUPPRESS
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.SpecialNames

internal class SkippableFunctionsCollector(
    private val unskippableFunctions: MutableSet<ReportFunction>,
    private val fixedSuppressedFunctions: MutableSet<ReportFunction>,
    private val stabilityInferencer: StabilityInferencer
) : IrElementVisitorVoid {

    private var currentFunction: FunctionInfo? = null

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSetValue(expression: IrSetValue) {
        iterateOverParamsInFunctionChain(expression) { info, index ->
            ++info.paramsSetCount[index]
        }
        super.visitSetValue(expression)
    }

    override fun visitGetValue(expression: IrGetValue) {
        iterateOverParamsInFunctionChain(expression) { info, index ->
            val usedCount = ++info.paramsGetCount[index]
            info.usedParams[index] = usedCount > 1 || !info.function.isRestartable()
        }
        super.visitGetValue(expression)
    }

    override fun visitFunction(declaration: IrFunction) {
        currentFunction = FunctionInfo(declaration, currentFunction)
        super.visitFunction(declaration)

        val functionInfo = currentFunction
        currentFunction = currentFunction?.parent

        if (functionInfo == null || functionInfo.mayBeSkippable().not()) return

        val function = functionInfo.function
        val nonSkippableParams = mutableSetOf<String>()

        functionInfo.params.forEachIndexed { paramIndex, param ->
            val isRequired = functionInfo.paramsSetCount[paramIndex] < 1
            val stability = stabilityInferencer.stabilityOf(param.varargElementType ?: param.type)
            val isUnstable = stability.knownUnstable()
            val isUsed = functionInfo.usedParams[paramIndex]
            val paramType = functionInfo.paramsTypes[paramIndex]
            val isFromCompose = param.type.classFqName?.startsWith(COMPOSE_PACKAGE_NAME) == true

            if (!isFromCompose && isUsed && isUnstable && isRequired) {
                nonSkippableParams += "(name=${param.name.asString()}, type=$paramType, class=${param.type.classFqName?.asString()}, reason=${stability})"
            }
        }

        val suppressAnnotation = function.annotations.any { annotation ->
            val argumentsRange = 0 until annotation.valueArgumentsCount
            annotation.symbol.owner.parentAsClass.name.asString().contains(SUPPRESS)
                    && argumentsRange.any { annotation.getValueArgument(it)?.dumpKotlinLike().orEmpty().contains(NON_SKIPPABLE_COMPOSABLE) }
        }

        when {
            !suppressAnnotation && nonSkippableParams.isNotEmpty() -> unskippableFunctions += ReportFunction(function.kotlinFqName.asString(), nonSkippableParams)
            suppressAnnotation && nonSkippableParams.isEmpty() -> fixedSuppressedFunctions += ReportFunction(function.kotlinFqName.asString())
        }

    }

    private fun FunctionInfo.mayBeSkippable() = hasComposer && function.isRestartable() && !function.hasNonSkippableComposableAnnotation

    private inline fun iterateOverParamsInFunctionChain(
        expression: IrValueAccessExpression,
        block: (info: FunctionInfo, index: Int) -> Unit
    ) {
        var info = currentFunction
        val declaration = expression.symbol.owner as? IrValueParameter ?: return
        val parentFunction = declaration.parent
        while (info != null) {
            if (info.function == parentFunction) {
                val index = info.params.indexOf(declaration)
                if (index != -1) block(info, index)
                break
            }
            info = info.parent
        }
    }


    private fun IrFunction.isRestartable(): Boolean = when {
        body == null || this !is IrSimpleFunction -> false
        isLocal && parentClassOrNull?.origin != JvmLoweredDeclarationOrigin.LAMBDA_IMPL -> false
        isInline -> false
        hasNonRestartableAnnotation -> false
        hasExplicitGroupsAnnotation -> false
        isLambda -> false
        !returnType.isUnit() -> false
        isComposableDelegatedAccessor -> false
        composerParam() == null -> false
        else -> origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
    }


    private val IrFunction.hasNonRestartableAnnotation: Boolean
        get() = hasAnnotation(NonRestartableComposable)

    private val IrFunction.hasNonSkippableComposableAnnotation: Boolean
        get() = hasAnnotation(NonSkippableComposable)

    private val IrFunction.hasExplicitGroupsAnnotation: Boolean
        get() = hasAnnotation(ExplicitGroupsComposable)

    private val IrFunction.hasComposableAnnotation: Boolean
        get() = hasAnnotation(Composable)

    private fun IrFunction.composerParam(): IrValueParameter? {
        for (param in valueParameters.asReversed()) {
            if (param.isComposerParam()) return param
            if (!param.name.asString().startsWith('$')) return null
        }
        return null
    }

    private fun IrValueParameter.isComposerParam(): Boolean =
        name.asString() == "\$composer" && type.classFqName == Composer


    private val IrFunction.isComposableDelegatedAccessor: Boolean
        get() = origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR &&
                body?.let {
                    val returnStatement = it.statements.singleOrNull() as? IrReturn
                    val callStatement = returnStatement?.value as? IrCall
                    val target = callStatement?.symbol?.owner
                    target?.hasComposableAnnotation
                } == true

    private val IrFunction.isLambda: Boolean
        get() = name == SpecialNames.ANONYMOUS

    private class FunctionInfo(val function: IrFunction, val parent: FunctionInfo? = null) {

        private var realValueParameterCount = 0

        var hasComposer = false
            private set

        init {
            for (param in function.valueParameters) {
                val paramName = param.name.asString()
                val isRealParam = !(paramName == "\$composer"
                        || paramName.startsWith("\$default")
                        || paramName.startsWith("\$changed")
                        || paramName.startsWith("\$context_receiver_")
                        || paramName.startsWith("\$name\$for\$destructuring")
                        || paramName.startsWith("\$noName_")
                        || paramName == "\$this")

                if (paramName == "\$composer") hasComposer = true
                if (isRealParam) realValueParameterCount++
            }
        }

        private val allValueParameters = function.valueParameters.take(function.contextReceiverParametersCount + realValueParameterCount)
        private val contextValueParameters = allValueParameters.take(function.contextReceiverParametersCount)
        private val declaredValueParameters = allValueParameters.drop(function.contextReceiverParametersCount).take(realValueParameterCount)

        val params = buildList {
            function.extensionReceiverParameter?.let(::add)
            addAll(allValueParameters)
            function.dispatchReceiverParameter?.let(::add)
        }

        val paramsTypes = buildList {
            if(function.extensionReceiverParameter != null) add(ParameterType.Extension)
            repeat(contextValueParameters.size) { add(ParameterType.Context) }
            repeat(declaredValueParameters.size) { add(ParameterType.Parameter) }
            if(function.dispatchReceiverParameter != null) add(ParameterType.Dispatch)
        }

        val usedParams = BooleanArray(params.size) { false }
        val paramsGetCount = IntArray(params.size) { 0 }
        val paramsSetCount = IntArray(params.size) { 0 }

        enum class ParameterType {
            Extension,
            Dispatch,
            Context,
            Parameter
        }
    }

}