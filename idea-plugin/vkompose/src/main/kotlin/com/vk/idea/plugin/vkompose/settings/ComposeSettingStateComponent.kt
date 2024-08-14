package com.vk.idea.plugin.vkompose.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.application
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@State(
    name = "com.vk.idea.plugin.vkompose.settings.ComposeSettingStateComponent",
    storages = [Storage("VkPluginExternalComposeSettings.xml")]
)
internal class ComposeSettingStateComponent :
    SimplePersistentStateComponent<ComposeSettingStateComponent.State>(State()) {

    class State : BaseState() {
        var isFunctionSkippabilityCheckingEnabled by property(true)
        var isStrongSkippingEnabled by property(false)
        var isStrongSkippingFailFastEnabled by property(false)
        var stabilityChecksIgnoringClasses by string("")
        var stabilityConfigurationPath by string("")
    }

    operator fun provideDelegate(thisRef: Any, property: KProperty<*>): ReadOnlyProperty<Any, State> {
        return ReadOnlyProperty { _, _ -> state }
    }

    companion object {
        fun getInstance(): ComposeSettingStateComponent = application.service()
    }
}