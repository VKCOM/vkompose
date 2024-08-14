package com.vk.idea.plugin.vkompose.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
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

internal class ComposeConfigurable : BoundSearchableConfigurable("VKompose", "preferences.vkompose") {

    private val settings by ComposeSettingStateComponent.getInstance()

    override fun createPanel(): DialogPanel = panel {
        lateinit var skippabilityChecks: Cell<JBCheckBox>
        row {
            skippabilityChecks = checkBox("Check parameters stability of composable functions")
                .bindSelected(settings::isFunctionSkippabilityCheckingEnabled)
        }

        row {
            visibleIf(skippabilityChecks.selected)
            val strongSkipping = checkBox("Strong skipping mode")
                .bindSelected(settings::isStrongSkippingEnabled)
            checkBox("Report strong skipping problems as error")
                .bindSelected(settings::isStrongSkippingFailFastEnabled)
                .enabledIf(strongSkipping.selected)
        }

        row {
            visibleIf(skippabilityChecks.selected)
            label("Stability configuration path:")
        }

        row {
            visibleIf(skippabilityChecks.selected)
            textFieldWithBrowseButton(
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(),
                fileChosen = { newFile -> newFile.path }
            ).align(AlignX.FILL)
                .bindText(
                    getter = { settings.stabilityConfigurationPath.orEmpty() },
                    setter = { settings.stabilityConfigurationPath = it },
                )
        }

        row {
            visibleIf(skippabilityChecks.selected)
            label("Additional ignored classes:")
        }

        row {
            visibleIf(skippabilityChecks.selected)
            textArea()
                .rows(5)
                .align(AlignX.FILL)
                .bindText(
                    getter = { settings.stabilityChecksIgnoringClasses.orEmpty() },
                    setter = { settings.stabilityChecksIgnoringClasses = it },
                )
        }
    }
}