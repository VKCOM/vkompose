package com.vk.compiler.plugin.compose.test.tag.applier

import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.DEFAULT_TAG_TEMPLATE
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class TestTagApplierComponentRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "com.vk.compose-test-tag-applier.compiler-plugin"

    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        if (configuration.get(TestTagApplierCommandLineProcessor.ENABLED, true)) {
            IrGenerationExtension.registerExtension(
                TestTagApplierIrGeneration(
                    configuration.get(TestTagApplierCommandLineProcessor.TAG_TEMPLATE, DEFAULT_TAG_TEMPLATE)
                )
            )
        }
    }

}