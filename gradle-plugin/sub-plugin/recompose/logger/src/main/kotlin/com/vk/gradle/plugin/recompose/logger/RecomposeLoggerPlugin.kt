package com.vk.gradle.plugin.recompose.logger

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RecomposeLoggerPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("recomposeLogger", RecomposeLoggerExtension::class.java)
        target.applyRuntimeDependency()
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension =
            project.extensions.findByType(RecomposeLoggerExtension::class.java)
                ?: project.extensions.create(
                    "recomposeLogger",
                    RecomposeLoggerExtension::class.java
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

    override fun getCompilerPluginId(): String = "com.vk.recompose-logger.compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.vk.recompose-logger",
        artifactId = "compiler-plugin",
        version = "0.1"
    )

    private fun Project.applyRuntimeDependency() {
        dependencies {
            add("implementation", "com.vk.recompose-logger:compiler-runtime:0.1")
        }
    }

}