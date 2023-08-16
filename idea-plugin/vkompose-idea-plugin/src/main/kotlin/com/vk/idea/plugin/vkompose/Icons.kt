package com.vk.idea.plugin.vkompose

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object Icons {

    val ComposeTestTag = loadIcon("/icons/compose_test_tag_icon.svg")

    fun loadIcon(res: String): Icon {
        return IconLoader.getIcon(res, Icons::class.java)
    }
}