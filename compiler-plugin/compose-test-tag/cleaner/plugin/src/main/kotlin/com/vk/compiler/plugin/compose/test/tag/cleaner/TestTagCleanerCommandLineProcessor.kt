package com.vk.compiler.plugin.compose.test.tag.cleaner

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class TestTagCleanerCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.vk.compose-test-tag-cleaner.compiler-plugin"
    override val pluginOptions: Collection<AbstractCliOption> =
        listOf(
            CliOption(
                "enabled",
                "<true|false>",
                "clean Composables test tags",
                required = false
            )
        )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "enabled" -> configuration.put(ENABLED, value.toBoolean())
        }
    }

    companion object {
        val ENABLED = CompilerConfigurationKey<Boolean>("Enable Composable Test Tag Cleaner")
    }
}