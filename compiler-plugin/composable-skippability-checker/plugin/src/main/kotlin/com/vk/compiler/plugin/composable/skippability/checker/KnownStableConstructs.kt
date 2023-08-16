package com.vk.compiler.plugin.composable.skippability.checker

import kotlin.coroutines.EmptyCoroutineContext

internal object KnownStableConstructs {

    val stableTypes = mapOf(
        Pair::class.qualifiedName!! to 0b11,
        Triple::class.qualifiedName!! to 0b111,
        Comparator::class.qualifiedName!! to 0,
        Result::class.qualifiedName!! to 0b1,
        ClosedRange::class.qualifiedName!! to 0b1,
        ClosedFloatingPointRange::class.qualifiedName!! to 0b1,
        // Guava
        "com.google.common.collect.ImmutableList" to 0b1,
        "com.google.common.collect.ImmutableEnumMap" to 0b11,
        "com.google.common.collect.ImmutableMap" to 0b11,
        "com.google.common.collect.ImmutableEnumSet" to 0b1,
        "com.google.common.collect.ImmutableSet" to 0b1,
        // Kotlinx immutable
        "kotlinx.collections.immutable.ImmutableCollection" to 0b1,
        "kotlinx.collections.immutable.ImmutableList" to 0b1,
        "kotlinx.collections.immutable.ImmutableSet" to 0b1,
        "kotlinx.collections.immutable.ImmutableMap" to 0b11,
        "kotlinx.collections.immutable.PersistentCollection" to 0b1,
        "kotlinx.collections.immutable.PersistentList" to 0b1,
        "kotlinx.collections.immutable.PersistentSet" to 0b1,
        "kotlinx.collections.immutable.PersistentMap" to 0b11,
        // Dagger
        "dagger.Lazy" to 0b1,
        // Coroutines
        EmptyCoroutineContext::class.qualifiedName!! to 0,
    )

    // TODO: buildList, buildMap, buildSet, etc.
    val stableFunctions = mapOf(
        "kotlin.collections.emptyList" to 0,
        "kotlin.collections.listOf" to 0b1,
        "kotlin.collections.listOfNotNull" to 0b1,
        "kotlin.collections.mapOf" to 0b11,
        "kotlin.collections.emptyMap" to 0,
        "kotlin.collections.setOf" to 0b1,
        "kotlin.collections.emptySet" to 0,
        "kotlin.to" to 0b11,
        // Kotlinx immutable
        "kotlinx.collections.immutable.immutableListOf" to 0b1,
        "kotlinx.collections.immutable.immutableSetOf" to 0b1,
        "kotlinx.collections.immutable.immutableMapOf" to 0b11,
        "kotlinx.collections.immutable.persistentListOf" to 0b1,
        "kotlinx.collections.immutable.persistentSetOf" to 0b1,
        "kotlinx.collections.immutable.persistentMapOf" to 0b11,
    )
}