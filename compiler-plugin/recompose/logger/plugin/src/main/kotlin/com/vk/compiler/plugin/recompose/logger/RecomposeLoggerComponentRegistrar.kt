package com.vk.compiler.plugin.recompose.logger

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class RecomposeLoggerComponentRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "com.vk.recompose-logger.compiler-plugin"

    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        if (configuration.get(RecomposeLoggerCommandLineProcessor.ENABLED, true)) {
            val logModifierChanges = configuration.get(RecomposeLoggerCommandLineProcessor.LOG_MODIFIER_CHANGES, true)
            val logFunctionChanges = configuration.get(RecomposeLoggerCommandLineProcessor.LOG_FUNCTION_CHANGES, true)
            IrGenerationExtension.registerExtension(
                RecomposeLoggerIrGeneration(logModifierChanges, logFunctionChanges)
            )
        }
    }

}