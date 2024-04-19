package com.vk.gradle.plugin.recompose.logger

open class RecomposeLoggerExtension {
    open var isEnabled: Boolean = true
    open var logModifierChanges = true
    open var logFunctionChanges = false
}