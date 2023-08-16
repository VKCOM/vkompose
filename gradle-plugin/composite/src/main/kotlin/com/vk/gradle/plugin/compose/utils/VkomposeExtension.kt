package com.vk.gradle.plugin.compose.utils

import com.vk.gradle.plugin.compose.utils.settings.RecomposeSetting
import com.vk.gradle.plugin.compose.utils.settings.Setting
import com.vk.gradle.plugin.compose.utils.settings.SkippabilityChecksSetting
import com.vk.gradle.plugin.compose.utils.settings.SourceInfoCleanSetting
import com.vk.gradle.plugin.compose.utils.settings.TestTagSetting
import kotlin.reflect.KClass

abstract class VkomposeExtension {
    internal val settings = mutableMapOf<KClass<out Setting>, Setting>()

    var sourceInformationClean: Boolean
        get() = settings[SourceInfoCleanSetting::class]?.isEnabled ?: false
        set(value) {
            settings[SourceInfoCleanSetting::class] = SourceInfoCleanSetting().apply {
                isEnabled = value
            }
        }

    var skippabilityCheck: Boolean
        get() = settings[SkippabilityChecksSetting::class]?.isEnabled ?: false
        set(value) {
            settings[SkippabilityChecksSetting::class] = SkippabilityChecksSetting().apply {
                isEnabled = value
            }
        }

    fun recompose(configure: RecomposeSetting.() -> Unit) {
        settings[RecomposeSetting::class] = RecomposeSetting().apply { configure() }
    }

    fun testTag(configure: TestTagSetting.() -> Unit) {
        settings[TestTagSetting::class] = TestTagSetting().apply { configure() }
    }

    fun skippabilityCheck(configure: SkippabilityChecksSetting.() -> Unit) {
        settings[SkippabilityChecksSetting::class] = SkippabilityChecksSetting().apply { configure() }
    }
}