package com.vk.idea.plugin.vkompose.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

internal class ComposeConfigurable : BoundSearchableConfigurable("VKompose", "preferences.vkompose") {

    private val settings = ComposeSettingStateComponent.getInstance()

    override fun createPanel(): DialogPanel =
        panel {
            row {
                checkBox("Show marker with possible test tag value")
                    .bindSelected(settings::isTestTagHintShowed)
            }
            row {
                checkBox("Check parameters stability of composable functions")
                    .bindSelected(settings::isFunctionSkippabilityCheckingEnabled)
            }
        }
}