package com.vk.gradle.plugin.composable.skippability.checker

open class ComposableSkippabilityCheckerExtension {
    open var isEnabled: Boolean = true
    open var stabilityConfigurationPath: String? = null
}