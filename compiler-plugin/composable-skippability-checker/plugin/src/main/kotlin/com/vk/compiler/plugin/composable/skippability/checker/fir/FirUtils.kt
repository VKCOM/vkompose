package com.vk.compiler.plugin.composable.skippability.checker.fir

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.name.ClassId
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


/**
 * Function, KFunction
 * private in [FunctionTypeUtils]
 */
internal fun ConeKotlinType.isFunctionOrKFunctionType(session: FirSession, errorOnNotFunctionType: Boolean): Boolean {
    return isFunctionTypeWithPredicate(session, errorOnNotFunctionType) { it.isBasicFunctionOrKFunction }
}

private inline fun ConeKotlinType.isFunctionTypeWithPredicate(
    session: FirSession,
    errorOnNotFunctionType: Boolean = false,
    predicate: (FunctionTypeKind) -> Boolean
): Boolean {
    val kind = functionTypeKind(session)
        ?: if (errorOnNotFunctionType) error("$this is not a function type") else return false
    return predicate(kind)
}


/**
 * version of [isNonReifiedTypeParameter] from [FirCastDiagnosticsHelpers] but with contract which cast type
 */
@OptIn(ExperimentalContracts::class)
internal fun ConeKotlinType.isNonReifiedTypeParameter(): Boolean {
    contract {
        returns(true) implies (this@isNonReifiedTypeParameter is ConeTypeParameterType)
    }
    return this is ConeTypeParameterType && !this.lookupTag.typeParameterSymbol.isReified
}

/**
 * missed function in [FirAnnotationUtils]
 */
fun FirBasedSymbol<*>.hasAnnotationSafe(classId: ClassId, session: FirSession): Boolean {
    return resolvedAnnotationsWithClassIds.hasAnnotationSafe(classId, session)
}