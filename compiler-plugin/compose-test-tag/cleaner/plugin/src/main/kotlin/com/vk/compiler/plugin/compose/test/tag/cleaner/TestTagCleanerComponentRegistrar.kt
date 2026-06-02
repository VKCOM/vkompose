package com.vk.compiler.plugin.compose.test.tag.cleaner

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class TestTagCleanerComponentRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "com.vk.compose-test-tag-cleaner.compiler-plugin"

    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        if (configuration.get(TestTagCleanerCommandLineProcessor.ENABLED, true)) {
            IrGenerationExtension.registerExtension(TestTagCleanerIrGeneration(),)
        }
    }

}