package com.vk.compiler.plugin.composable.skippability.checker

import com.vk.compiler.plugin.composable.skippability.checker.ir.SkippabilityChecker
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class SkippabilityComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        if (configuration.get(SkippabilityCommandLineProcessor.ENABLED, true)) {

            val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

            val stabilityConfigPath = configuration.get(SkippabilityCommandLineProcessor.STABILITY_CONFIG_PATH_KEY, "")
            val stableTypeMatchers = try {
                StabilityConfigParser.fromFile(stabilityConfigPath).stableTypeMatchers
            } catch (e: Exception) {
                messageCollector.report(CompilerMessageSeverity.ERROR, e.message ?: "Error parsing stability configuration")
                emptySet()
            }

            project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
                .registerExtension(SkippabilityChecker(messageCollector, stableTypeMatchers), LoadingOrder.LAST)

//            FirExtensionRegistrarAdapter.registerExtension(project, SkippabilityCheckerFirExtensionRegistrar())
        }
    }

    override val supportsK2: Boolean = true

}
