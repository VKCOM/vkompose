package com.vk.idea.plugin.vkompose.extensions

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtProperty

context(KaSession)
fun KaPropertySymbol.resolveDelegateType(): KaType? {
    val property = psi as? KtProperty ?: return null
    val delegateExpression = property.delegateExpression ?: return null
    return delegateExpression.expressionType
}