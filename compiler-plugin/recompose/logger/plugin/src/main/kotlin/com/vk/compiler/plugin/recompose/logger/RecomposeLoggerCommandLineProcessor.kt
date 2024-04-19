package com.vk.compiler.plugin.recompose.logger

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class RecomposeLoggerCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.vk.recompose-logger.compiler-plugin"
    override val pluginOptions: Collection<AbstractCliOption> =
        listOf(
            CliOption(
                "enabled",
                "<true|false>",
                "Is Recompose Logger enabled",
                required = false
            ),
            CliOption(
                "logModifierChanges",
                "<true|false>",
                "Experimental feature to log changes of modifier arguments in composable function",
                required = false
            ),
            CliOption(
                "logFunctionChanges",
                "<true|false>",
                "Experimental feature to log changes of function arguments in composable function",
                required = false
            )
        )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "enabled" -> configuration.put(ENABLED, value.toBoolean())
            "logModifierChanges" -> configuration.put(LOG_MODIFIER_CHANGES, value.toBoolean())
            "logFunctionChanges" -> configuration.put(LOG_MODIFIER_CHANGES, value.toBoolean())
        }
    }

    companion object {
        val ENABLED = CompilerConfigurationKey<Boolean>("Enable Recompose Logger")
        val LOG_MODIFIER_CHANGES = CompilerConfigurationKey<Boolean>("Enable Recompose logs for modifiers changes")
        val LOG_FUNCTION_CHANGES = CompilerConfigurationKey<Boolean>("Enable Recompose logs for functions arguments changes")
    }
}
