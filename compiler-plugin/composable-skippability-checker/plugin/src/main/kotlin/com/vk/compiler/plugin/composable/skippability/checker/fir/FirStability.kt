package com.vk.compiler.plugin.composable.skippability.checker.fir

import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassId
import com.vk.compiler.plugin.composable.skippability.checker.KnownStableConstructs
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.isInlineOrValueClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumEntry
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.isPrimitiveType
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.isPrimitive
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.getUnsubstitutedUnderlyingType

sealed class FirStability {
    // class Foo(val bar: Int)
    class Certain(val stable: Boolean) : FirStability() {
        override fun toString(): String {
            return if (stable) "Stable" else "Unstable"
        }
    }

    // class Foo(val bar: ExternalType) -> ExternalType.$stable
    class Runtime(val classSymbol: FirRegularClassSymbol) : FirStability() {
        override fun toString(): String {
            return "Runtime(${classSymbol.name.asString()})"
        }
    }

    // interface Foo { fun result(): Int }
    class Unknown(val declaration: FirRegularClassSymbol) : FirStability() {
        override fun toString(): String {
            return "Uncertain(${declaration.name.asString()})"
        }
    }

    // class <T> Foo(val value: T)
    class Parameter(val parameter: FirTypeParameterSymbol) : FirStability() {
        override fun toString(): String {
            return "Parameter(${parameter.name.asString()})"
        }
    }

    // class Foo(val foo: A, val bar: B)
    class Combined(val elements: List<FirStability>) : FirStability() {
        override fun toString(): String {
            return elements.joinToString(",")
        }
    }

    operator fun plus(other: FirStability): FirStability = when {
        other is Certain -> if (other.stable) this else other
        this is Certain -> if (stable) other else this
        else -> Combined(listOf(this, other))
    }

    operator fun plus(other: List<FirStability>): FirStability {
        var stability = this
        for (el in other) {
            stability += el
        }
        return stability
    }

    companion object {
        val Stable: FirStability = Certain(true)
        val Unstable: FirStability = Certain(false)
    }
}

fun FirStability.knownUnstable(): Boolean = when (this) {
    is FirStability.Certain -> !stable
    is FirStability.Runtime -> false
    is FirStability.Unknown -> false
    is FirStability.Parameter -> false
    is FirStability.Combined -> elements.any { it.knownUnstable() }
}

fun FirStability.knownStable(): Boolean = when (this) {
    is FirStability.Certain -> stable
    is FirStability.Runtime -> false
    is FirStability.Unknown -> false
    is FirStability.Parameter -> false
    is FirStability.Combined -> elements.all { it.knownStable() }
}

fun FirStability.isUncertain(): Boolean = when (this) {
    is FirStability.Certain -> false
    is FirStability.Runtime -> true
    is FirStability.Unknown -> true
    is FirStability.Parameter -> true
    is FirStability.Combined -> elements.any { it.isUncertain() }
}

fun FirStability.isExpressible(): Boolean = when (this) {
    is FirStability.Certain -> true
    is FirStability.Runtime -> true
    is FirStability.Unknown -> false
    is FirStability.Parameter -> true
    is FirStability.Combined -> elements.all { it.isExpressible() }
}

fun FirStability.normalize(): FirStability {
    when (this) {
        // if not combined, there is no normalization needed
        is FirStability.Certain,
        is FirStability.Parameter,
        is FirStability.Runtime,
        is FirStability.Unknown -> return this

        is FirStability.Combined -> {
            // if combined, we perform the more expensive normalization process
        }
    }
    val parameters = mutableSetOf<FirTypeParameterSymbol>()
    val parts = mutableListOf<FirStability>()
    val stack = mutableListOf<FirStability>(this)
    while (stack.isNotEmpty()) {
        when (val stability: FirStability = stack.removeAt(stack.size - 1)) {
            is FirStability.Combined -> {
                stack.addAll(stability.elements)
            }

            is FirStability.Certain -> {
                if (!stability.stable)
                    return FirStability.Unstable
            }

            is FirStability.Parameter -> {
                if (parameters.contains(stability.parameter)) {
                    parameters.add(stability.parameter)
                    parts.add(stability)
                }
            }

            is FirStability.Runtime -> parts.add(stability)
            is FirStability.Unknown -> {
                /* do nothing */
            }
        }
    }
    return FirStability.Combined(parts)
}

fun FirStability.forEach(callback: (FirStability) -> Unit) {
    if (this is FirStability.Combined) {
        elements.forEach { it.forEach(callback) }
    } else {
        callback(this)
    }
}

@OptIn(UnresolvedExpressionTypeAccess::class)
fun FirRegularClassSymbol.hasStableMarkerAnnotation(session: FirSession): Boolean {
    return resolvedAnnotationsWithClassIds.map { it.coneTypeOrNull }
        .mapNotNull { it?.toRegularClassSymbol(session) }
        .any { it.hasAnnotation(ComposeClassId.StableMarker, session) }
}

private fun FirRegularClassSymbol.hasStableMarkedDescendant(session: FirSession): Boolean {
    if (hasStableMarkerAnnotation(session)) return true
    return resolvedSuperTypes.any {
        !it.isAny && it.toRegularClassSymbol(session)?.hasStableMarkedDescendant(session) == true
    }
}

private fun FirRegularClassSymbol.isProtobufType(session: FirSession): Boolean {
    // Quick exit as all protos are final
    if (!isFinal) return false
    val directParentClassName =
        resolvedSuperTypes
            .mapNotNull { it.toRegularClassSymbol(session) }
            .lastOrNull { !it.isInterface }
            ?.classId?.asFqNameString()
    return directParentClassName == "com.google.protobuf.GeneratedMessageLite" ||
            directParentClassName == "com.google.protobuf.GeneratedMessage"
}

//private fun IrAnnotationContainer.stabilityParamBitmask(): Int? =
//    (annotations.findAnnotation(StabilityInferred)
//        ?.getValueArgument(0) as? IrConst<*>
//            )?.value as? Int

fun firStabilityOf(firTypeRef: FirTypeRef, session: FirSession): FirStability =
    firStabilityOf(firTypeRef, session, emptyMap(), emptySet())

@OptIn(UnresolvedExpressionTypeAccess::class)
@Suppress("ReturnCount", "NestedBlockDepth") // expected
private fun firStabilityOf(
    classSymbol: FirRegularClassSymbol,
    session: FirSession,
    substitutions: Map<FirTypeParameterSymbol, ConeKotlinTypeProjection>,
    currentlyAnalyzing: Set<FirClassifierSymbol<*>>
): FirStability {
    if (currentlyAnalyzing.contains(classSymbol)) return FirStability.Unstable
    if (classSymbol.hasStableMarkedDescendant(session)) return FirStability.Stable
    if (classSymbol.isEnumClass || classSymbol.isEnumEntry) return FirStability.Stable
    if (classSymbol.isPrimitiveType()) return FirStability.Stable
    if (classSymbol.isProtobufType(session)) return FirStability.Stable

//    if (classSymbol.origin == IrDeclarationOrigin.IR_BUILTINS_STUB) {
//        error("Builtins Stub: ${declaration.name}")
//    }

    val analyzing = currentlyAnalyzing + classSymbol

    if (canInferStability(classSymbol)) {
        val fqName = classSymbol.classId.asFqNameString()
        val stability: FirStability
        val mask: Int
        if (KnownStableConstructs.stableTypes.contains(fqName)) {
            mask = KnownStableConstructs.stableTypes[fqName] ?: 0
            stability = FirStability.Stable
        } else {
            mask = retrieveParameterMask(classSymbol, session, substitutions, analyzing)
                ?: return FirStability.Unstable
            stability = FirStability.Runtime(classSymbol)
        }
        return stability + FirStability.Combined(
            classSymbol.typeParameterSymbols.mapIndexedNotNull { index, parameter ->
                if (mask and (0b1 shl index) != 0) {
                    val type = substitutions[parameter]?.type
                    if (type != null)
                        firStabilityOf(type, session, substitutions, analyzing)
                    else
                        FirStability.Stable
                } else null
            }
        )
    } else if (classSymbol.origin is FirDeclarationOrigin.Java) {
        return FirStability.Unstable
    }

    if (classSymbol.isInterface) {
        return FirStability.Unknown(classSymbol)
    }

    var stability = FirStability.Stable

    for (member in classSymbol.declarationSymbols) {
        when (member) {
            is FirPropertySymbol -> {
                member.backingFieldSymbol?.let {
                    if (member.isVar && member.delegate == null) return FirStability.Unstable
                    val delegateType = member.delegate?.coneTypeOrNull
                    stability += firStabilityOf(
                        delegateType ?: it.resolvedReturnType,
                        session,
                        substitutions,
                        analyzing
                    )
                }
            }

            is FirFieldSymbol -> {
                stability += firStabilityOf(
                    member.resolvedReturnType,
                    session,
                    substitutions,
                    analyzing
                )
            }
        }
    }

    return stability
}

private fun canInferStability(declaration: FirRegularClassSymbol): Boolean {
    val fqName = declaration.classId.asFqNameString()
    return KnownStableConstructs.stableTypes.contains(fqName) || declaration.origin is FirDeclarationOrigin.BuiltIns
}

private fun firStabilityOf(
    firTypeRef: FirTypeRef,
    session: FirSession,
    substitutions: Map<FirTypeParameterSymbol, ConeKotlinTypeProjection>,
    currentlyAnalyzing: Set<FirClassifierSymbol<*>>
): FirStability {
    if (firTypeRef !is FirResolvedTypeRef) return FirStability.Unstable
    return firStabilityOf(firTypeRef.coneType, session, substitutions, currentlyAnalyzing)
}

private fun firStabilityOf(
    coneKotlinType: ConeKotlinType,
    session: FirSession,
    substitutions: Map<FirTypeParameterSymbol, ConeKotlinTypeProjection>,
    currentlyAnalyzing: Set<FirClassifierSymbol<*>>
): FirStability {
    val expandedType = coneKotlinType.fullyExpandedType(session)
    val regularClassSymbol = expandedType.toRegularClassSymbol(session)
    return when {
        expandedType is ConeDynamicType -> FirStability.Unstable

        expandedType.isUnit ||
                expandedType.isPrimitive ||
                expandedType.isFunctionOrKFunctionType(session, false) ||
                regularClassSymbol?.isSyntheticComposableFunction() == true ||
                expandedType.isString -> FirStability.Stable

        expandedType.isNonReifiedTypeParameter() -> {
            val parameterSymbol = expandedType.lookupTag.symbol
            val arg = substitutions[parameterSymbol]?.type
            if (arg != null) {
                firStabilityOf(arg, session, substitutions, currentlyAnalyzing)
            } else {
                FirStability.Parameter(parameterSymbol)
            }
        }

        expandedType.isMarkedNullable -> {
            val type = expandedType.withNullability(nullable = false, session.typeContext)
            firStabilityOf(
                coneKotlinType = type,
                session = session,
                substitutions = substitutions,
                currentlyAnalyzing = currentlyAnalyzing
            )
        }

        regularClassSymbol?.isInlineOrValueClass() == true -> {
            if (regularClassSymbol.hasAnnotationSafe(ComposeClassId.StableMarker, session)) {
                FirStability.Stable
            } else {
                val type = expandedType.getUnsubstitutedUnderlyingType() as? ConeKotlinType
                if (type != null) {
                    firStabilityOf(
                        coneKotlinType = type,
                        session = session,
                        substitutions = substitutions,
                        currentlyAnalyzing = currentlyAnalyzing
                    )
                } else FirStability.Unstable
            }
        }

        expandedType is ConeClassLikeType -> {
            val classSymbol = expandedType.lookupTag.toRegularClassSymbol(session)
            if (classSymbol != null) {
                firStabilityOf(
                    classSymbol,
                    session,
                    substitutions + classSymbol.substitutionMap(coneKotlinType),
                    currentlyAnalyzing
                )
            } else FirStability.Unstable
        }

        expandedType is ConeTypeParameterType -> {
            FirStability.Unstable
        }
//      isAlias -> we expanded type early
        else -> error("Unexpected coneKotlinType: $coneKotlinType")
    }
}


private fun retrieveParameterMask(
    classSymbol: FirRegularClassSymbol,
    session: FirSession,
    substitutions: Map<FirTypeParameterSymbol, ConeKotlinTypeProjection>,
    currentlyAnalyzing: Set<FirClassifierSymbol<*>>
): Int? {

    if (
        classSymbol.visibility !== Visibilities.Public ||
        classSymbol.isEnumClass ||
        classSymbol.isEnumEntry ||
        classSymbol.isInterface ||
        classSymbol.classKind == ClassKind.ANNOTATION_CLASS ||
        classSymbol.name == SpecialNames.NO_NAME_PROVIDED ||
        classSymbol.isExpect ||
        classSymbol.isInner ||
        classSymbol.isCompanion ||
        classSymbol.isInlineOrValueClass() ||
        classSymbol.defaultType().isAny
    ) return null

    val stability =
        firStabilityOf(classSymbol, session, substitutions, currentlyAnalyzing).normalize()

    var parameterMask = 0

    if (classSymbol.typeParameterSymbols.isNotEmpty()) {

        stability.forEach {
            when (it) {
                is FirStability.Parameter -> {
                    val index = classSymbol.typeParameterSymbols.indexOf(it.parameter)
                    if (index != -1) {
                        // the stability of this parameter matters for the stability of the
                        // class
                        parameterMask = parameterMask or 0b1 shl index
                    }
                }

                else -> {
                    /* No action necessary */
                }
            }
        }
    }
    return parameterMask
}

private fun FirClassifierSymbol<*>.substitutionMap(coneKotlinType: ConeKotlinType): Map<FirTypeParameterSymbol, ConeKotlinTypeProjection> {
    val params = typeParameterSymbols
    val args = coneKotlinType.typeArguments
    return params?.zip(args)?.filter { (_, arg) -> arg is ConeKotlinTypeProjection }
        ?.associateTo(mutableMapOf()) { (param, arg) -> param to arg as ConeKotlinTypeProjection }
        .orEmpty()
}

private fun FirRegularClassSymbol.isSyntheticComposableFunction() =
    classId.asFqNameString().startsWith("androidx.compose.runtime.internal.ComposableFunction")