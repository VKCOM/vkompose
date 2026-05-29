package com.vk.compiler.plugin.compose.test.tag.drawer

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class TestTagDrawerComponentRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "com.vk.compose-test-tag-drawer.compiler-plugin"

    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        if (configuration.get(TestTagDrawerCommandLineProcessor.ENABLED, true)) {
            IrGenerationExtension.registerExtension(TestTagDrawerIrGeneration())
        }
    }

}