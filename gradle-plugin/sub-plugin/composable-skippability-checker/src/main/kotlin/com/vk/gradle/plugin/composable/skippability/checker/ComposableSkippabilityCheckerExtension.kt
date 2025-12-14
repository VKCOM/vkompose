package com.vk.gradle.plugin.composable.skippability.checker

open class ComposableSkippabilityCheckerExtension {
    open var isEnabled: Boolean = true
    open var isFirEnabled: Boolean = false
    open var stabilityConfigurationPath: String? = null
    open var strongSkippingEnabled: Boolean = false
    open var strongSkippingFailFastEnabled: Boolean = false
}