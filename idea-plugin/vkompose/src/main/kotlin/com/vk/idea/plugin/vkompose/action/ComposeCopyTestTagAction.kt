package com.vk.idea.plugin.vkompose.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

internal class ComposeCopyTestTagAction(tag: String) : AnAction() {

    private val transferableText = StringSelection(tag)
    private val copyPasteManager = CopyPasteManager.getInstance()

    override fun actionPerformed(e: AnActionEvent) {
        copyPasteManager.setContents(transferableText)
    }
}