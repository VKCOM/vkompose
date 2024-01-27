package com.vk.idea.plugin.vkompose.extensions

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.types.KotlinType

val KtContextReceiver.type: KotlinType?
    get() {
        val reference = typeReference() ?: return null
        return BindingUtils.getTypeByReference(
            analyze(),
            reference,
        )
    }