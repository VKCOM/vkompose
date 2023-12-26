package com.vk.gradle.plugin.compose.utils

import com.vk.gradle.plugin.composable.skippability.checker.ComposableSkippabilityCheckerPlugin
import com.vk.gradle.plugin.compose.source.information.cleaner.SourceInformationCleanerPlugin
import com.vk.gradle.plugin.compose.test.tag.applier.ComposeTestTagApplierPlugin
import com.vk.gradle.plugin.compose.test.tag.cleaner.ComposeTestTagCleanerPlugin
import com.vk.gradle.plugin.compose.test.tag.drawer.ComposeTestTagDrawerPlugin
import com.vk.gradle.plugin.compose.utils.settings.RecomposeSetting
import com.vk.gradle.plugin.compose.utils.settings.SkippabilityChecksSetting
import com.vk.gradle.plugin.compose.utils.settings.SourceInfoCleanSetting
import com.vk.gradle.plugin.compose.utils.settings.TestTagSetting
import com.vk.gradle.plugin.recompose.highlighter.RecomposeHighlighterPlugin
import com.vk.gradle.plugin.recompose.logger.RecomposeLoggerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class VkomposePlugin : Plugin<Project> {

    override fun apply(target: Project) {

        // order matters
        target.apply<RecomposeHighlighterPlugin>()
        target.apply<RecomposeLoggerPlugin>()
        target.apply<ComposeTestTagApplierPlugin>()
        target.apply<ComposeTestTagCleanerPlugin>()
        target.apply<ComposeTestTagDrawerPlugin>()
        target.apply<SourceInformationCleanerPlugin>()
        target.apply<ComposableSkippabilityCheckerPlugin>()

        val extension = target.extensions.create("vkompose", VkomposeExtension::class.java)
        val settings = extension.settings

        // apply default settings
        settings[RecomposeSetting::class]?.setup(target)
        settings[TestTagSetting::class]?.setup(target)
        settings[SourceInfoCleanSetting::class]?.setup(target)
        settings[SkippabilityChecksSetting::class]?.setup(target)
    }
}