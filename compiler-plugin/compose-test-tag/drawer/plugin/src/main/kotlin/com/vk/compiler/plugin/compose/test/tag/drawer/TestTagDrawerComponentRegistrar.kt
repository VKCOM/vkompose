package com.vk.compiler.plugin.compose.test.tag.drawer

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class TestTagDrawerComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        if (configuration.get(TestTagDrawerCommandLineProcessor.ENABLED, true)) {
            project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
                .registerExtension(TestTagDrawerIrGeneration(), LoadingOrder.FIRST)
        }
    }

    override val supportsK2: Boolean = true

}