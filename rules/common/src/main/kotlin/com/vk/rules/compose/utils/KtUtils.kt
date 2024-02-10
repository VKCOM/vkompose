package com.vk.rules.compose.utils

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

fun KtAnnotated.hasAnnotation(name: String): Boolean =
    annotationEntries.any { it.calleeExpression?.text == name }

fun KtContextReceiver.resolveType(bindingContext: BindingContext): KotlinType? {
    val reference = typeReference() ?: return null
    return BindingUtils.getTypeByReference(
        bindingContext,
        reference,
    )
}

internal fun PropertyDescriptor.resolveDelegateType(bindingContext: BindingContext): KotlinType? {
    val expression = (this.findPsi() as? KtProperty)?.delegateExpression
    return expression?.getType(bindingContext)
}


val KotlinType?.toClassDescriptor: ClassDescriptor?
    get() = this?.constructor?.declarationDescriptor?.let { descriptor ->
        when (descriptor) {
            is ClassDescriptor -> descriptor
            is TypeParameterDescriptor -> descriptor.representativeUpperBound.toClassDescriptor
            else -> null
        }
    }


fun KtClassOrObject.resolveToDescriptorIfAny(bindingContext: BindingContext): ClassDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(bindingContext) as? ClassDescriptor
}


fun KtParameter.resolveDescriptor(bindingContext: BindingContext): ValueParameterDescriptor?
    = this.resolveToParameterDescriptorIfAny(bindingContext)

fun KtParameter.resolveToParameterDescriptorIfAny(bindingContext: BindingContext): ValueParameterDescriptor? {
    return bindingContext.get(BindingContext.VALUE_PARAMETER, this) as? ValueParameterDescriptor
}

fun KtNamedFunction.resolveToDescriptorIfAny(bindingContext: BindingContext): FunctionDescriptor? {
    return (this as KtDeclaration).resolveToDescriptorIfAny(bindingContext) as? FunctionDescriptor
}


fun KtDeclaration.resolveToDescriptorIfAny(bindingContext: BindingContext): DeclarationDescriptor? {
    return if (this is KtParameter && hasValOrVar()) {
        bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)
        // It is incorrect to have `val/var` parameters outside the primary constructor (e.g., `fun foo(val x: Int)`)
        // but we still want to try to resolve in such cases.
            ?: bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    } else {
        bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    }
}

fun KotlinType.isValueClassType(): Boolean = constructor.declarationDescriptor?.isValueClass() ?: false

fun ClassifierDescriptor.isValueClass(): Boolean = isInlineClass() || isMultiFieldValueClass()

fun KotlinType.isInlineClassType(): Boolean = constructor.declarationDescriptor?.isInlineClass() ?: false

fun ClassifierDescriptor.isInlineClass(): Boolean = this is ClassDescriptor && this.valueClassRepresentation is InlineClassRepresentation

fun ClassifierDescriptor.isMultiFieldValueClass(): Boolean =
    this is ClassDescriptor && this.valueClassRepresentation is MultiFieldValueClassRepresentation