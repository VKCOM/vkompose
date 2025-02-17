package com.vk.idea.plugin.vkompose.extensions

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleConstructorCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.KtAnnotationEntry

fun KtAnnotationEntry.getQualifiedName(): String? = analyze(this) {
  resolveToCall()?.singleConstructorCallOrNull()?.symbol?.containingClassId?.asFqNameString()
}

fun KtAnnotationEntry.fqNameMatches(fqName: String): Boolean {
  // For inspiration, see IDELightClassGenerationSupport.KtUltraLightSupportImpl.findAnnotation in the Kotlin plugin.
  val shortName = shortName?.asString() ?: return false
  return fqName.endsWith(shortName) && fqName == getQualifiedName()
}