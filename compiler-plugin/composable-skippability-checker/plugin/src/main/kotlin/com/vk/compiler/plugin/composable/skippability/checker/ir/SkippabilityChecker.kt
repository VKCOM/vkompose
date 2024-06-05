package com.vk.compiler.plugin.composable.skippability.checker.ir

import com.vk.compiler.plugin.composable.skippability.checker.FqNameMatcher
import com.vk.compiler.plugin.composable.skippability.checker.Messages
import com.vk.compiler.plugin.composable.skippability.checker.Messages.REF_COMPARISON_FIX_EXPLANATION
import com.vk.compiler.plugin.composable.skippability.checker.Messages.REMOVE_SKIPPABILITY_ANNOTATION
import com.vk.compiler.plugin.composable.skippability.checker.Messages.SKIPPABILITY_FIX_EXPLANATION
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class SkippabilityChecker(
    private val isStrongSkippingModeEnabled: Boolean,
    private val isStrongSkippingFailFastEnabled: Boolean,
    private val messageCollector: MessageCollector,
    private val stableTypeMatchers: Set<FqNameMatcher>
) : IrGenerationExtension {


    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val unskippableFunctions = mutableSetOf<ReportFunction>()
        val fixedSuppressedFunctions = mutableSetOf<ReportFunction>()

        val stabilityInferencer = StabilityInferencer(
            @OptIn(ObsoleteDescriptorBasedAPI::class) pluginContext.moduleDescriptor,
            stableTypeMatchers,
        )

        moduleFragment.acceptChildrenVoid(
            SkippingProblemFunctionsCollector(
                isStrongSkippingModeEnabled,
                unskippableFunctions,
                fixedSuppressedFunctions,
                stabilityInferencer
            )
        )

        if (fixedSuppressedFunctions.isNotEmpty()) {
            val warningAboutFixedFunctions = buildString {
                append(REMOVE_SKIPPABILITY_ANNOTATION)
                fixedSuppressedFunctions.forEach { append("${it.path}\n") }
            }
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                warningAboutFixedFunctions,
            )
        }

        if (unskippableFunctions.isNotEmpty()) {
            if (isStrongSkippingModeEnabled) {
                val severity = when {
                    isStrongSkippingFailFastEnabled -> CompilerMessageSeverity.ERROR
                    else -> CompilerMessageSeverity.WARNING
                }
                messageCollector.report(
                    severity,
                    createRefComparisonFixDescription(unskippableFunctions),
                )
            } else {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    createSkippabilityFixDescription(unskippableFunctions),
                )
            }
        }
    }

    private fun createSkippabilityFixDescription(functions: MutableSet<ReportFunction>): String =
        buildString {
            append("SKIPPABILITY CHECK IS NOT PASSED\n\n")
            append(SKIPPABILITY_FIX_EXPLANATION)
            append("\n\nFunctions with unstable paramaters:\n")
            functions.forEach { function ->
                append("${function.path}:")
                function.params.forEach { append("\n\t $it") }
                append("\n")
            }
        }

    private fun createRefComparisonFixDescription(functions: MutableSet<ReportFunction>): String =
        buildString {
            append("Functions' paramaters will be compared by refs\n\n")
            append(REF_COMPARISON_FIX_EXPLANATION)
            append("\n\nFunctions with ref compared paramaters:\n")
            functions.forEach { function ->
                append("${function.path}:")
                function.params.forEach { append("\n\t $it") }
                append("\n")
            }
        }
}