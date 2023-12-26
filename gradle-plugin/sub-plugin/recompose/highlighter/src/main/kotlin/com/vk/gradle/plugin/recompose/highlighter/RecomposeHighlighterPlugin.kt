package com.vk.gradle.plugin.recompose.highlighter

import com.vk.recompose_highlighter.highlighter.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RecomposeHighlighterPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("recomposeHighlighter", RecomposeHighlighterExtension::class.java)
        target.applyRuntimeDependency()
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension =
            project.extensions.findByType(RecomposeHighlighterExtension::class.java)
                ?: project.extensions.create(
                    "recomposeHighlighter",
                    RecomposeHighlighterExtension::class.java
                )

        return project.provider {
            listOf(
                SubpluginOption(
                    "enabled",
                    extension.isEnabled.toString()
                )
            )
        }
    }

    override fun getCompilerPluginId(): String = "com.vk.recompose-highlighter.compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.vk.recompose-highlighter",
        artifactId = "compiler-plugin",
        version = BuildConfig.VERSION
    )

    private fun Project.applyRuntimeDependency() {
        dependencies {
            add("implementation", "com.vk.recompose-highlighter:compiler-runtime:${BuildConfig.VERSION}")
        }
    }

}