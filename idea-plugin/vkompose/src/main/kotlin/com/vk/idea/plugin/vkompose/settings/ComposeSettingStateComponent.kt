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

    var isFunctionSkippabilityCheckingEnabled: Boolean by state::isFunctionSkippabilityCheckingEnabled
    var stabilityChecksIgnoringClasses: String? by state::stabilityChecksIgnoringClasses
    var stabilityConfigurationPath: String? by state::stabilityConfigurationPath

    class State : BaseState() {
        var isFunctionSkippabilityCheckingEnabled by property(true)
        var stabilityChecksIgnoringClasses by string("")
        var stabilityConfigurationPath by string("")
    }

    companion object {
        fun getInstance(): ComposeSettingStateComponent = ApplicationManager.getApplication().getService(
            ComposeSettingStateComponent::class.java)
    }
}