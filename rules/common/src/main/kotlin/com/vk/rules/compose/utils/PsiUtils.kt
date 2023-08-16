package com.vk.rules.compose.utils

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFile

inline fun <reified T : PsiElement> PsiElement.findParentOfType(strict: Boolean = true): T? {
    return findParentInFile(!strict) { it is T } as? T
}

inline fun PsiElement.findParentInFile(
    withSelf: Boolean = false,
    predicate: (PsiElement) -> Boolean
): PsiElement? {
    var current = when {
        withSelf -> this
        this is PsiFile -> return null
        else -> parent
    }

    while (current != null) {
        if (predicate(current)) return current
        if (current is PsiFile) break
        current = current.parent
    }
    return null
}