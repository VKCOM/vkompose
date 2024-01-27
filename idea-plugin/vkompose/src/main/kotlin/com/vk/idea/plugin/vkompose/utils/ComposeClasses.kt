package com.vk.idea.plugin.vkompose.utils

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

const val COMPOSE_PACKAGE = "androidx.compose"
val COMPOSE_PACKAGE_NAME = Name.identifier("androidx.compose")
private const val ROOT = "$COMPOSE_PACKAGE.runtime"
private const val ROOT_INTERNAL = "$ROOT.internal"
private val ROOT_FQ_NAME = FqName(ROOT)
private val ROOT_INTERNAL_FQ_NAME = FqName(ROOT_INTERNAL)

internal object ComposeClassName {
    val Composable = FqName("$ROOT.Composable")
    val NonRestartableComposable = FqName("$ROOT.NonRestartableComposable")
    val NonSkippableComposable = FqName("$ROOT.NonSkippableComposable")
    val ExplicitGroupsComposable = FqName("$ROOT.ExplicitGroupsComposable")
    val Composer = FqName("$ROOT.Composer")
    val StableMarker = FqName("$ROOT.StableMarker")
    val Stable = FqName("$ROOT.Stable")
    val Immutable = FqName("$ROOT.Immutable")
    val StabilityInferred = FqName("$ROOT_INTERNAL.StabilityInferred")
}