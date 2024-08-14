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

class StrongSkippingSetting : Setting() {
    override var isEnabled: Boolean = true

    @JvmField
    var strongSkippingFailFastEnabled = false

    override fun setup(project: Project) {
        project.extensions.getByType<ComposableSkippabilityCheckerExtension>().apply {
            strongSkippingEnabled = this@StrongSkippingSetting.isEnabled
            strongSkippingFailFastEnabled = this@StrongSkippingSetting.strongSkippingFailFastEnabled
        }
    }
}

class RecomposeLoggerSetting : Setting() {
    override var isEnabled: Boolean = true

    @JvmField
    var logModifierChanges = true
    @JvmField
    var logFunctionChanges = false

    override fun setup(project: Project) {
        project.extensions.getByType<RecomposeLoggerExtension>().isEnabled = isEnabled
        project.extensions.getByType<RecomposeLoggerExtension>().logModifierChanges = logModifierChanges
        project.extensions.getByType<RecomposeLoggerExtension>().logFunctionChanges = logFunctionChanges
    }
}

class RecomposeSetting : Setting() {
    override val isEnabled: Boolean = true

    private var loggerSetting = RecomposeLoggerSetting()
    @JvmField
    var isHighlighterEnabled = false

    @JvmField
    var isLoggerEnabled = false

    override fun setup(project: Project) {
        project.extensions.getByType<RecomposeHighlighterExtension>().isEnabled = isHighlighterEnabled
        if (isLoggerEnabled) {
            loggerSetting.setup(project)
        }
    }

    fun Project.logger(configure: RecomposeLoggerSetting.() -> Unit) {
        loggerSetting.apply(configure)
        loggerSetting.setup(this)
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

    private val strongSkippingSetting = StrongSkippingSetting()

    @JvmField
    var stabilityConfigurationPath: String? = null

    @JvmField
    var strongSkippingEnabled = false

    override fun setup(project: Project) {
        project.extensions.getByType<ComposableSkippabilityCheckerExtension>().apply {
            stabilityConfigurationPath = this@SkippabilityChecksSetting.stabilityConfigurationPath
            isEnabled = this@SkippabilityChecksSetting.isEnabled
        }
        if (strongSkippingEnabled) {
            strongSkippingSetting.setup(project)
        }
    }

    fun Project.strongSkipping(configure: StrongSkippingSetting.() -> Unit) {
        strongSkippingSetting.apply(configure)
        strongSkippingSetting.setup(this)
    }
}