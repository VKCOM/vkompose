package com.vk.gradle.plugin.compose.utils.settings

import com.vk.gradle.plugin.composable.skippability.checker.ComposableSkippabilityCheckerExtension
import com.vk.gradle.plugin.compose.source.information.cleaner.SourceInformationCleanerExtension
import com.vk.gradle.plugin.compose.test.tag.applier.ComposeTestTagApplierExtension
import com.vk.gradle.plugin.compose.test.tag.cleaner.ComposeTestTagCleanerExtension
import com.vk.gradle.plugin.compose.test.tag.drawer.ComposeTestTagDrawerExtension
import com.vk.gradle.plugin.recompose.highlighter.RecomposeHighlighterExtension
import com.vk.gradle.plugin.recompose.logger.RecomposeLoggerExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

// properties are sorted in applying order

@SettingsMarker
sealed class Setting {
    open val isEnabled: Boolean = false

    internal abstract fun setup(project: Project)
}

class RecomposeSetting : Setting() {
    override val isEnabled: Boolean = true

    @JvmField
    var isHighlighterEnabled = false
    @JvmField
    var isLoggerEnabled = false

    override fun setup(project: Project) {
        project.extensions.getByType<RecomposeHighlighterExtension>().isEnabled = isHighlighterEnabled
        project.extensions.getByType<RecomposeLoggerExtension>().isEnabled = isLoggerEnabled
    }
}

class TestTagSetting : Setting() {
    override val isEnabled: Boolean = true

    @JvmField
    var isApplierEnabled = false
    @JvmField
    var isCleanerEnabled = false
    @JvmField
    var isDrawerEnabled = false

    override fun setup(project: Project) {
        project.extensions.getByType<ComposeTestTagApplierExtension>().isEnabled = isApplierEnabled
        project.extensions.getByType<ComposeTestTagCleanerExtension>().isEnabled = isCleanerEnabled
        project.extensions.getByType<ComposeTestTagDrawerExtension>().isEnabled = isDrawerEnabled
    }
}

class SourceInfoCleanSetting : Setting() {
    override var isEnabled: Boolean = true

    override fun setup(project: Project) {
        project.extensions.getByType<SourceInformationCleanerExtension>().isEnabled = isEnabled
    }
}

class SkippabilityChecksSetting : Setting() {
    override var isEnabled: Boolean = true

    @JvmField
    var stabilityConfigurationPath: String? = null

    override fun setup(project: Project) {
        project.extensions.getByType<ComposableSkippabilityCheckerExtension>().apply {
            stabilityConfigurationPath = this@SkippabilityChecksSetting.stabilityConfigurationPath
            isEnabled = this@SkippabilityChecksSetting.isEnabled
        }
    }
}