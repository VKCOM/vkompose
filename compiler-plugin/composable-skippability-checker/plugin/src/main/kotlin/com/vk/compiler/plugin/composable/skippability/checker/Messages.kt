package com.vk.compiler.plugin.composable.skippability.checker

import com.vk.compiler.plugin.composable.skippability.checker.Keys.SUPPRESS
import com.vk.compiler.plugin.composable.skippability.checker.Keys.NON_SKIPPABLE_COMPOSABLE

object Messages {
    val SKIPPABILITY_FIX_EXPLANATION = """
                        These functions are not skippable because in them unstable parameters are used.
                        Please fix or add @$SUPPRESS("$NON_SKIPPABLE_COMPOSABLE")
                        How to fix:
                        1. Check parameters in list below. Maybe you fogot about @Stable or @Immutable annotation?
                        2. If you cannot make your parameter stable, try to declare default value (only if it is semantically required)
                        3. If your function is little(just using as proxy) and is not root function, try to add @NonRestartableComposable annotation (like LaunchedEffect, Image and etc).
                        4. If your function is root function and it doesn't read state, try to add @NonRestartableComposable annotation (like Surface and etc).
                           
                        P.S. For 3 and 4 - When you store <this> reference to class instance and use it to access some parameters, your function cannot be skippable.
                             If you cannot fix add "@$SUPPRESS("$NON_SKIPPABLE_COMPOSABLE")" to save restartable opportunity.
                             For more information read: https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md#functions-that-are-restartable-but-not-skippable
                    """.trimIndent()

    val REMOVE_SKIPPABILITY_ANNOTATION = "These functions are skippable. You should remove @$SUPPRESS(\"$NON_SKIPPABLE_COMPOSABLE\") for them:\n"
}