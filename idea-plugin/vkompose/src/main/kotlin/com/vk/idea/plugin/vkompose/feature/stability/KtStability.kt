@file:OptIn(KaExperimentalApi::class)

package com.vk.idea.plugin.vkompose.feature.stability

import com.vk.idea.plugin.vkompose.extensions.resolveDelegateType
import com.vk.idea.plugin.vkompose.utils.ComposeClassName
import com.vk.idea.plugin.vkompose.utils.isInlineClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.base.analysis.api.utils.isJavaSourceOrLibrary
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtClass

val logger = com.intellij.openapi.diagnostic.Logger.getInstance(KtStability::class.java)

sealed class KtStability {
    // class Foo(val bar: Int)
    class Certain(val stable: Boolean, val reason: String) : KtStability() {
        override fun toString(): String {
            return if (stable) "Stable" else "Unstable($reason)"
        }
    }

    // class Foo(val bar: ExternalType) -> ExternalType.$stable
    class Runtime(val classSymbol: KaClassSymbol) : KtStability() {
        private val name = classSymbol.name

        override fun toString(): String {
            return "Runtime(${name?.asString()})"
        }
    }

    // interface Foo { fun result(): Int }
    class Unknown(val declaration: KaClassSymbol) : KtStability() {
        private val name = declaration.name

        override fun toString(): String {
            return "Uncertain(${name?.asString()})"
        }
    }

    // class <T> Foo(val value: T)
    class Parameter(val parameter: KaTypeParameterSymbol) : KtStability() {
        private val name = parameter.name

        override fun toString(): String {
            return "Parameter(${name.asString()})"
        }
    }

    // class Foo(val foo: A, val bar: B)
    class Combined(val elements: List<KtStability>) : KtStability() {
        override fun toString(): String {
            return elements.joinToString(", ")
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
    val parameters = mutableSetOf<KaTypeParameterSymbol>()
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

context(KaSession)
fun KaDeclarationSymbol.hasStableMarkerAnnotation(visited: MutableSet<ClassId> = mutableSetOf()): Boolean {
    return if (KotlinPluginModeProvider.isK2Mode()) {
        annotations.any { annotation ->
            val classId = annotation.classId ?: return@any false
            if (!visited.add(classId)) return@any false
            classId.asSingleFqName() == ComposeClassName.StableMarker
                    ||  annotation.constructorSymbol?.containingDeclaration?.hasStableMarkerAnnotation(visited) == true
        }
    } else {
        val psiCl = psi
        when {
            psiCl is KtClass -> {
                psiCl.annotationEntries.any { annotation ->
                    annotation.typeReference?.type?.symbol?.annotations?.any {
                        it.classId?.asSingleFqName() == ComposeClassName.StableMarker
                    } == true
                }
            }
            else -> {
                logger.error("Can't extract psi from ${this.name?.asString()}")
                true
            }
        }
    }
}


context(KaSession)
private fun KaNamedClassSymbol.hasStableMarkedDescendant(): Boolean {
    if (hasStableMarkerAnnotation()) return true
    return superTypes.any {
        !it.isAnyType && (it.symbol as? KaNamedClassSymbol)?.hasStableMarkedDescendant() == true
    }
}

context(KaSession)
private fun KaNamedClassSymbol.isProtobufType(): Boolean {
    // Quick exit as all protos are final
    if (modality != KaSymbolModality.FINAL) return false
    val directParentClassName =
        superTypes
            .lastOrNull { !it.isInterface() }
            ?.symbol?.classId?.asFqNameString()
    return directParentClassName == "com.google.protobuf.GeneratedMessageLite" ||
            directParentClassName == "com.google.protobuf.GeneratedMessage"
}

fun KaType.isInterface(): Boolean {
    if (this !is KaClassType) return false
    val classSymbol = symbol
    return classSymbol is KaClassSymbol && classSymbol.classKind == KaClassKind.INTERFACE
}


//private fun IrAnnotationContainer.stabilityParamBitmask(): Int? =
//    (annotations.findAnnotation(StabilityInferred)
//        ?.getValueArgument(0) as? IrConst<*>
//            )?.value as? Int

private data class SymbolForAnalysis(
    val symbol: KaClassSymbol,
    val typeParameters: List<KaClassLikeSymbol?>
)

class StabilityInferencer(
    externalStableTypeMatchers: Set<FqNameMatcher>
) {

    private val externalTypeMatcherCollection = FqNameMatcherCollection(externalStableTypeMatchers)

    private var currentModule: KaSourceModule? = null

    fun setFunctionModule(module: KaSourceModule) {
        currentModule = module
    }

    context(KaSession)
    fun ktStabilityOf(kotlinType: KaType): KtStability =
        ktStabilityOf(kotlinType, emptyMap(), emptySet())

    context(KaSession)
    @Suppress("ReturnCount", "NestedBlockDepth", "CyclomaticComplexMethod") // expected
    private fun ktStabilityOf(
        declaration: KaClassSymbol,
        substitutions: Map<KaTypeParameterSymbol, KaType?>,
        currentlyAnalyzing: Set<SymbolForAnalysis>
    ): KtStability {
        val typeArguments = declaration.typeParameters.map { substitutions[it]?.symbol }
        val fullSymbol = SymbolForAnalysis(declaration, typeArguments)
        if (currentlyAnalyzing.contains(fullSymbol)) return KtStability.Unstable("recursive analyse ${declaration.name}")
        if (declaration is KaNamedClassSymbol && declaration.hasStableMarkedDescendant()) return KtStability.Stable
        if (declaration is KaNamedClassSymbol && declaration.classKind == KaClassKind.ENUM_CLASS) return KtStability.Stable
        if (declaration is KaNamedClassSymbol && declaration.defaultType.isPrimitive) return KtStability.Stable
        if (declaration is KaNamedClassSymbol && declaration.isProtobufType()) return KtStability.Stable

//    if (classSymbol == IrDeclarationOrigin.IR_BUILTINS_STUB) {
//        error("Builtins Stub: ${declaration.name}")
//    }

        val analyzing = currentlyAnalyzing + fullSymbol

        val funModule = currentModule
        val isFromDifferentModule = funModule != null && funModule.name != (declaration.containingModule as? KaSourceModule)?.name
        val isExternalStablyType = declaration is KaNamedClassSymbol && declaration.isExternalStableType()
        if (canInferStability(declaration) || isFromDifferentModule || isExternalStablyType) {
            val fqName = declaration.classId?.asFqNameString()
            val typeParameters = declaration.typeParameters
            val stability: KtStability
            val mask: Int
            if (KnownStableConstructs.stableTypes.contains(fqName)) {
                mask = KnownStableConstructs.stableTypes[fqName] ?: 0
                stability = KtStability.Stable
            } else if (isExternalStablyType) {
                mask = externalTypeMatcherCollection
                    .maskForName(declaration.classId?.asSingleFqName()) ?: 0
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
        } else if (declaration.origin.isJavaSourceOrLibrary()) {
            return KtStability.Unstable("type from Java: ${declaration.name}")
        }

        if (declaration.classKind == KaClassKind.INTERFACE) {
            return KtStability.Unknown(declaration)
        }

        var stability = KtStability.Stable

        for (member in declaration.memberScope.declarations) {
            when (member) {
                is KaPropertySymbol -> {
                    if (member.containingSymbol != declaration) continue  // skip properties from parent
                    if (member.getter?.isDefault == false && member.isVal && !member.isDelegatedProperty) continue // skip properties with non default getter because they are usually like function

                    // custom getter and setter without using field
                    if (!member.isVal && member.getter?.isDefault == false && member.setter?.isDefault == false && member.initializer == null) continue
                    member.backingFieldSymbol?.let {
                        if (!member.isVal && !member.isDelegatedProperty) return KtStability.Unstable("type ${declaration.name} contains non-delegated var ${member.name}")
                        stability += ktStabilityOf(
                            member.resolveDelegateType() ?: member.returnType,
                            substitutions,
                            analyzing
                        )
                    }
                }

                is KaVariableSymbol -> {
                    stability += ktStabilityOf(
                        member.returnType,
                        substitutions,
                        analyzing
                    )
                }

                else -> {}
            }
        }

        return stability
    }

    context(KaSession)
    private fun KaNamedClassSymbol.isExternalStableType(): Boolean {
        return externalTypeMatcherCollection.matches(classId?.asSingleFqName(), superTypes.toList())
    }

    private fun canInferStability(declaration: KaClassSymbol): Boolean {
        val fqName = declaration.classId?.asFqNameString()
        return KnownStableConstructs.stableTypes.contains(fqName) || fqName?.startsWith("kotlin") == true
    }

    context(KaSession)
    private fun ktStabilityOf(
        kotlinType: KaType,
        substitutions: Map<KaTypeParameterSymbol, KaType?>,
        currentlyAnalyzing: Set<SymbolForAnalysis>
    ): KtStability {
        return when {
            kotlinType is KaDynamicType -> KtStability.Unstable("DynamicType")

            kotlinType.isUnitType ||
                    kotlinType.isPrimitive ||
                    kotlinType.isFunctionType ||
                    kotlinType.isKFunctionType ||
                    kotlinType.isSyntheticComposableFunction() ||
                    kotlinType.isStringType -> KtStability.Stable

            kotlinType is KaTypeParameterType && !kotlinType.symbol.isReified -> {
                val descriptor = kotlinType.symbol
                val arg = substitutions[descriptor]
                if (arg != null) {
                    ktStabilityOf(arg, substitutions, currentlyAnalyzing)
                } else {
                    KtStability.Parameter(descriptor)
                }
            }

            kotlinType.isMarkedNullable -> {
                ktStabilityOf(
                    kotlinType = kotlinType.withNullability(KaTypeNullability.NON_NULLABLE),
                    substitutions = substitutions,
                    currentlyAnalyzing = currentlyAnalyzing
                )
            }

            kotlinType.symbol?.isInlineClass() == true -> {
                val symbol = kotlinType.symbol as? KaNamedClassSymbol
                if (symbol?.hasStableMarkerAnnotation() == true) {
                    KtStability.Stable
                } else {
                    val type = symbol?.memberScope?.constructors?.firstOrNull { it.isPrimary }?.valueParameters?.firstOrNull()?.returnType
                    if (type != null) {
                        ktStabilityOf(
                            kotlinType = type,
                            substitutions = substitutions,
                            currentlyAnalyzing = currentlyAnalyzing
                        )
                    } else KtStability.Unstable("can't resolve underlying type for inline class $kotlinType")
                }
            }

            kotlinType is KaClassType -> {
                val classSymbol =  kotlinType.fullyExpandedType.expandedSymbol as KaClassSymbol
                val map: Map<KaTypeParameterSymbol, KaType?> = classSymbol.typeParameters
                    .zip(kotlinType.typeArguments).toMap()
                    .mapValues { it.value.type }
                ktStabilityOf(
                    classSymbol,
                    substitutions + map,
                    currentlyAnalyzing
                )
            }

            kotlinType is KaTypeParameterType -> {
                KtStability.Unstable("contains Generic $kotlinType")
            }

            kotlinType.symbol is KaTypeAliasSymbol -> {
                val type = (kotlinType.symbol as KaTypeAliasSymbol).expandedType
                ktStabilityOf(
                    type,
                    substitutions,
                    currentlyAnalyzing
                )
            }

            else -> error("Unexpected kotlinType: $kotlinType")
        }
    }


    context(KaSession)
    @OptIn(KaExperimentalApi::class)
    @Suppress("NestedBlockDepth", "ComplexCondition", "CyclomaticComplexMethod") // expected
    private fun retrieveParameterMask(classSymbol: KaClassSymbol): Int? {
        if (
            (classSymbol.visibility != KaSymbolVisibility.PUBLIC && classSymbol.visibility != KaSymbolVisibility.INTERNAL) ||
            classSymbol.classKind == KaClassKind.ENUM_CLASS ||
            classSymbol.classKind == KaClassKind.INTERFACE ||
            classSymbol.classKind == KaClassKind.ANNOTATION_CLASS ||
            classSymbol.name == SpecialNames.NO_NAME_PROVIDED ||
            classSymbol.isExpect ||
            classSymbol is KaNamedClassSymbol && classSymbol.isInner ||
            classSymbol.classKind == KaClassKind.COMPANION_OBJECT ||
            classSymbol.isInlineClass() ||
            classSymbol is KaNamedClassSymbol && !classSymbol.defaultType.isPrimitive && classSymbol.classId?.asFqNameString()?.startsWith("kotlin") == true
        ) return null

        val savedModule = currentModule
        currentModule = null
        val stability = ktStabilityOf(classSymbol, emptyMap(), emptySet()).normalize()
        currentModule = savedModule

        var parameterMask = 0

        val typeParameters = classSymbol.typeParameters
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
}

private fun KaType.isSyntheticComposableFunction() =
    symbol?.classId?.asString().orEmpty().startsWith("androidx.compose.runtime.internal.ComposableFunction")