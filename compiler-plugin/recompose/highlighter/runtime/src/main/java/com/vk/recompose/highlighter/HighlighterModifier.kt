package com.vk.recompose.highlighter

import androidx.compose.ui.Modifier

fun Modifier.applyHighlighter(): Modifier {
    return if (RecomposeHighlighterConfig.isEnabled) then(Modifier.recomposeHighlighter()) else this
}