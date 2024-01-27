package com.vk.compiler.plugin.composable.skippability.checker

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class SkippabilityCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.vk.composable-skippability-checker.compiler-plugin"
    override val pluginOptions: Collection<AbstractCliOption> =
        listOf(
            CliOption(
                "enabled",
                "<true|false>",
                "Check unstable params in functions that doesn`t allow them to be skippable",
                required = false
            ),
            CliOption(
                "stabilityConfigurationPath",
                "<path>",
                "Path to stability configuration file",
                required = false,
                allowMultipleOccurrences = true
            )
        )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            "enabled" -> configuration.put(ENABLED, value.toBoolean())
            "stabilityConfigurationPath" -> configuration.put(STABILITY_CONFIG_PATH_KEY, value)
        }
    }

    companion object {
        val STABILITY_CONFIG_PATH_KEY = CompilerConfigurationKey<String>("Path to stability configuration file")
        val ENABLED = CompilerConfigurationKey<Boolean>("Enable checking stability of functions parameters that doesn`t allow them to be skippable")
    }
}
