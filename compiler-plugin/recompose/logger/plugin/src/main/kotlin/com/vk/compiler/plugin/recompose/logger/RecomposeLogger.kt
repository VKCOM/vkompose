@file:OptIn(DeprecatedForRemovalCompilerApi::class)

package com.vk.compiler.plugin.recompose.logger

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.isAnonymous
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.util.concurrent.atomic.AtomicInteger

internal class RecomposeLogger(
    pluginContext: IrPluginContext,
    logModifierChanges: Boolean,
    logFunctionChanges: Boolean
) : IrElementTransformerVoid() {

    private val bodyTransformer = BodyCallsStatementsTransformer(pluginContext, logModifierChanges, logFunctionChanges)

    private var currentFunction: FunctionInfo? = null
    private val filesStack = mutableListOf<IrFile>()

    override fun visitFile(declaration: IrFile): IrFile {
        filesStack += declaration
        val result = super.visitFile(declaration)
        filesStack.popLast()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentFunction = FunctionInfo(declaration, currentFunction)
        val result = super.visitFunction(declaration)
        currentFunction = currentFunction?.parent
        return result
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {

        val lastFile = filesStack.lastOrNull()
        val lastFunctionInfo = currentFunction?.searchNotAnonymous()

        if (lastFile == null || lastFunctionInfo == null) return super.visitBlockBody(body)

        val lastFunction = lastFunctionInfo.function
        if (lastFunction.name.isSpecial || lastFunction.isInline) return super.visitBlockBody(body)

        val transformedBody = bodyTransformer.transform(
            body,
            lastFunction,
            createPrefixForRecomposeFunction(lastFile, lastFunctionInfo),
        )

        return super.visitBlockBody(transformedBody)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val lastFunction = currentFunction?.searchNotAnonymous()

        val functionOwner = expression.symbol.owner
        val callFunctionName = functionOwner.name

        if (!callFunctionName.isAnonymous && !callFunctionName.isSpecial && !expression.isRecomposeLoggerFunction()) {
            lastFunction?.calledFunctionNamesStack?.add(callFunctionName.asString())
        }

        val result = super.visitCall(expression)

        lastFunction?.calledFunctionNamesStack?.removeLastOrNull()

        return result
    }


    private fun createPrefixForRecomposeFunction(file: IrFile, functionInfo: FunctionInfo): String =
        buildString {
            append(file.name)
            append(":")
            append(functionInfo.function.name)
            functionInfo.calledFunctionNamesStack.forEach { append(":${it}") }
        }


    private class FunctionInfo(
        val function: IrFunction,
        val parent: FunctionInfo? = null,
    ) {
        val calledFunctionNamesStack: MutableList<String> = mutableListOf()

        fun searchNotAnonymous(): FunctionInfo? {
            if (!function.name.isAnonymous) return this

            return parent?.searchNotAnonymous()
        }
    }

    private class BodyCallsStatementsTransformer(
        private val pluginContext: IrPluginContext,
        private val logModifierChanges: Boolean,
        private val logFunctionChanges: Boolean
    ) {
        private val counter = AtomicInteger()

        private val loggerFunctionSymbol by lazy {
            pluginContext.referenceFunctions(recomposeLoggerCallableId).firstOrNull()?.owner?.symbol
        }

        fun transform(
            body: IrBlockBody,
            outerFunction: IrFunction,
            prefixLogName: String
        ): IrBlockBody {
            val loggerFunction = loggerFunctionSymbol ?: return body
            body.addLoggers(outerFunction, loggerFunction, prefixLogName)
            return body
        }

        private fun IrBlockBody.addLoggers(
            outerFunction: IrFunction,
            loggerFunction: IrSimpleFunctionSymbol,
            prefixLogName: String
        ) {
            val modifiedStatements = processStatements(statements, outerFunction, loggerFunction, prefixLogName)
            statements.clear()
            statements.addAll(modifiedStatements)
        }

        private fun processStatements(
            statements: List<IrStatement>,
            outerFunction: IrFunction,
            loggerFunction: IrSimpleFunctionSymbol,
            prefixLogName: String
        ): MutableList<IrStatement> {
            val modifiedStatements = mutableListOf<IrStatement>()
            for (statement in statements) {
                when (statement) {
                    is IrBlock -> {
                        statement.addLoggers(outerFunction, loggerFunction, prefixLogName)
                        modifiedStatements += statement
                    }

                    is IrCall -> {
                        if (!statement.isRecomposeLoggerFunction()) {
                            addLoggerForFunction(
                                targetFunction = statement,
                                outerFunction = outerFunction,
                                loggerFunction = loggerFunction,
                                prefixLogName = prefixLogName,
                                modifiedStatements = modifiedStatements
                            )
                        } else {
                            modifiedStatements += statement
                        }
                    }

                    else -> {
                        modifiedStatements += statement
                    }
                }
            }
            return modifiedStatements
        }

        private fun addLoggerForFunction(
            targetFunction: IrCall,
            outerFunction: IrFunction,
            loggerFunction: IrSimpleFunctionSymbol,
            prefixLogName: String,
            modifiedStatements: MutableList<IrStatement>
        ) {
            val result = createRecomposeLoggerCall(
                outerFunction,
                loggerFunction,
                prefixLogName,
                targetFunction
            )
            if (result != null) {
                val (logger, variables) = result
                modifiedStatements += variables
                modifiedStatements += targetFunction
                modifiedStatements += logger
            } else {
                modifiedStatements += targetFunction
            }
        }

        private fun IrBlock.addLoggers(
            outerFunction: IrFunction,
            loggerFunction: IrSimpleFunctionSymbol,
            prefixLogName: String
        ) {
            val modifiedStatements = processStatements(statements, outerFunction, loggerFunction, prefixLogName)
            statements.clear()
            statements.addAll(modifiedStatements)
        }

        private fun createRecomposeLoggerCall(
            outerFunction: IrFunction,
            loggerFunctionSymbol: IrSimpleFunctionSymbol,
            prefixLogName: String,
            call: IrCall
        ): Pair<IrCallImpl, MutableList<IrVariable>>? {
            val variables = mutableListOf<IrVariable>()
            if (call.isValidComposableCall()) {

                val arguments = mutableMapOf<String, IrExpression>()

                for (index in 0 until call.valueArgumentsCount) {
                    call.modifyArgument(index, outerFunction, variables, arguments)
                }

                val logName = "$prefixLogName:${call.symbol.owner.name.asString()}"
                return IrCallImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = pluginContext.irBuiltIns.unitType,
                    symbol = loggerFunctionSymbol,
                ).apply {
                    putValueArgument(0, logName.toIrConst(pluginContext.irBuiltIns.stringType))
                    putValueArgument(
                        1,
                        createRecomposeLoggerArgumentsExpression(arguments)
                    )
                } to variables

            }
            return null
        }

        private fun IrCall.modifyArgument(
            index: Int,
            outerFunction: IrFunction,
            variables: MutableList<IrVariable>,
            arguments: MutableMap<String, IrExpression>
        ) {
            val parameter = symbol.owner.valueParameters[index]
            val expression = getValueArgument(index) ?: return
            if ((parameter.isComposeModifier() && !logModifierChanges) || (expression.isFunctionExpression() && (!logFunctionChanges))) return
            if (expression.isFunctionExpression()) {
                if (canTrackFunctionArgument(symbol.owner, expression, parameter)) {
                    val variable = handleFunctionExpressionArgument(
                        parameter,
                        expression,
                        outerFunction,
                        this,
                        index,
                    )
                    variables += variable
                    arguments[parameter.name.asString()] = IrGetValueImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        variable.symbol,
                    )
                }
            } else {
                arguments[parameter.name.asString()] = expression.deepCopySavingMetadata(outerFunction)
            }
        }

        private fun canTrackFunctionArgument(
            function: IrSimpleFunction,
            expression: IrExpression,
            parameter: IrValueParameter
        ): Boolean {
            if (!function.isInline) return true
            if (parameter.isNoinline) return true

            return when (expression) {
                is IrFunctionReference -> !expression.symbol.owner.isInline
                is IrRawFunctionReference -> !expression.symbol.owner.isInline
                is IrFunctionExpression -> false
                else -> false
            }
        }

        private fun handleFunctionExpressionArgument(
            parameter: IrValueParameter,
            expression: IrExpression,
            outerFunction: IrFunction,
            call: IrCall,
            index: Int,
        ): IrVariableImpl {
            val variable = IrVariableImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                symbol = IrVariableSymbolImpl(),
                name = Name.identifier("$${parameter.name.asString()}${expression.startOffset}${counter.incrementAndGet()}"),
                type = parameter.type,
                isVar = false,
                isConst = false,
                isLateinit = false
            )
            variable.parent = outerFunction
            variable.initializer = expression
            call.putValueArgument(
                index, IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    variable.symbol,
                )
            )
            return variable
        }

        private fun createRecomposeLoggerArgumentsExpression(
            arguments: Map<String, IrExpression>,
        ): IrExpression {
            val irBuiltIns = pluginContext.irBuiltIns
            val nullableAnyType = irBuiltIns.anyType.makeNullable()
            val stringType = irBuiltIns.stringType

            val argumentPairType = pluginContext.referenceClass(pairClassId)
                ?.typeWith(stringType, nullableAnyType) ?: nullableAnyType
            val pairConstructorCall = pluginContext.referenceConstructors(pairClassId)
                .first { it.owner.valueParameters.size == 2 }

            val mapOfSymbol = pluginContext.referenceFunctions(mapOfCallableId)
                .first { it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg }
            val argumentsMapType = irBuiltIns.mapClass.typeWith(stringType, nullableAnyType)

            return IrCallImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = argumentsMapType,
                symbol = mapOfSymbol,
            ).apply {
                putTypeArgument(0, stringType)
                putTypeArgument(1, nullableAnyType)
                putValueArgument(
                    0, IrVarargImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        irBuiltIns.arrayClass.typeWith(argumentPairType),
                        argumentPairType,
                        arguments.map { (name, expression) ->
                            IrConstructorCallImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                argumentPairType,
                                pairConstructorCall,
                                typeArgumentsCount = 2,
                                constructorTypeArgumentsCount = 0,
                            ).apply {
                                putTypeArgument(0, stringType)
                                putTypeArgument(1, nullableAnyType)
                                putValueArgument(0, name.toIrConst(stringType))
                                putValueArgument(1, expression)
                            }
                        })
                )
            }
        }

        private fun IrCall.isValidComposableCall(): Boolean {
            val callFunctionOwner = symbol.owner
            val callFunctionName = callFunctionOwner.name
            return !callFunctionName.isSpecial
                    && callFunctionOwner.isComposable()
                    && valueArgumentsCount != 0
                    && callFunctionOwner.returnType.isUnit()
                    && callFunctionOwner.valueParameters.any { it.isMainComposeModifier() }
        }

        private fun IrValueParameter.isMainComposeModifier(): Boolean =
            name.asString() == "modifier" && type.isComposeModifier()

        private fun IrFunction.isComposable(): Boolean = hasAnnotation(Composable)

        private fun IrValueParameter.isComposeModifier(): Boolean = type.isComposeModifier()

        private fun IrType.isComposeModifier(): Boolean = classFqName?.asString() == MODIFIER_FULL

        private fun IrExpression.isFunctionExpression(): Boolean =
            this is IrFunctionExpression || this is IrFunctionReference || this is IrRawFunctionReference
    }

    private companion object {
        val Composable = FqName("androidx.compose.runtime.Composable")
        const val COMPOSE_UI_PACKAGE = "androidx.compose.ui"
        const val MODIFIER_FULL = "$COMPOSE_UI_PACKAGE.Modifier"
        val pairClassId = ClassId(
            FqName("kotlin"),
            Name.identifier("Pair")
        )

        val mapOfCallableId = CallableId(
            FqName("kotlin.collections"),
            Name.identifier("mapOf")
        )

        val recomposeLoggerCallableId = CallableId(
            FqName("com.vk.recompose.logger"),
            Name.identifier("RecomposeLogger")
        )

        fun IrCall.isRecomposeLoggerFunction(): Boolean {
            return symbol.owner.fqNameWhenAvailable == recomposeLoggerCallableId.asSingleFqName()
        }
    }
}
