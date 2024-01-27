package com.vk.idea.plugin.vkompose.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage


@State(
    name = "com.vk.idea.plugin.vkompose.settings.ComposeSettingStateComponent",
    storages = [Storage("VkPluginExternalComposeSettings.xml")]
)
internal class ComposeSettingStateComponent :
    SimplePersistentStateComponent<ComposeSettingStateComponent.State>(State()) {

    var isTestTagHintShowed: Boolean
        set(value) {
            state.isTestTagHintShowed = value
        }
        get() = state.isTestTagHintShowed

    var isFunctionSkippabilityCheckingEnabled: Boolean
        set(value) {
            state.isFunctionSkippabilityCheckingEnabled = value
        }
        get() = state.isFunctionSkippabilityCheckingEnabled

    var stabilityChecksIgnoringClasses: String
        set(value) {
            state.stabilityChecksIgnoringClasses = value
        }
        get() = state.stabilityChecksIgnoringClasses.orEmpty()

    var stabilityConfigurationPath: String
        set(value) {
            state.stabilityConfigurationPath = value
        }
        get() = state.stabilityConfigurationPath.orEmpty()


    class State : BaseState() {

        var isTestTagHintShowed by property(true)
        var isFunctionSkippabilityCheckingEnabled by property(true)
        var stabilityChecksIgnoringClasses by string("")
        var stabilityConfigurationPath by string("")
    }

    companion object {
        fun getInstance(): ComposeSettingStateComponent = ApplicationManager.getApplication().getService(
            ComposeSettingStateComponent::class.java)
    }
}