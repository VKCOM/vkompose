package com.vk.idea.plugin.vkompose.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.selected

internal class ComposeConfigurable : BoundSearchableConfigurable("VKompose", "preferences.vkompose") {

    private val settings = ComposeSettingStateComponent.getInstance()

    override fun createPanel(): DialogPanel =
        panel {
            row {
                checkBox("Show marker with possible test tag value")
                    .bindSelected(settings::isTestTagHintShowed)
            }

            separator()

            lateinit var skippabilityChecks: Cell<JBCheckBox>
            row {
                skippabilityChecks = checkBox("Check parameters stability of composable functions")
                    .bindSelected(settings::isFunctionSkippabilityCheckingEnabled)
            }

            row {
                label("Ignored classes:")
            }.visibleIf(skippabilityChecks.selected)

            row {
                textArea()
                    .rows(10)
                    .align(AlignX.FILL)
                    .bindText(settings::stabilityChecksIgnoringClasses)
            }.visibleIf(skippabilityChecks.selected)
        }
}