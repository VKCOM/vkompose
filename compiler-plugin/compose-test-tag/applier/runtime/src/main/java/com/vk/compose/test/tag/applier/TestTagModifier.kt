package com.vk.compose.test.tag.applier

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId

fun Modifier.applyTestTag(tag: String): Modifier = then(TestTagElement(tag))

private data class TestTagElement(
    val tag: String,
) : ModifierNodeElement<TestTagNode>() {
    override fun create() = TestTagNode(tag)

    override fun update(node: TestTagNode) {
        node.tag = tag
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "composableTestTag"
        properties["tag"] = tag
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestTagElement) return false

        return tag == other.tag
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class TestTagNode(
    var tag: String,
) : Modifier.Node(), SemanticsModifierNode {

    override fun SemanticsPropertyReceiver.applySemantics() {
        testTag = tag
        testTagsAsResourceId = true
    }
}