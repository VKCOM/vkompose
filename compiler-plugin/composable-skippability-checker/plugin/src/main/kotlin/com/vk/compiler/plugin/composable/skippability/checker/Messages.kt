package com.vk.compiler.plugin.composable.skippability.checker

import com.vk.compiler.plugin.composable.skippability.checker.Keys.SUPPRESS
import com.vk.compiler.plugin.composable.skippability.checker.Keys.NON_SKIPPABLE_COMPOSABLE
import com.vk.compiler.plugin.composable.skippability.checker.Keys.PARAMS_COMPARED_BY_REF

object Messages {
    val SKIPPABILITY_FIX_EXPLANATION = """
                        These functions are not skippable because in them unstable parameters are used.
                        Please fix or add annotation @$SUPPRESS("$NON_SKIPPABLE_COMPOSABLE") or @NonSkippableComposable (Compose Runtime 1.6.0)
                        How to fix:
                        1. Check parameters in list below. Maybe you fogot about @Stable or @Immutable annotation?
                        2. If you cannot make your parameter stable, try to declare default value (only if it is semantically required)
                        3. If your function is little(just using as proxy) and is not root function, try to add @NonRestartableComposable annotation (like LaunchedEffect, Image and etc).
                        4. If your function is root function and it doesn't read state, try to add @NonRestartableComposable annotation (like Surface and etc).
                        5. Enable strong skipping mode (Compose Compiler 1.6.0) https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/strong-skipping.md
                           
                        P.S. For 3 and 4 - When you store <this> reference to class instance and use it to access some parameters, your function cannot be skippable.
                             If you cannot fix add "@$SUPPRESS("$NON_SKIPPABLE_COMPOSABLE")" to save restartable opportunity.
                             For more information read: https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/compiler-metrics.md#functions-that-are-restartable-but-not-skippable
                    """.trimIndent()


    val REF_COMPARISON_FIX_EXPLANATION = """
                        These functions have paramaters that will be compared by ref.
                        Please fix or add annotation @$SUPPRESS("$PARAMS_COMPARED_BY_REF") or @NonSkippableComposable (Compose Runtime 1.6.0)
                        For fix check parameters in list below. Maybe you fogot about @Stable or @Immutable annotation?
                        
                        Details about Strong Skipping Mode: https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/strong-skipping.md
                    """.trimIndent()

    val REMOVE_SKIPPABILITY_ANNOTATION =
        "These functions are skippable. You should remove @$SUPPRESS(\"$NON_SKIPPABLE_COMPOSABLE\") for them:\n"
}