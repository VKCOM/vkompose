package com.vk.rules.compose

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

const val COMPOSE_PACKAGE = "androidx.compose"
internal val COMPOSE_PACKAGE_NAME = Name.identifier("androidx.compose")
private const val ROOT = "$COMPOSE_PACKAGE.runtime"

internal object ComposeClassName {
    val StableMarker = FqName("$ROOT.StableMarker")
}

internal object ComposeClassString {
    const val NonRestartableComposable = "NonRestartableComposable"
    const val ParamsComparedByRef = "ParamsComparedByRef"
    const val NonSkippableComposable = "NonSkippableComposable"
    const val ExplicitGroupsComposable = "ExplicitGroupsComposable"
    const val Composable = "Composable"
}
