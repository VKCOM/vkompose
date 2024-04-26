package com.vk.idea.plugin.vkompose.utils

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

internal object Icons {

    val ComposeTestTag = loadIcon("/icons/compose_test_tag_icon.svg")

    private fun loadIcon(res: String): Icon = IconLoader.getIcon(res, Icons::class.java)
}