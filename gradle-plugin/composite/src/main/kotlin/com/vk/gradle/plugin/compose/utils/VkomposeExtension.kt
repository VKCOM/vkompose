package com.vk.gradle.plugin.compose.utils

import com.vk.gradle.plugin.compose.utils.settings.RecomposeSetting
import com.vk.gradle.plugin.compose.utils.settings.SkippabilityChecksSetting
import com.vk.gradle.plugin.compose.utils.settings.SourceInfoCleanSetting
import com.vk.gradle.plugin.compose.utils.settings.TestTagSetting
import org.gradle.api.Project

abstract class VkomposeExtension {
    internal val settings = mutableMapOf(
        RecomposeSetting::class to RecomposeSetting(),
        TestTagSetting::class to TestTagSetting(),
        SourceInfoCleanSetting::class to SourceInfoCleanSetting().apply { isEnabled = false },
        SkippabilityChecksSetting::class to SkippabilityChecksSetting().apply { isEnabled = false },
    )

    var Project.sourceInformationClean: Boolean
        get() = settings[SourceInfoCleanSetting::class]?.isEnabled ?: false
        set(value) {
            val setting = SourceInfoCleanSetting().apply {
                isEnabled = value
            }
            setting.setup(this)
            settings[SourceInfoCleanSetting::class] = setting
        }

    var Project.skippabilityCheck: Boolean
        get() = settings[SkippabilityChecksSetting::class]?.isEnabled ?: false
        set(value) {
            val setting = SkippabilityChecksSetting().apply {
                isEnabled = value
            }
            setting.setup(this)
            settings[SkippabilityChecksSetting::class] = setting
        }

    fun Project.recompose(configure: RecomposeSetting.() -> Unit) {
        val setting = RecomposeSetting().apply(configure)
        setting.setup(this)
        settings[RecomposeSetting::class] = setting
    }

    fun Project.testTag(configure: TestTagSetting.() -> Unit) {
        val setting = TestTagSetting().apply(configure)
        setting.setup(this)
        settings[TestTagSetting::class] = setting
    }

    fun Project.skippabilityCheck(configure: SkippabilityChecksSetting.() -> Unit) {
        val setting = SkippabilityChecksSetting().apply(configure)
        setting.setup(this)
        settings[SkippabilityChecksSetting::class] = setting
    }
}