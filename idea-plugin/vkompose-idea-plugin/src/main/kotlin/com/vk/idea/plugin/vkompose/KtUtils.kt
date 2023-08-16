package com.vk.idea.plugin.vkompose

import com.vk.idea.plugin.vkompose.ComposeClassName.Composable
import com.vk.idea.plugin.vkompose.extensions.fqNameMatches
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isMultiFieldValueClass
import org.jetbrains.kotlin.types.KotlinType


internal fun KtNamedFunction.hasComposableAnnotation(): Boolean = hasAnnotation(Composable.asString())

internal fun KtNamedFunction.hasAnnotation(fqString: String): Boolean =
    annotationEntries.any { annotation -> annotation.fqNameMatches(fqString) }

fun KotlinType.isValueClassType(): Boolean = constructor.declarationDescriptor?.isValueClass() ?: false

fun DeclarationDescriptor.isValueClass(): Boolean = isInlineClass() || isMultiFieldValueClass()

