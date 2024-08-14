package com.vk.gradle.plugin.composable.skippability.checker

import com.vk.composable_skippability_checker.composable_skippability_checker.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ComposableSkippabilityCheckerPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create(
            "composableSkippabilityChecker",
            ComposableSkippabilityCheckerExtension::class.java
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension =
            project.extensions.findByType(ComposableSkippabilityCheckerExtension::class.java)
                ?: project.extensions.create(
                    "composableSkippabilityChecker",
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

                if (extension.stabilityConfigurationPath.isNullOrEmpty().not()) {
                    add(
                        SubpluginOption(
                            "stabilityConfigurationPath",
                            extension.stabilityConfigurationPath.orEmpty()
                        )
                    )
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

}