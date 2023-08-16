package com.vk.idea.plugin.vkompose.extensions

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


fun KtAnnotationEntry.getQualifiedName(): String? {
  return analyze(BodyResolveMode.PARTIAL).get(BindingContext.ANNOTATION, this)?.fqName?.asString()
}


fun KtAnnotationEntry.fqNameMatches(fqName: String): Boolean {
  // For inspiration, see IDELightClassGenerationSupport.KtUltraLightSupportImpl.findAnnotation in the Kotlin plugin.
  val shortName = shortName?.asString() ?: return false
  return fqName.endsWith(shortName) && fqName == getQualifiedName()
}