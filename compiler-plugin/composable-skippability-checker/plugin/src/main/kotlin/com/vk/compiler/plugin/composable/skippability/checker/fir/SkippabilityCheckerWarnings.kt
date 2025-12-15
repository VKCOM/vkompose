package com.vk.compiler.plugin.composable.skippability.checker.fir

import com.vk.compiler.plugin.composable.skippability.checker.Messages.REMOVE_SKIPPABILITY_ANNOTATION
import com.vk.compiler.plugin.composable.skippability.checker.Messages.SKIPPABILITY_FIX_EXPLANATION
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning0

object SkippabilityCheckerWarnings : KtDiagnosticsContainer() {
    val NON_SKIPPABLE_FUNCTION by warning0<PsiElement>()
    val UNUSED_SKIPPABLE_SUPPRESS by warning0<PsiElement>()
    val UNSTABLE_FUNCTION_PARAM by warning0<PsiElement>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = WarningMessages

    private object WarningMessages : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("StabilityChecker") { map ->
            map.put(
                factory = NON_SKIPPABLE_FUNCTION,
                message = SKIPPABILITY_FIX_EXPLANATION
            )
            map.put(
                factory = UNUSED_SKIPPABLE_SUPPRESS,
                message = REMOVE_SKIPPABILITY_ANNOTATION
            )
            map.put(
                factory = UNSTABLE_FUNCTION_PARAM,
                message = "Unstable type of parameter"
            )
        }
    }


}