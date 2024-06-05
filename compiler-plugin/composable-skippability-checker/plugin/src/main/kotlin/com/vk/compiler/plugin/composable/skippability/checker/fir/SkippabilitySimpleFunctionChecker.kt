package com.vk.compiler.plugin.composable.skippability.checker.fir

import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassId
import com.vk.compiler.plugin.composable.skippability.checker.Keys
import com.vk.compiler.plugin.composable.skippability.checker.fir.SkippabilityCheckerWarnings.NON_SKIPPABLE_FUNCTION
import com.vk.compiler.plugin.composable.skippability.checker.fir.SkippabilityCheckerWarnings.UNSTABLE_FUNCTION_PARAM
import com.vk.compiler.plugin.composable.skippability.checker.fir.SkippabilityCheckerWarnings.UNUSED_SKIPPABLE_SUPPRESS
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.UnexpandedTypeCheck
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal object SkippabilitySimpleFunctionChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {

    override fun check(
        declaration: FirSimpleFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val session = context.session
//        val isComposable = declaration.hasAnnotationSafe(ComposeClassId.Composable)
        val isComposable = declaration.hasAnnotationSafe(ComposeClassId.Composable, session)

        if (!isComposable || !declaration.isRestartable(session)) return

        val notSkippableParams = mutableSetOf<FirValueParameter>()
        for (valueParameter in declaration.valueParameters) {

            val returnTypeRef = valueParameter.returnTypeRef

            val isRequired = valueParameter.defaultValue == null
            val isUnstable = firStabilityOf(returnTypeRef, session).knownUnstable()
            val isUsed = true // TODO check declaration.body.statements.source.text
            val packageFqName = returnTypeRef.coneTypeSafe<ConeKotlinType>()?.classId?.packageFqName
            val isFromCompose = packageFqName?.startsWith(composePackage) == true

            if (!isFromCompose && isUsed && isUnstable && isRequired) {
                notSkippableParams += valueParameter
            }
        }

        val suppressAnnotation = declaration.annotations.any { annotation ->
            val isSuppress = annotation.toAnnotationClassIdSafe(session)?.asFqNameString()
                ?.contains(Keys.SUPPRESS) == true
            val callAnnotation = annotation as? FirAnnotationCall

            if (callAnnotation == null || !isSuppress) return@any false

            callAnnotation.arguments.any { it.source?.lighterASTNode.toString().contains(Keys.NON_SKIPPABLE_COMPOSABLE) }
        }



        when {
            !suppressAnnotation && notSkippableParams.isNotEmpty() -> {
                reporter.reportOn(
                    declaration.source,
                    NON_SKIPPABLE_FUNCTION,
                    context
                )
                notSkippableParams.forEach {
                    reporter.reportOn(
                        it.source,
                        UNSTABLE_FUNCTION_PARAM,
                        context
                    )
                }
            }

            suppressAnnotation && notSkippableParams.isEmpty() -> {
                reporter.reportOn(
                    declaration.source,
                    UNUSED_SKIPPABLE_SUPPRESS,
                    context
                )
            }
        }
    }


    @OptIn(UnexpandedTypeCheck::class)
    private fun FirSimpleFunction.isRestartable(session: FirSession): Boolean = when {
        isLocal -> false
        isInline -> false
        hasAnnotationSafe(ComposeClassId.NonRestartableComposable, session) -> false
        hasAnnotationSafe(ComposeClassId.ExplicitGroupsComposable, session) -> false
        isLambda -> false
        !returnTypeRef.isUnit -> false
        isComposableDelegatedAccessor(session) -> false
        else -> true
    }

    private fun FirSimpleFunction.isComposableDelegatedAccessor(session: FirSession): Boolean =
        origin == FirDeclarationOrigin.Delegated &&
                body?.let {
                    val returnStatement = it.statements.singleOrNull() as? FirCallableDeclaration
                    val target = returnStatement?.symbol
                    target?.hasAnnotationSafe(ComposeClassId.Composable, session)
                } == true

    private val FirSimpleFunction.isLambda: Boolean
        get() = name == SpecialNames.ANONYMOUS

    private val composePackage = Name.identifier("androidx.compose")
}