package com.vk.gradle.plugin.compose.test.tag.applier

import com.vk.compose_test_tag_applier.applier.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ComposeTestTagApplierPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create(EXTENSION_NAME, ComposeTestTagApplierExtension::class.java)
        target.applyRuntimeDependency()
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        return project.provider {
            listOf(SubpluginOption("enabled", project.getExtension().isEnabled.toString()))
            listOf(SubpluginOption("tagTemplate", project.getExtension().tagTemplate))
        }
    }

    override fun getCompilerPluginId(): String = "com.vk.compose-test-tag-applier.compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.vk.compose-test-tag-applier",
        artifactId = "compiler-plugin",
        version = BuildConfig.VERSION
    )


    private fun Project.applyRuntimeDependency() = afterEvaluate {
        if (getExtension().isEnabled) {
            dependencies {
                add(
                    "implementation",
                    "com.vk.compose-test-tag-applier:compiler-runtime:${BuildConfig.VERSION}"
                )
            }
        }

    }

    private fun Project.getExtension(): ComposeTestTagApplierExtension {
        return project.extensions.findByType(ComposeTestTagApplierExtension::class.java)
            ?: project.extensions.create(EXTENSION_NAME, ComposeTestTagApplierExtension::class.java)
    }

    companion object {
        private const val EXTENSION_NAME = "composeTestTagApplier"
    }

}