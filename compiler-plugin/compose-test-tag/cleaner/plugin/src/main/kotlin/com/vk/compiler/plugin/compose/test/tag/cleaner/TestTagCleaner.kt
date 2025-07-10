package com.vk.compiler.plugin.compose.test.tag.cleaner

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

@OptIn(DeprecatedForRemovalCompilerApi::class)
internal class TestTagCleaner(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    override fun visitCall(expression: IrCall): IrExpression {

        val function = expression.symbol.owner
        val packageName = runCatching { expression.symbol.owner.parent.kotlinFqName }.getOrNull()

        if (packageName?.asString() == TEST_TAG_PACKAGE && function.name.asString() == TEST_TAG_FUNCTION_NAME) {
            expression.putValueArgument(0, "".toIrConst(pluginContext.irBuiltIns.stringType))
            return super.visitCall(expression)
        }

        if (packageName?.asString() == SEMANTICS_PACKAGE && function.correspondingPropertySymbol?.owner?.name?.asString() == TEST_TAG_PROPERTY_NAME) {
            expression.putValueArgument(0, "".toIrConst(pluginContext.irBuiltIns.stringType))
            return super.visitCall(expression)
        }

        return super.visitCall(expression)
    }

    private companion object {
        const val TEST_TAG_PACKAGE = "androidx.compose.ui.platform"
        const val SEMANTICS_PACKAGE = "androidx.compose.ui.semantics"
        const val TEST_TAG_FUNCTION_NAME = "testTag"
        const val TEST_TAG_PROPERTY_NAME = "testTag"
    }
}