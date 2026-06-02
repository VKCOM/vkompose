package com.vk.gradle.plugin.composable.skippability.checker

import com.vk.composable_skippability_checker.composable_skippability_checker.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ComposableSkippabilityCheckerPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create(
            EXTENSION_NAME,
            ComposableSkippabilityCheckerExtension::class.java
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        kotlinCompilation.compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xcompiler-plugin-order=androidx.compose.compiler.plugins.kotlin>${getCompilerPluginId()}")
        }

        val extension =
            project.extensions.findByType(ComposableSkippabilityCheckerExtension::class.java)
                ?: project.extensions.create(
                    EXTENSION_NAME,
                    ComposableSkippabilityCheckerExtension::class.java
                )

        return project.provider {
            buildList {
                add(
                    SubpluginOption(
                        "enabled",
                        extension.isEnabled.toString()
                    )
                )
                add(
                    SubpluginOption(
                        "enabledFir",
                        extension.isFirEnabled.toString()
                    )
                )

                if (extension.stabilityConfigurationPath.isNotEmpty()) {
                    extension.stabilityConfigurationPath.map { path ->
                        add(SubpluginOption("stabilityConfigurationPath", path))
                    }
                }
                add(
                    SubpluginOption(
                        "strongSkippingFailFastEnabled",
                        extension.strongSkippingFailFastEnabled.toString()
                    )
                )
                add(
                    SubpluginOption(
                        "strongSkippingEnabled",
                        extension.strongSkippingEnabled.toString()
                    )
                )
            }
        }
    }

    override fun getCompilerPluginId(): String =
        "com.vk.composable-skippability-checker.compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.vk.composable-skippability-checker",
        artifactId = "compiler-plugin",
        version = BuildConfig.VERSION
    )

    companion object {
        private const val EXTENSION_NAME = "composableSkippabilityChecker"
    }

}