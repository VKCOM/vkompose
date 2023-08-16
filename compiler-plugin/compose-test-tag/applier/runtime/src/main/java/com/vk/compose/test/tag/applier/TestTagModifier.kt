package com.vk.compose.test.tag.applier

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.applyTestTag(tag: String): Modifier = inspectable(debugInspectorInfo {
    name = "composableTestTag"
    properties["tag"] = tag
}) {
    Modifier.semantics {
        testTag = tag
        // иначе compose мержит сверху вниз, чтобы функции выше не знали какие тэги у функции ниже
        // делаем доступным все тэги на любом уровне иерархии
        testTagsAsResourceId = true
    }
}