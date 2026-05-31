package com.vk.compiler.plugin.composable.skippability.checker

import androidx.compose.compiler.plugins.kotlin.analysis.FqNameMatcher
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityConfigParser
import com.vk.compiler.plugin.composable.skippability.checker.ir.SkippabilityChecker
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.FileNotFoundException

@ExperimentalCompilerApi
class SkippabilityComponentRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "com.vk.composable-skippability-checker.compiler-plugin"

    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        if (configuration.get(SkippabilityCommandLineProcessor.ENABLED, true)) {

            val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

            val isStrongSkippingModeEnabled = configuration.get(SkippabilityCommandLineProcessor.STRONG_SKIPPING_MODE_ENABLED, false)
            val isStrongSkippingFailFastEnabled = configuration.get(SkippabilityCommandLineProcessor.STRONG_SKIPPING_MODE_FAIL_FAST_ENABLED, false)
            val stabilityConfigPaths = configuration.getList(SkippabilityCommandLineProcessor.STABILITY_CONFIG_PATH_KEY)
            val stableTypeMatchers = stabilityConfigPaths.flatMapTo(mutableSetOf()) { path ->
                try {
                    StabilityConfigParser.fromFile(path).stableTypeMatchers
                } catch (e: Exception) {
                    messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        e.message ?: "Error parsing stability configuration"
                    )
                    emptySet()
                }
            }

            IrGenerationExtension.registerExtension(
                SkippabilityChecker(
                    isStrongSkippingModeEnabled,
                    isStrongSkippingFailFastEnabled,
                    messageCollector,
                    stableTypeMatchers
                )
            )
            //            FirExtensionRegistrarAdapter.registerExtension(project, SkippabilityCheckerFirExtensionRegistrar())
        }
    }

}
