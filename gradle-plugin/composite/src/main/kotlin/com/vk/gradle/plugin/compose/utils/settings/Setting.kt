package com.vk.gradle.plugin.compose.utils.settings

import com.vk.gradle.plugin.composable.skippability.checker.ComposableSkippabilityCheckerExtension
import com.vk.gradle.plugin.composable.skippability.checker.ComposableSkippabilityCheckerPlugin
import com.vk.gradle.plugin.compose.source.information.cleaner.SourceInformationCleanerPlugin
import com.vk.gradle.plugin.compose.test.tag.applier.ComposeTestTagApplierPlugin
import com.vk.gradle.plugin.compose.test.tag.cleaner.ComposeTestTagCleanerPlugin
import com.vk.gradle.plugin.compose.test.tag.drawer.ComposeTestTagDrawerPlugin
import com.vk.gradle.plugin.recompose.highlighter.RecomposeHighlighterPlugin
import com.vk.gradle.plugin.recompose.logger.RecomposeLoggerPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

// properties are sorted in applying order

@SettingsMarker
sealed class Setting {
    open val isEnabled: Boolean = false

    internal abstract fun setup(project: Project)
}

class RecomposeSetting : Setting() {
    override val isEnabled: Boolean = true

    var isHighlighterEnabled = false
    var isLoggerEnabled = false

    override fun setup(project: Project) {
        if (isHighlighterEnabled) project.apply<RecomposeHighlighterPlugin>()
        if (isLoggerEnabled) project.apply<RecomposeLoggerPlugin>()
    }
}

class TestTagSetting : Setting() {
    override var isEnabled: Boolean = true

    var isApplierEnabled = false
    var isCleanerEnabled = false
    var isDrawerEnabled = false

    override fun setup(project: Project) {
        if (isApplierEnabled) project.apply<ComposeTestTagApplierPlugin>()
        if (isCleanerEnabled) project.apply<ComposeTestTagCleanerPlugin>()
        if (isDrawerEnabled) project.apply<ComposeTestTagDrawerPlugin>()
    }
}

class SourceInfoCleanSetting : Setting() {
    override var isEnabled: Boolean = true

    override fun setup(project: Project) {
        if (isEnabled) project.apply<SourceInformationCleanerPlugin>()
    }
}

class SkippabilityChecksSetting : Setting() {
    override var isEnabled: Boolean = true

    var stabilityConfigurationPath: String? = null

    override fun setup(project: Project) {
        if (isEnabled) {
            project.apply<ComposableSkippabilityCheckerPlugin>()
            project.extensions.getByType<ComposableSkippabilityCheckerExtension>().apply {
                stabilityConfigurationPath = this@SkippabilityChecksSetting.stabilityConfigurationPath
            }
        }
    }
}