package com.vk.compiler.plugin.compose.source.information.cleaner

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class SourceInformationCleaner : IrElementTransformerVoid() {

    // compose generate blocks in function body
    override fun visitBlock(expression: IrBlock): IrExpression {
        return super.visitBlock(expression.apply {
            val elements = statements.filterNot(::isSourceInformationFunctionCall)
            statements.clear()
            statements.addAll(elements)
        })
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        return super.visitBlockBody(body.apply {
            val elements = statements.filterNot(::isSourceInformationFunctionCall)
            statements.clear()
            statements.addAll(elements)
        })
    }

    private fun isSourceInformationFunctionCall(statement: IrStatement): Boolean {
        val call = statement as? IrCallImpl ?: return false

        val function = call.symbol.owner
        val packageName = runCatching { call.symbol.owner.parent.kotlinFqName }.getOrNull()

        return packageName?.asString() == FUNCTIONS_PACKAGE && function.name.asString() in functionsToRemove
    }

    private companion object {
        const val FUNCTIONS_PACKAGE = "androidx.compose.runtime"
        const val INFORMATION_FUNC = "sourceInformation"
        const val INFORMATION_START_FUNC = "sourceInformationMarkerStart"
        const val INFORMATION_END_FUNC = "sourceInformationMarkerEnd"

        val functionsToRemove = setOf(INFORMATION_FUNC, INFORMATION_START_FUNC, INFORMATION_END_FUNC)
    }
}