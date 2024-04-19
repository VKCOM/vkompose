package com.vk.compiler.plugin.recompose.logger

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@ExperimentalCompilerApi
class RecomposeLoggerComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        if (configuration.get(RecomposeLoggerCommandLineProcessor.ENABLED, true)) {
            val logModifierChanges = configuration.get(RecomposeLoggerCommandLineProcessor.LOG_MODIFIER_CHANGES, true)
            val logFunctionChanges = configuration.get(RecomposeLoggerCommandLineProcessor.LOG_FUNCTION_CHANGES, true)
            project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
                .registerExtension(RecomposeLoggerIrGeneration(logModifierChanges, logFunctionChanges), LoadingOrder.FIRST)
        }
    }

    override val supportsK2: Boolean = true

}