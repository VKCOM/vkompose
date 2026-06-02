package com.vk.compiler.plugin.compose.source.information.cleaner

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class SourceInformationComponentRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "com.vk.compose-source-information-cleaner.compiler-plugin"

    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        if (configuration.get(SourceInformationCommandLineProcessor.ENABLED, true)) {
            IrGenerationExtension.registerExtension(SourceInformationIrGeneration())
        }
    }

}