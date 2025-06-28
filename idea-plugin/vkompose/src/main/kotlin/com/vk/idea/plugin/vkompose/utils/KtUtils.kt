package com.vk.idea.plugin.vkompose.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.vk.idea.plugin.vkompose.extensions.fqNameMatches
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

internal fun KtNamedFunction.hasAnnotation(fqName: FqName): Boolean = hasAnnotation(fqName.asString())

internal fun KtNamedFunction.hasAnnotation(fqString: String): Boolean =
    annotationEntries.any { annotation -> annotation.fqNameMatches(fqString) }

internal fun LeafPsiElement.isInFileHeader(): Boolean {
    return haveParentOfType<KtFileAnnotationList>() ||
            haveParentOfType<KtPackageDirective>() ||
            haveParentOfType<KtImportList>()
}

internal inline fun <reified T> Any.safeCast(): T? = this as? T

private inline fun <reified T : PsiElement> PsiElement.haveParentOfType(strict: Boolean = false): Boolean {
    return getParentOfType<T>(strict) != null
}

internal fun KaClassLikeSymbol?.isInlineClass(): Boolean = this is KaNamedClassSymbol && this.isInline

