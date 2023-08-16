package com.vk.compose.test.tag.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.random.Random

@OptIn(ExperimentalTextApi::class)
@Stable
fun Modifier.drawTestTag(): Modifier = composed(debugInspectorInfo {
    name = "testTagDrawer"
}) {
    if (!TestTagDrawConfig.isEnabled) return@composed Modifier

    val actual = this@drawTestTag.searchInspectableValue {
        it.semanticsConfiguration.getOrNull(SemanticsProperties.TestTag).orEmpty().isNotEmpty()
    }
    val currentTag = actual?.semanticsConfiguration?.getOrNull(SemanticsProperties.TestTag)
        ?: return@composed Modifier

    val text = remember(currentTag) {
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Red)) {
                append("testTag:")
            }
            withStyle(style = SpanStyle(color = Color.Green)) {
                append(currentTag)
            }
        }
    }

    val randomStrokeColor = remember { Color(Random.nextInt()) }
    var showTag by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val borderWidth = 4.dp
    val borderWidthPx = with(density) { borderWidth.toPx() }
    val positionProvider = remember(density) { DropdownMenuPositionProvider(DpOffset.Zero, density) }

    if (showTag) {
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { showTag = false }
        ) {
            BasicText(
                text = text,
                modifier = Modifier
                    .background(Color.Black)
                    .border(borderWidth, randomStrokeColor)
            )
        }
    }

    Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                if (!showTag) {
                    try {
                        withTimeout(1000) {
                            waitForUpOrCancellation()
                        }
                    } catch (e: PointerEventTimeoutCancellationException) {
                        showTag = !showTag
                        do {
                            val event = awaitPointerEvent()
                            event.changes.fastForEach { it.consume() }
                        } while (event.changes.fastAny { it.pressed })
                    }
                }
            }
        }
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                if (showTag) {
                    drawRect(randomStrokeColor, style = Stroke(borderWidthPx))
                }
            }
        }

}


private fun Modifier.searchInspectableValue(predicate: (SemanticsModifier) -> Boolean): SemanticsModifier? {
    return when (this) {
        is SemanticsModifier -> this.takeIf(predicate)
        is CombinedModifier -> {
            val (outerValue, innerValue) = splitCombinedModifier()
            val outerInspectableValue = outerValue?.searchInspectableValue(predicate)
            val innerInspectableValue = innerValue?.searchInspectableValue(predicate)
            innerInspectableValue ?: outerInspectableValue
        }

        else -> null
    }
}

private fun CombinedModifier.splitCombinedModifier(): Pair<Modifier?, Modifier?> {
    val outer = CombinedModifier::class.java.getDeclaredField("outer")
    val inner = CombinedModifier::class.java.getDeclaredField("inner")
    outer.isAccessible = true
    inner.isAccessible = true
    val outerValue = outer.get(this) as? Modifier
    val innerValue = inner.get(this) as? Modifier
    return Pair(outerValue, innerValue)
}

// copy past from VkDropdown. i do not want to add dependency of material for this
@Immutable
private data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { 14.dp.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(
                toRight,
                toLeft,
                // If the anchor gets outside of the window on the left, we want to position
                // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
            )
        } else {
            sequenceOf(
                toLeft,
                toRight,
                // If the anchor gets outside of the window on the right, we want to position
                // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
            )
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
            it >= verticalMargin &&
                    it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}