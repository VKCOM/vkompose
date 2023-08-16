package com.vk.recompose.highlighter

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

object HighlighterModifier : Modifier by Modifier.applyHighlighter()

private fun Modifier.applyHighlighter(): Modifier = composed {
    if (RecomposeHighlighterConfig.isEnabled) then(recomposeHighlighter()) else this
}