package com.vk.compiler.plugin.composable.skippability.checker.ir

import com.vk.compiler.plugin.composable.skippability.checker.FqNameMatcher
import com.vk.compiler.plugin.composable.skippability.checker.Messages.REMOVE_SKIPPABILITY_ANNOTATION
import com.vk.compiler.plugin.composable.skippability.checker.Messages.SKIPPABILITY_FIX_EXPLANATION
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class SkippabilityChecker(
    private val messageCollector: MessageCollector,
    private val stableTypeMatchers: Set<FqNameMatcher>
) : IrGenerationExtension {


    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val unskippableFunctions = mutableSetOf<ReportFunction>()
        val fixedSuppressedFunctions = mutableSetOf<ReportFunction>()

        val stabilityInferencer = StabilityInferencer(
            pluginContext.moduleDescriptor,
            stableTypeMatchers,
        )

        moduleFragment.acceptChildrenVoid(SkippableFunctionsCollector(unskippableFunctions, fixedSuppressedFunctions, stabilityInferencer))

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
            val errorAboutUnskippableFunctions = buildString {
                append("SKIPPABILITY CHECK IS NOT PASSED\n\n")
                append(SKIPPABILITY_FIX_EXPLANATION)
                append("\n\nFunctions with unstable paramaters:\n")
                unskippableFunctions.forEach { function ->
                    append("${function.path}:")
                    function.params.forEach { append("\n\t $it") }
                    append("\n")
                }
            }
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                errorAboutUnskippableFunctions,
            )
        }
    }
}