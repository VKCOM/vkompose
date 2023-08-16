package com.vk.rules.compose

import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtParameter

sealed interface SkippabilityResult {
    data object None : SkippabilityResult
    data class Unstable(
        val unstableParams: Map<KtParameter, KtStability>,
        val unstableContextReceiver: Map<KtContextReceiver, KtStability>,
        val extensionReceiverStability: KtStability? = null,
        val dispatchReceiverStability: KtStability? = null,
    ) : SkippabilityResult
}