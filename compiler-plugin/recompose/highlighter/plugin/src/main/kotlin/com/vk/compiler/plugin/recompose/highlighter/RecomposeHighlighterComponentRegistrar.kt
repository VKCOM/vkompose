package com.vk.compiler.plugin.recompose.highlighter

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class RecomposeHighlighterComponentRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "com.vk.recompose-highlighter.compiler-plugin"

    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        if (configuration.get(RecomposeHighlighterCommandLineProcessor.ENABLED, true)) {
            IrGenerationExtension.registerExtension(RecomposeHighlighterIrGeneration())
        }
    }

}