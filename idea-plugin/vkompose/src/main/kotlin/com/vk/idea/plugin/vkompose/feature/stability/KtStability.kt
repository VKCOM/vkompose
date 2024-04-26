package com.vk.idea.plugin.vkompose.feature.stability

import com.vk.idea.plugin.vkompose.utils.ComposeClassName
import com.vk.idea.plugin.vkompose.extensions.resolveDelegateType
import com.vk.idea.plugin.vkompose.utils.isValueClass
import com.vk.idea.plugin.vkompose.utils.isValueClassType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isKFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FieldDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.descriptors.isFinalClass
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.inlineClassRepresentation
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isPrimitiveType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isAny
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.util.isJavaDescriptor
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

sealed class KtStability {
    // class Foo(val bar: Int)
    class Certain(val stable: Boolean, val reason: String) : KtStability() {
        override fun toString(): String {
            return if (stable) "Stable" else "Unstable($reason)"
        }
    }

    // class Foo(val bar: ExternalType) -> ExternalType.$stable
    class Runtime(val classSymbol: ClassDescriptor) : KtStability() {
        override fun toString(): String {
            return "Runtime(${classSymbol.name.asString()})"
        }
    }

    // interface Foo { fun result(): Int }
    class Unknown(val declaration: ClassDescriptor) : KtStability() {
        override fun toString(): String {
            return "Uncertain(${declaration.name.asString()})"
        }
    }

    // class <T> Foo(val value: T)
    class Parameter(val parameter: TypeParameterDescriptor) : KtStability() {
        override fun toString(): String {
            return "Parameter(${parameter.name.asString()})"
        }
    }

    // class Foo(val foo: A, val bar: B)
    class Combined(val elements: List<KtStability>) : KtStability() {
        override fun toString(): String {
            return elements.joinToString(",")
        }
    }

    operator fun plus(other: KtStability): KtStability = when {
        other is Certain -> if (other.stable) this else mergeReason(other)
        this is Certain -> if (stable) other else mergeReason(this)
        else -> Combined(listOf(this, other))
    }

    operator fun plus(other: List<KtStability>): KtStability {
        var stability = this
        for (el in other) {
            stability += el
        }
        return stability
    }

    companion object {
        val Stable: KtStability = Certain(true, "")
        fun Unstable(reason: String) = Certain(false, reason)
    }
}

private fun KtStability.mergeReason(unstable: KtStability.Certain): KtStability {
    if (this is KtStability.Certain && !this.stable) {
        return KtStability.Unstable(reason = this.reason + "," + unstable.reason)
    }
    return unstable
}

fun KtStability.knownUnstable(): Boolean = when (this) {
    is KtStability.Certain -> !stable
    is KtStability.Runtime -> false
    is KtStability.Unknown -> false
    is KtStability.Parameter -> false
    is KtStability.Combined -> elements.any { it.knownUnstable() }
}

fun KtStability.knownStable(): Boolean = when (this) {
    is KtStability.Certain -> stable
    is KtStability.Runtime -> false
    is KtStability.Unknown -> false
    is KtStability.Parameter -> false
    is KtStability.Combined -> elements.all { it.knownStable() }
}

fun KtStability.isUncertain(): Boolean = when (this) {
    is KtStability.Certain -> false
    is KtStability.Runtime -> true
    is KtStability.Unknown -> true
    is KtStability.Parameter -> true
    is KtStability.Combined -> elements.any { it.isUncertain() }
}

fun KtStability.isExpressible(): Boolean = when (this) {
    is KtStability.Certain -> true
    is KtStability.Runtime -> true
    is KtStability.Unknown -> false
    is KtStability.Parameter -> true
    is KtStability.Combined -> elements.all { it.isExpressible() }
}

fun KtStability.normalize(): KtStability {
    when (this) {
        // if not combined, there is no normalization needed
        is KtStability.Certain,
        is KtStability.Parameter,
        is KtStability.Runtime,
        is KtStability.Unknown -> return this

        is KtStability.Combined -> {
            // if combined, we perform the more expensive normalization process
        }
    }
    val parameters = mutableSetOf<TypeParameterDescriptor>()
    val parts = mutableListOf<KtStability>()
    val stack = mutableListOf<KtStability>(this)
    while (stack.isNotEmpty()) {
        when (val stability: KtStability = stack.removeAt(stack.size - 1)) {
            is KtStability.Combined -> {
                stack.addAll(stability.elements)
            }

            is KtStability.Certain -> {
                if (!stability.stable)
                    return KtStability.Unstable(stability.reason)
            }

            is KtStability.Parameter -> {
                if (stability.parameter !in parameters) {
                    parameters.add(stability.parameter)
                    parts.add(stability)
                }
            }

            is KtStability.Runtime -> parts.add(stability)
            is KtStability.Unknown -> {
                /* do nothing */
            }
        }
    }
    return KtStability.Combined(parts)
}

fun KtStability.forEach(callback: (KtStability) -> Unit) {
    if (this is KtStability.Combined) {
        elements.forEach { it.forEach(callback) }
    } else {
        callback(this)
    }
}

fun ClassDescriptor.hasStableMarkerAnnotation(): Boolean {
    return annotations.any { it.type.toClassDescriptor?.annotations?.hasAnnotation(ComposeClassName.StableMarker) == true }
}


private fun ClassDescriptor.hasStableMarkedDescendant(): Boolean {
    if (hasStableMarkerAnnotation()) return true
    return defaultType.supertypes().any {
        !it.isAny() && it.toClassDescriptor?.hasStableMarkedDescendant() == true
    }
}

private fun ClassDescriptor.isProtobufType(): Boolean {
    // Quick exit as all protos are final
    if (!isFinalClass) return false
    val directParentClassName =
        defaultType.supertypes()
            .mapNotNull { it.toClassDescriptor }
            .lastOrNull { !it.kind.isInterface }
            ?.classId?.asFqNameString()
    return directParentClassName == "com.google.protobuf.GeneratedMessageLite" ||
            directParentClassName == "com.google.protobuf.GeneratedMessage"
}

//private fun IrAnnotationContainer.stabilityParamBitmask(): Int? =
//    (annotations.findAnnotation(StabilityInferred)
//        ?.getValueArgument(0) as? IrConst<*>
//            )?.value as? Int

private data class SymbolForAnalysis(
    val symbol: ClassDescriptor,
    val typeParameters: List<KotlinType?>
)

class StabilityInferencer(
    externalStableTypeMatchers: Set<FqNameMatcher>
) {

    private val externalTypeMatcherCollection = FqNameMatcherCollection(externalStableTypeMatchers)

    private var currentModule: ModuleDescriptor? = null

    fun setFunctionModule(module: ModuleDescriptor) {
        currentModule = module
    }

    fun ktStabilityOf(kotlinType: KotlinType): KtStability =
        ktStabilityOf(kotlinType, emptyMap(), emptySet())

    @Suppress("ReturnCount", "NestedBlockDepth") // expected
    private fun ktStabilityOf(
        declaration: ClassDescriptor,
        substitutions: Map<TypeParameterDescriptor, KotlinType>,
        currentlyAnalyzing: Set<SymbolForAnalysis>
    ): KtStability {
        val typeArguments = declaration.declaredTypeParameters.map { substitutions[it] }
        val fullSymbol = SymbolForAnalysis(declaration, typeArguments)
        if (currentlyAnalyzing.contains(fullSymbol)) return KtStability.Unstable("recursive analyse ${declaration.name}")
        if (declaration.hasStableMarkedDescendant()) return KtStability.Stable
        if (declaration.kind.isEnumClass || declaration.kind == ClassKind.ENUM_ENTRY) return KtStability.Stable
        if (declaration.defaultType.isPrimitiveType()) return KtStability.Stable
        if (declaration.isProtobufType()) return KtStability.Stable

//    if (classSymbol == IrDeclarationOrigin.IR_BUILTINS_STUB) {
//        error("Builtins Stub: ${declaration.name}")
//    }

        val analyzing = currentlyAnalyzing + fullSymbol

        val funModule = currentModule
        val isFromDifferentModule = funModule != null && funModule.name != declaration.module.name
        if (canInferStability(declaration) || isFromDifferentModule || declaration.isExternalStableType()) {
            val fqName = declaration.classId?.asFqNameString()
            val typeParameters = declaration.declaredTypeParameters
            val stability: KtStability
            val mask: Int
            if (KnownStableConstructs.stableTypes.contains(fqName)) {
                mask = KnownStableConstructs.stableTypes[fqName] ?: 0
                stability = KtStability.Stable
            } else if (declaration.isExternalStableType()) {
                mask = externalTypeMatcherCollection
                    .maskForName(declaration.fqNameOrNull()) ?: 0
                stability = KtStability.Stable
            } else {
                val bitmask = retrieveParameterMask(declaration)
                    ?: return KtStability.Unstable("type ${declaration.name} doesn't have @StabilityInferred")

                val knownStableMask = if (typeParameters.size < 32) 0b1 shl typeParameters.size else 0
                val isKnownStable = bitmask and knownStableMask != 0
                mask = bitmask and knownStableMask.inv()

                stability = if (isKnownStable && !isFromDifferentModule) {
                    KtStability.Stable
                } else {
                    KtStability.Runtime(declaration)
                }
            }
            return when {
                mask == 0 || typeParameters.isEmpty() -> stability
                else -> stability + KtStability.Combined(
                    typeParameters.mapIndexedNotNull { index, parameter ->
                        if (mask and (0b1 shl index) != 0) {
                            val type = substitutions[parameter]
                            if (type != null)
                                ktStabilityOf(type, substitutions, analyzing)
                            else
                                KtStability.Stable
                        } else null
                    }
                )
            }
        } else if (declaration.isJavaDescriptor) {
            return KtStability.Unstable("type from Java: ${declaration.name}")
        }

        if (declaration.kind.isInterface) {
            return KtStability.Unknown(declaration)
        }

        var stability = KtStability.Stable

        for (member in declaration.unsubstitutedMemberScope.getDescriptorsFiltered()) {
            when (member) {
                is PropertyDescriptor -> {
                    if (!member.kind.isReal) continue  // skip properties from parent
                    if (member.getter?.isDefault == false && !member.isVar && !member.isDelegated) continue // skip properties with non default getter because they are usually like function

                    member.backingField?.let {
                        if (member.isVar && !member.isDelegated) return KtStability.Unstable("type ${declaration.name} contains non-delegated var ${member.name}")
                        stability += ktStabilityOf(
                            member.resolveDelegateType() ?: member.type,
                            substitutions,
                            analyzing
                        )
                    }
                }

                is FieldDescriptor -> {
                    stability += ktStabilityOf(
                        member.correspondingProperty.type,
                        substitutions,
                        analyzing
                    )
                }
            }
        }

        return stability
    }

    private fun ClassDescriptor.isExternalStableType(): Boolean {
        return externalTypeMatcherCollection.matches(fqNameOrNull(), defaultType.supertypes().toList())
    }

    private fun canInferStability(declaration: ClassDescriptor): Boolean {
        val fqName = declaration.classId?.asFqNameString()
        return KnownStableConstructs.stableTypes.contains(fqName) || KotlinBuiltIns.isBuiltIn(declaration)
    }

    private fun ktStabilityOf(
        kotlinType: KotlinType,
        substitutions: Map<TypeParameterDescriptor, KotlinType>,
        currentlyAnalyzing: Set<SymbolForAnalysis>
    ): KtStability {
        return when {
            kotlinType.isDynamic() -> KtStability.Unstable("DynamicType")

            kotlinType.isUnit() ||
                    KotlinBuiltIns.isPrimitiveType(kotlinType) ||
                    kotlinType.isFunctionType ||
                    kotlinType.isKFunctionType ||
                    kotlinType.isSyntheticComposableFunction() ||
                    KotlinBuiltIns.isString(kotlinType) -> KtStability.Stable

            TypeUtils.isNonReifiedTypeParameter(kotlinType) -> {
                val descriptor = TypeUtils.getTypeParameterDescriptorOrNull(kotlinType)!!
                val arg = substitutions[descriptor]
                if (arg != null) {
                    ktStabilityOf(arg, substitutions, currentlyAnalyzing)
                } else {
                    KtStability.Parameter(descriptor)
                }
            }

            kotlinType.isNullable() -> {
                ktStabilityOf(
                    kotlinType = kotlinType.makeNotNullable(),
                    substitutions = substitutions,
                    currentlyAnalyzing = currentlyAnalyzing
                )
            }

            kotlinType.isInlineClassType() || kotlinType.isValueClassType() -> {
                val descriptor = kotlinType.constructor.declarationDescriptor as? ClassDescriptor
                if (descriptor?.hasStableMarkerAnnotation() == true) {
                    KtStability.Stable
                } else {
                    val type = descriptor?.inlineClassRepresentation?.underlyingType as? KotlinType
                    if (type != null) {
                        ktStabilityOf(
                            kotlinType = type,
                            substitutions = substitutions,
                            currentlyAnalyzing = currentlyAnalyzing
                        )
                    } else KtStability.Unstable("can't resolve underlying type for inline class $kotlinType")
                }
            }

            kotlinType.constructor.declarationDescriptor is ClassDescriptor -> {
                val classDescriptor = kotlinType.toClassDescriptor
                if (classDescriptor != null) {
                    ktStabilityOf(
                        classDescriptor,
                        substitutions + classDescriptor.substitutionMap(kotlinType),
                        currentlyAnalyzing
                    )
                } else KtStability.Unstable("can't resolve class descriptor of $kotlinType")
            }

            kotlinType.isTypeParameter() -> {
                KtStability.Unstable("contains Generic $kotlinType")
            }

            kotlinType.getAbbreviation() != null -> {
                val type = kotlinType.getAbbreviation() as? KotlinType
                if (type != null) {
                    ktStabilityOf(
                        type,
                        substitutions,
                        currentlyAnalyzing
                    )
                } else KtStability.Unstable("can't abbreviation of $kotlinType")
            }

            else -> error("Unexpected kotlinType: $kotlinType")
        }
    }


    @Suppress("NestedBlockDepth", "ComplexCondition", "CyclomaticComplexMethod") // expected
    private fun retrieveParameterMask(classDescriptor: ClassDescriptor): Int? {
        if (
            (classDescriptor.visibility != DescriptorVisibilities.PUBLIC && classDescriptor.visibility != DescriptorVisibilities.INTERNAL) ||
            classDescriptor.kind.isEnumClass ||
            classDescriptor.kind == ClassKind.ENUM_ENTRY ||
            classDescriptor.kind.isInterface ||
            classDescriptor.kind == ClassKind.ANNOTATION_CLASS ||
            classDescriptor.name == SpecialNames.NO_NAME_PROVIDED ||
            classDescriptor.isExpect ||
            classDescriptor.isInner ||
            classDescriptor.isCompanionObject ||
            classDescriptor.isInline ||
            classDescriptor.isValueClass() ||
            !KotlinBuiltIns.isPrimitiveType(classDescriptor.defaultType) && KotlinBuiltIns.isBuiltIn(classDescriptor)
        ) return null

        val savedModule = currentModule
        currentModule = null
        val stability = ktStabilityOf(classDescriptor, emptyMap(), emptySet()).normalize()
        currentModule = savedModule

        var parameterMask = 0

        val typeParameters = classDescriptor.declaredTypeParameters
        if (typeParameters.isNotEmpty()) {

            stability.forEach {
                when (it) {
                    is KtStability.Parameter -> {
                        val index = typeParameters.indexOf(it.parameter)
                        if (index != -1) {
                            // the stability of this parameter matters for the stability of the
                            // class
                            parameterMask = parameterMask or (0b1 shl index)
                        }
                    }

                    else -> {
                        /* No action necessary */
                    }
                }
            }
            if (stability.knownStable() && typeParameters.size < 32) {
                parameterMask = parameterMask or (0b1 shl typeParameters.size)
            }
        } else {
            if (stability.knownStable()) {
                parameterMask = 0b1
            }
        }
        return parameterMask
    }

    private fun ClassDescriptor.substitutionMap(kotlinType: KotlinType): Map<TypeParameterDescriptor, KotlinType> {
        return declaredTypeParameters.zip(kotlinType.arguments)
            .associate { (descriptor, projection) -> descriptor to projection.type }
    }
}

private fun KotlinType.isSyntheticComposableFunction() =
    fqName?.asString().orEmpty().startsWith("androidx.compose.runtime.internal.ComposableFunction")