package com.vk.compiler.plugin.compose.test.tag.drawer

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(DeprecatedForRemovalCompilerApi::class)
internal class TestTagDrawer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private val drawTestTagFunction by lazy {
        pluginContext.referenceFunctions(drawTestTagCallableId).firstOrNull()?.owner?.symbol
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val drawTestTag = drawTestTagFunction

        if (drawTestTag != null && expression.symbol.owner.isComposable()) {
            expression.transformModifierArguments(drawTestTag)
        }
        return super.visitCall(expression)
    }

    private fun IrCall.transformModifierArguments(drawTestTagFunction: IrSimpleFunctionSymbol): IrCall {
        for(index in 0 until valueArgumentsCount) {
            val parameter = symbol.owner.valueParameters[index]
            val argumentExpression = retrieveArgumentExpression(parameter, getValueArgument(index))

            if (argumentExpression == null || parameter.isVararg || !parameter.isMainComposeModifier()) {
                continue
            }

            val modifiedArgumentExpression = createDrawTestTagArgument(
                parameter,
                argumentExpression,
                drawTestTagFunction,
            )
            putValueArgument(index, modifiedArgumentExpression)
        }
        return this
    }

    private fun createDrawTestTagArgument(
        parameter: IrValueParameter,
        argumentExpression: IrExpression,
        drawTestTagFunction: IrSimpleFunctionSymbol
    ): IrExpression {

        // Column() -> Column(<default-value>.drawTestTag())
        // Column(Modifier) -> Column(Modifier.drawTestTag())
        // Column(modifier) -> Column(modifier.drawTestTag())
        // Column(modifier.fillMaxSize()) -> Column(modifier.fillMaxSize().drawTestTag())
        val drawTestTagCall = IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = parameter.type,
            symbol = drawTestTagFunction,
        )

        drawTestTagCall.extensionReceiver = argumentExpression

        return drawTestTagCall
    }

    private fun IrFunction.isComposable(): Boolean = hasAnnotation(Composable)

    private fun IrValueParameter.isMainComposeModifier(): Boolean =
        name.asString() == "modifier" && type.isComposeModifier()

    private fun IrType.isComposeModifier(): Boolean = classFqName?.asString() == MODIFIER_FULL

    private fun retrieveArgumentExpression(
        param: IrValueParameter,
        value: IrExpression?
    ): IrExpression? =
        when {
            value == null -> param.defaultValue?.expression?.takeIf { it.isValidValueExpression() }
            value.isValidValueExpression() -> value
            else -> null
        }

    private fun IrExpression.isValidValueExpression(): Boolean =
        this is IrCall || this is IrGetObjectValue || this is IrGetValue


    private companion object {
        const val COMPOSE_UI_PACKAGE = "androidx.compose.ui"
        const val MODIFIER_FULL = "$COMPOSE_UI_PACKAGE.Modifier"
        val Composable = FqName("androidx.compose.runtime.Composable")

        val drawTestTagCallableId =
            CallableId(FqName("com.vk.compose.test.tag.drawer"), Name.identifier("drawTestTag"))
    }
}