package com.vk.compiler.plugin.recompose.highlighter

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
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(DeprecatedForRemovalCompilerApi::class)
internal class RecomposeHighlighter(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private val highlighterModifier by lazy {
        pluginContext.referenceClass(highlighterModifierClassId)
    }

    private val thenFunction by lazy {
        pluginContext.referenceFunctions(thenFuncCallableId).firstOrNull()?.owner?.symbol
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val thenFunction = thenFunction
        val highlighterModifier = highlighterModifier

        if (thenFunction != null
            && highlighterModifier != null
            && expression.symbol.owner.isComposable()
        ) {
            expression.transformModifierArguments(
                highlighterModifier,
                thenFunction
            )
        }

        return super.visitCall(expression)
    }

    private fun IrCall.transformModifierArguments(
        highlighterModifier: IrClassSymbol,
        thenFunction: IrSimpleFunctionSymbol
    ) {
        for (index in 0 until valueArgumentsCount) {
            val parameter = symbol.owner.valueParameters[index]

            if (parameter.isVararg || !parameter.type.isComposeModifier()) continue

            val expression = getValueArgument(index) ?: parameter.defaultValue?.expression

            if (expression is IrCall || expression is IrGetObjectValue || expression is IrGetValue) {
                val modifiedArgumentExpression = createHighlightedModifierArgument(
                    parameter,
                    expression,
                    highlighterModifier,
                    thenFunction
                )
                putValueArgument(index, modifiedArgumentExpression)
            }
        }
    }

    private fun createHighlightedModifierArgument(
        parameter: IrValueParameter,
        argumentExpression: IrExpression,
        highlighterModifier: IrClassSymbol,
        thenFunction: IrSimpleFunctionSymbol
    ): IrExpression {

        val customModifier = IrGetObjectValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = parameter.type,
            symbol = highlighterModifier
        )

        val thenCall = IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = parameter.type,
            symbol = thenFunction,
        )
        thenCall.putValueArgument(0, customModifier)
        thenCall.dispatchReceiver = argumentExpression
        return thenCall
    }


    private fun IrFunction.isComposable(): Boolean {
        return hasAnnotation(Composable)
    }

    private fun IrType.isComposeModifier(): Boolean =
        classFqName?.asString() == MODIFIER_FULL


    private companion object {
        const val MODIFIER_FULL = "androidx.compose.ui.Modifier"
        val Composable = FqName("androidx.compose.runtime.Composable")
        val thenFuncCallableId = CallableId(
            ClassId(
                FqName("androidx.compose.ui"),
                Name.identifier("Modifier")
            ),
            Name.identifier("then")
        )
        val highlighterModifierClassId = ClassId(
            FqName("com.vk.recompose.highlighter"),
            Name.identifier("HighlighterModifier")
        )
    }
}