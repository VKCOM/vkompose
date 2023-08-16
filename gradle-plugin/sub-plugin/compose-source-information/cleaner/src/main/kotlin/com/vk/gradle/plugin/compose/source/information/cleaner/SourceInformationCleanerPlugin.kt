package com.vk.gradle.plugin.compose.source.information.cleaner

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class SourceInformationCleanerPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("composeSourceInformationCleaner", SourceInformationCleanerExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension = project.extensions.findByType(SourceInformationCleanerExtension::class.java)
                ?: project.extensions.create(
                    "composeSourceInformationCleaner",
                    SourceInformationCleanerExtension::class.java
                )

        return project.provider {
            listOf(
                SubpluginOption(
                    "enabled",
                    extension.isEnabled.toString()
                ),
            )
        }
    }

    override fun getCompilerPluginId(): String = "com.vk.compose-source-information-cleaner.compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.vk.compose-source-information-cleaner",
        artifactId = "compiler-plugin",
        version = "0.1"
    )

}