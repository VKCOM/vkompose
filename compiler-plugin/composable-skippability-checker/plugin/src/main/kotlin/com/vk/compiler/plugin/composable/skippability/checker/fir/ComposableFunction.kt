package com.vk.compiler.plugin.composable.skippability.checker.fir

import com.vk.compiler.plugin.composable.skippability.checker.ComposeClassId
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object ComposableFunction : FunctionTypeKind(
    FqName.topLevel(Name.identifier("androidx.compose.runtime.internal")),
    "ComposableFunction",
    ComposeClassId.Composable,
    isReflectType = false
) {
    override val prefixForTypeRender: String
        get() = "@Composable"

    override fun reflectKind(): FunctionTypeKind = KComposableFunction
}

object KComposableFunction : FunctionTypeKind(
    FqName.topLevel(Name.identifier("androidx.compose.runtime.internal")),
    "KComposableFunction",
    ComposeClassId.Composable,
    isReflectType = true
) {
    override fun nonReflectKind(): FunctionTypeKind = ComposableFunction
}