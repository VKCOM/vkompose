package com.vk.compiler.plugin.compose.test.tag.applier

import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.CALLING_FUNCTION_NAME_PLACEHOLDER
import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.CALLING_FUNCTION_OFFSET_PLACEHOLDER
import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.DEFAULT_TAG_TEMPLATE
import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.FILENAME_PLACEHOLDER
import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.OUTER_FUNCTION_NAME_PLACEHOLDER_SAMPLE
import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.PARENT_FUNCTION_NAME_PLACEHOLDER
import com.vk.compiler.plugin.compose.test.tag.applier.TestTagApplier.Companion.PARENT_FUNCTION_OFFSET_PLACEHOLDER
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class TestTagApplierCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.vk.compose-test-tag-applier.compiler-plugin"
    override val pluginOptions: Collection<AbstractCliOption> =
        listOf(
            CliOption(
                "enabled",
                "<true|false>",
                "apply test tag to Composables if it is not specified",
                required = false
            ),
            CliOption(
                "tagTemplate",
                DEFAULT_TAG_TEMPLATE,
                """
                    Use these placeholders to generate tag:
                        $FILENAME_PLACEHOLDER
                        $PARENT_FUNCTION_NAME_PLACEHOLDER
                        $PARENT_FUNCTION_OFFSET_PLACEHOLDER
                        $OUTER_FUNCTION_NAME_PLACEHOLDER_SAMPLE - all groups are optional
                        $CALLING_FUNCTION_NAME_PLACEHOLDER
                        $CALLING_FUNCTION_OFFSET_PLACEHOLDER
                    
                    
                    'range' in %outer_function_name% can be set as:
                        range=: or range=1: - include all outer functions. default value
                        range=2: - skip first function 
                        range=-1: - last function
                        range=1:2 - first and second functions
                """.trimIndent(),
                required = false,
            )

        )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "enabled" -> configuration.put(ENABLED, value.toBoolean())
            "tagTemplate" -> configuration.put(TAG_TEMPLATE, value.takeIf { it.isNotBlank() } ?: DEFAULT_TAG_TEMPLATE)
        }
    }

    companion object {
        val ENABLED = CompilerConfigurationKey<Boolean>("Enable Composable Test Tag Applier")
        val TAG_TEMPLATE = CompilerConfigurationKey<String>("Configure Composable Test Tag Generation")
    }
}