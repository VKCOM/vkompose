package com.vk.gradle.plugin.compose.test.tag.drawer

import com.vk.compose_test_tag_drawer.drawer.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ComposeTestTagDrawerPlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("composeTestTagDrawer", ComposeTestTagDrawerExtension::class.java)

        target.applyRuntimeDependency()
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension =
            project.extensions.findByType(ComposeTestTagDrawerExtension::class.java)
                ?: project.extensions.create(
                    "composeTestTagDrawer",
                    ComposeTestTagDrawerExtension::class.java
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

    override fun getCompilerPluginId(): String = "com.vk.compose-test-tag-drawer.compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.vk.compose-test-tag-drawer",
        artifactId = "compiler-plugin",
        version = BuildConfig.VERSION
    )


    private fun Project.applyRuntimeDependency() {
        dependencies {
            add("implementation", "com.vk.compose-test-tag-drawer:compiler-runtime:${BuildConfig.VERSION}")
        }
    }

}