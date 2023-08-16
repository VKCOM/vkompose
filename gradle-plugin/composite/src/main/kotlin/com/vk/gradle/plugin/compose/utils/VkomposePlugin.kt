package com.vk.gradle.plugin.compose.utils

import com.vk.gradle.plugin.compose.utils.settings.RecomposeSetting
import com.vk.gradle.plugin.compose.utils.settings.SkippabilityChecksSetting
import com.vk.gradle.plugin.compose.utils.settings.SourceInfoCleanSetting
import com.vk.gradle.plugin.compose.utils.settings.TestTagSetting
import org.gradle.api.Plugin
import org.gradle.api.Project

class VkomposePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions.create("vkompose", VkomposeExtension::class.java)
        val settings = extension.settings

        target.afterEvaluate {
            settings[RecomposeSetting::class]?.setup(target)
            settings[TestTagSetting::class]?.setup(target)
            settings[SourceInfoCleanSetting::class]?.setup(target)
            settings[SkippabilityChecksSetting::class]?.setup(target)
        }
    }
}