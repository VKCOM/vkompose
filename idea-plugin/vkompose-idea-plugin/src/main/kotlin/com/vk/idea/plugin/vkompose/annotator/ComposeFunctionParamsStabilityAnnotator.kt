package com.vk.idea.plugin.vkompose.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import com.vk.idea.plugin.vkompose.COMPOSE_PACKAGE_NAME
import com.vk.idea.plugin.vkompose.ComposeClassName
import com.vk.idea.plugin.vkompose.extensions.getQualifiedName
import com.vk.idea.plugin.vkompose.extensions.type
import com.vk.idea.plugin.vkompose.hasAnnotation
import com.vk.idea.plugin.vkompose.hasComposableAnnotation
import com.vk.idea.plugin.vkompose.settings.ComposeSettingStateComponent
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.RemoveAnnotationFix
import org.jetbrains.kotlin.idea.highlighter.AnnotationHostKind
import org.jetbrains.kotlin.idea.quickfix.KotlinSuppressIntentionAction
import org.jetbrains.kotlin.idea.quickfix.RemoveArgumentFix
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ComposeFunctionParamsStabilityAnnotator : Annotator {

    private val stabilityInferencer = StabilityInferencer()

    private val settings = ComposeSettingStateComponent.getInstance()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!settings.isFunctionSkippabilityCheckingEnabled) return

        if (element !is KtNamedFunction || !element.hasComposableAnnotation() || !element.isRestartable()) return

        stabilityInferencer.setFunctionModule(element.findModuleDescriptor())
        val nonSkippableParams = mutableMapOf<KtParameter, KtStability>()
        val nonSkippableContextReceivers = mutableMapOf<KtContextReceiver, KtStability>()


        val extensionReceiverType = element.resolveToDescriptorIfAny()?.extensionReceiverParameter?.type
        var extensionReceiverStability: KtStability? = null
        if (extensionReceiverType?.fqName?.startsWith(COMPOSE_PACKAGE_NAME) == false && stabilityInferencer.ktStabilityOf(extensionReceiverType).knownUnstable()) {
            extensionReceiverStability = stabilityInferencer.ktStabilityOf(extensionReceiverType)
        }

        val parentClassType = element.findParentOfType<KtClass>()?.resolveToDescriptorIfAny()?.defaultType
        var dispatchReceiverStability: KtStability? = null
        if (parentClassType?.fqName?.startsWith(COMPOSE_PACKAGE_NAME) == false && stabilityInferencer.ktStabilityOf(parentClassType).knownUnstable()) {
            dispatchReceiverStability = stabilityInferencer.ktStabilityOf(parentClassType)
        }

        for (receiver in element.contextReceivers) {
            val type = receiver.type ?: continue
            val stability = stabilityInferencer.ktStabilityOf(type)
            val isUnstable = stability.knownUnstable()
            val isFromCompose = type.fqName?.startsWith(COMPOSE_PACKAGE_NAME) == true

            if (!isFromCompose && isUnstable) {
                nonSkippableContextReceivers += receiver to stability
            }
        }


        for (valueParameter in element.valueParameters) {
            val returnType = valueParameter.descriptor?.type ?: continue

            val isRequired = valueParameter.defaultValue == null
            val stability = stabilityInferencer.ktStabilityOf(returnType)
            val isUnstable = stability.knownUnstable()
            val isFromCompose = valueParameter.descriptor?.type?.fqName?.startsWith(COMPOSE_PACKAGE_NAME) == true

            if (!isFromCompose && isUnstable && isRequired) {
                nonSkippableParams += valueParameter to stability
            }
        }

        showMessages(element, holder, nonSkippableParams, nonSkippableContextReceivers, extensionReceiverStability, dispatchReceiverStability)

    }

    private fun showMessages(
        function: KtNamedFunction,
        holder: AnnotationHolder,
        nonSkippableParams: Map<KtParameter, KtStability>,
        nonSkippableContextReceivers: MutableMap<KtContextReceiver, KtStability>,
        extensionReceiverStability: KtStability? = null,
        dispatchReceiverStability: KtStability? = null,
    ) {

        val suppressAnnotation = function.annotationEntries.firstOrNull { annotation ->
            val isSuppress = annotation.getQualifiedName()?.contains(Keys.SUPPRESS) == true
            isSuppress && annotation.valueArgumentList?.arguments.orEmpty().any { it.getArgumentExpression()?.text?.contains(Keys.NON_SKIPPABLE_COMPOSABLE) == true }
        }


        val hasUnstableParams = nonSkippableParams.isNotEmpty()
                || nonSkippableContextReceivers.isNotEmpty()
                || extensionReceiverStability != null
                || dispatchReceiverStability != null

        val isUnstable = suppressAnnotation == null && hasUnstableParams
        val isNowStable = suppressAnnotation != null && !hasUnstableParams

        when {
            isUnstable -> {
                val kind = AnnotationHostKind("fun", function.name.orEmpty(), newLineNeeded = true)
                val intentionAction = KotlinSuppressIntentionAction(function, Keys.NON_SKIPPABLE_COMPOSABLE, kind)

                holder.newAnnotation(HighlightSeverity.ERROR, "Non skippable function")
                    .range(function.nameIdentifier?.originalElement ?: function.originalElement)
                    .withFix(intentionAction)
                    .create()

                nonSkippableParams.forEach { (param, stability) ->
                    holder.newAnnotation(HighlightSeverity.ERROR, "Parameter: $stability")
                        .range(param.originalElement)
                        .withFix(intentionAction)
                        .create()
                }

                nonSkippableContextReceivers.forEach { (receiver, stability) ->
                    holder.newAnnotation(HighlightSeverity.ERROR, "Context receiver: $stability")
                        .range(receiver.originalElement)
                        .withFix(intentionAction)
                        .create()
                }

                if (extensionReceiverStability != null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Extension receiver: $extensionReceiverStability")
                        .range(function.receiverTypeReference?.originalElement ?: function.originalElement)
                        .withFix(intentionAction)
                        .create()
                }

                if (dispatchReceiverStability != null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Wrapper class: $dispatchReceiverStability")
                        .range(function.nameIdentifier?.originalElement ?: function.originalElement)
                        .withFix(intentionAction)
                        .create()
                }
            }

            isNowStable -> {
                if (suppressAnnotation?.valueArguments?.size == 1) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Remove unused @Suppress")
                        .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                        .range(suppressAnnotation.originalElement)
                        .newFix(RemoveAnnotationFix("Remove unused @Suppress", suppressAnnotation))
                        .registerFix()
                        .create()
                    return
                }
                suppressAnnotation?.valueArgumentList?.arguments.orEmpty()
                    .filter { it.getArgumentExpression()?.text?.contains(Keys.NON_SKIPPABLE_COMPOSABLE) == true }
                    .forEach { annotationValue ->
                        holder.newAnnotation(
                            HighlightSeverity.WARNING,
                            "Remove unused ${Keys.NON_SKIPPABLE_COMPOSABLE}"
                        )
                            .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                            .range(annotationValue.getArgumentExpression()!!.originalElement)
                            .newFix(RemoveArgumentFix(annotationValue))
                            .registerFix()
                            .create()
                    }

            }
        }
    }

    private fun KtNamedFunction.isRestartable(): Boolean = when {
        isLocal -> false
        hasModifier(KtTokens.INLINE_KEYWORD) -> false
        hasAnnotation(ComposeClassName.NonRestartableComposable.asString()) -> false
        hasAnnotation(ComposeClassName.ExplicitGroupsComposable.asString()) -> false
        resolveToDescriptorIfAny()?.returnType?.isUnit() == false -> false
        else -> true
    }

}
