package com.vk.compiler.plugin.compose.test.tag.applier

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.isAnonymous
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.popLast

internal class TestTagApplier(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private val applyTagFunction by lazy {
        pluginContext.referenceFunctions(applyTafCallableId).firstOrNull()?.owner?.symbol
    }

    private val functionsStack = mutableListOf<IrFunction>()
    private val filesStack = mutableListOf<IrFile>()

    override fun visitFile(declaration: IrFile): IrFile {
        filesStack += declaration
        val result = super.visitFile(declaration)
        filesStack.popLast()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        functionsStack += declaration
        val result = super.visitFunction(declaration)
        functionsStack.popLast()
        return result
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val applyTagFunction = applyTagFunction
        val lastFile = filesStack.lastOrNull()
        val lastFunction = functionsStack.lastOrNull()?.searchNotAnonymous()

        if (applyTagFunction != null && lastFile != null && lastFunction != null && expression.symbol.owner.isComposable()) {
            expression.transformModifierArguments(applyTagFunction, lastFile, lastFunction)
        }
        return super.visitCall(expression)
    }

    private fun IrCall.transformModifierArguments(
        applyTagFunction: IrSimpleFunctionSymbol,
        lastFile: IrFile,
        lastFunction: IrFunction
    ): IrCall {

        for(index in 0 until valueArgumentsCount) {
            val parameter = symbol.owner.valueParameters[index]
            val argumentExpression = retrieveArgumentExpression(parameter, getValueArgument(index))

            if (argumentExpression == null || argumentExpression.containsModifierWithTestTag() || parameter.isVararg || !parameter.isMainComposeModifier()) {
                continue
            }

            val modifiedArgumentExpression = createTestTaggedArgument(
                parameter,
                argumentExpression,
                createTag(this, lastFile, lastFunction),
                applyTagFunction
            )
            modifiedArgumentExpression?.let { putValueArgument(index, it) }
        }
        return this
    }

    private fun createTestTaggedArgument(
        parameter: IrValueParameter,
        argumentExpression: IrExpression,
        tag: String,
        applyTagFunction: IrSimpleFunctionSymbol
    ): IrExpression? {

        // Column() -> Column(<default-value>.applyTestTag(tag))
        // Column(Modifier) -> Column(Modifier.applyTestTag(tag))
        // Column(Modifier.fillMaxSize()) -> Column(Modifier.applyTestTag(tag).fillMaxSize())

        // Column(CustomModifier) -> skip
        // Column(modifier) -> skip
        // Column(modifier.fillMaxSize()) -> skip

        if (argumentExpression is IrCall) {
            val topmostReceiverCall = argumentExpression.getTopmostReceiverCall()
            val topmostReceiverObject =
                topmostReceiverCall.extensionReceiver ?: topmostReceiverCall.dispatchReceiver

            if (topmostReceiverObject?.type?.isComposeModifierObject() != true) return null

            val applyTagCall = IrCallImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = parameter.type,
                symbol = applyTagFunction,
                typeArgumentsCount = 0,
                valueArgumentsCount = 1,
            ).apply {
                extensionReceiver = topmostReceiverObject
                putValueArgument(0, tag.toIrConst(pluginContext.irBuiltIns.stringType))
            }

            // if top function is declared in Modifier
            if (topmostReceiverCall.dispatchReceiver == topmostReceiverObject) {
                topmostReceiverCall.dispatchReceiver = applyTagCall
            } else {
                // if top function is extension for Modifier
                topmostReceiverCall.extensionReceiver = applyTagCall
            }
            return null
        }

        if (argumentExpression is IrGetObjectValue && argumentExpression.type.isComposeModifierObject()) {
            val applyTagCall = IrCallImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = parameter.type,
                symbol = applyTagFunction,
                typeArgumentsCount = 0,
                valueArgumentsCount = 1,
            ).apply {
                extensionReceiver = argumentExpression
                putValueArgument(0, tag.toIrConst(pluginContext.irBuiltIns.stringType))
            }
            return applyTagCall
        }

        return null

    }


    private fun IrCall.getTopmostReceiverCall(): IrCall =
        when (val receiver = extensionReceiver ?: dispatchReceiver) {
            is IrCall -> receiver.getTopmostReceiverCall()
            else -> this
        }

    private fun IrFunction.isComposable(): Boolean {
        return hasAnnotation(Composable)
    }

    private fun IrValueParameter.isMainComposeModifier(): Boolean {
        return name.asString() == "modifier" && type.isComposeModifier()
    }

    private fun IrType.isComposeModifier(): Boolean {
        return classFqName?.asString() == MODIFIER
    }

    private fun IrType.isComposeModifierObject(): Boolean =
        classFqName?.asString() == MODIFIER_COMPANION

    private fun IrFunction.searchNotAnonymous(): IrFunction? {
        if (!name.isAnonymous) return this

        val parent = parent
        if (parent is IrFunction) return parent.searchNotAnonymous()

        return null
    }

    private fun IrExpression.containsModifierWithTestTag(): Boolean {
        return this.dump(true).contains(testTagRegex)
    }

    private fun createTag(
        irCall: IrCall,
        lastFile: IrFile,
        lastFunction: IrFunction,
    ) = "${lastFile.name}-" +
            "${lastFunction.name}(${lastFunction.startOffset})-" +
            "${irCall.symbol.owner.name}(${irCall.startOffset})"

    private fun retrieveArgumentExpression(
        param: IrValueParameter,
        value: IrExpression?
    ): IrExpression? =
        when {
            value == null -> param.defaultValue?.expression?.takeIf { it.isValidValueExpression() }
            value.isValidValueExpression() -> value
            else -> null
        }

    private fun IrExpression.isValidValueExpression(): Boolean {
        return this is IrCall || this is IrGetObjectValue
    }


    private companion object {
        const val MODIFIER = "androidx.compose.ui.Modifier"
        const val MODIFIER_COMPANION = "${MODIFIER}.Companion"
        val Composable = FqName( "androidx.compose.runtime.Composable")
        val testTagRegex = "applyTestTag|testTag".toRegex()
        val applyTafCallableId =
            CallableId(FqName("com.vk.compose.test.tag"), Name.identifier("applyTestTag"))
    }
}