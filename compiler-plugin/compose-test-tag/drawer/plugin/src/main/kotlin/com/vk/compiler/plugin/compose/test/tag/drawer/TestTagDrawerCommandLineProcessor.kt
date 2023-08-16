package com.vk.compiler.plugin.compose.test.tag.drawer

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class TestTagDrawerCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.vk.compose-test-tag-drawer.compiler-plugin"
    override val pluginOptions: Collection<AbstractCliOption> =
        listOf(
            CliOption(
                "enabled",
                "<true|false>",
                "draw actual testTag on screen by long tap",
                required = false
            )

        )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "enabled" -> configuration.put(ENABLED, value.toBoolean())
        }
    }

    companion object {
        val ENABLED = CompilerConfigurationKey<Boolean>("Enable Composable Test Tag Drawing")
    }
}