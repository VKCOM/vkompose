package com.vk.gradle.plugin.compose.test.tag.cleaner

import com.vk.compose_test_tag_cleaner.cleaner.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ComposeTestTagCleanerPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("composeTestTagCleaner", ComposeTestTagCleanerExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension =
            project.extensions.findByType(ComposeTestTagCleanerExtension::class.java)
                ?: project.extensions.create(
                    "composeTestTagCleaner",
                    ComposeTestTagCleanerExtension::class.java
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

    override fun getCompilerPluginId(): String = "com.vk.compose-test-tag-cleaner.compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.vk.compose-test-tag-cleaner",
        artifactId = "compiler-plugin",
        version = BuildConfig.VERSION
    )

}