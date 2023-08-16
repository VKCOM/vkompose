package com.vk.idea.plugin.vkompose.extensions

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.types.KotlinType

internal fun PropertyDescriptor.resolveDelegateType(): KotlinType? {
    val expression = (this.findPsi() as? KtProperty)?.delegateExpression
    val bindingContext = expression?.analyze() ?: return null
    return expression.getType(bindingContext)
}