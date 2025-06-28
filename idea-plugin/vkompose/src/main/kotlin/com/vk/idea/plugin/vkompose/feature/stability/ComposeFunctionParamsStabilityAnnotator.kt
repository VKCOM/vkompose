package com.vk.idea.plugin.vkompose.feature.stability

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import com.vk.idea.plugin.vkompose.extensions.getQualifiedName
import com.vk.idea.plugin.vkompose.settings.ComposeSettingStateComponent
import com.vk.idea.plugin.vkompose.utils.COMPOSE_PACKAGE_NAME
import com.vk.idea.plugin.vkompose.utils.ComposeClassName.Composable
import com.vk.idea.plugin.vkompose.utils.ComposeClassName.ExplicitGroupsComposable
import com.vk.idea.plugin.vkompose.utils.ComposeClassName.NonRestartableComposable
import com.vk.idea.plugin.vkompose.utils.ComposeClassName.NonSkippableComposable
import com.vk.idea.plugin.vkompose.utils.hasAnnotation
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.inspections.RemoveAnnotationFix
import org.jetbrains.kotlin.idea.inspections.suppress.AnnotationHostKind
import org.jetbrains.kotlin.idea.inspections.suppress.KotlinSuppressIntentionAction
import org.jetbrains.kotlin.idea.quickfix.RemoveArgumentFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

class ComposeFunctionParamsStabilityAnnotator : Annotator {

    private val settings by ComposeSettingStateComponent.getInstance()

    private val isStrongSkippingEnabled = settings.isStrongSkippingEnabled
    private val isStrongSkippingFailFastEnabled = settings.isStrongSkippingFailFastEnabled

    private var ignoredClasses: List<Regex> = settings.stabilityChecksIgnoringClasses
        ?.split("\n")
        ?.map { it.trim().toRegex() }
        .orEmpty()

    private val stableTypeMatchers = try {
        StabilityConfigParser.fromFile(settings.stabilityConfigurationPath).stableTypeMatchers
    } catch (e: Exception) {
        emptySet()
    }

    private val stabilityInferencer = StabilityInferencer(stableTypeMatchers)

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!settings.isFunctionSkippabilityCheckingEnabled) return

        if (element !is KtNamedFunction
            || !element.hasAnnotation(Composable)
            || !element.isRestartable()
            || (element.hasAnnotation(NonSkippableComposable))) {
            return
        }

        analyze(element) {
            (element.symbol.containingModule as? KaSourceModule)?.let { stabilityInferencer.setFunctionModule(it) }
            val problemParams = mutableMapOf<KtParameter, KtStability>()
            val problemContextReceivers = mutableMapOf<KtContextReceiver, KtStability>()


            val extensionReceiverType = (element.symbol as? KaFunctionSymbol)?.receiverType
            var extensionReceiverStability: KtStability? = null
            val extensionFqName = extensionReceiverType?.symbol?.classId?.asSingleFqName()
            if (!extensionFqName.shouldBeIgnored() && extensionFqName?.startsWith(COMPOSE_PACKAGE_NAME) == false && stabilityInferencer.ktStabilityOf(extensionReceiverType).knownUnstable()) {
                extensionReceiverStability = stabilityInferencer.ktStabilityOf(extensionReceiverType)
            }

            val parentClassType = element.findParentOfType<KtClass>()?.let {
                analyze(it) { (it.symbol as? KaNamedClassSymbol)?.defaultType }
            }
            var dispatchReceiverStability: KtStability? = null
            val dispatchFqName = parentClassType?.symbol?.classId?.asSingleFqName()
            if (!dispatchFqName.shouldBeIgnored() && dispatchFqName?.startsWith(COMPOSE_PACKAGE_NAME) == false && stabilityInferencer.ktStabilityOf(parentClassType).knownUnstable()) {
                dispatchReceiverStability = stabilityInferencer.ktStabilityOf(parentClassType)
            }

            for (receiver in element.contextReceivers) {
                analyze(receiver) {
                    val type = receiver.expectedType.takeIf { it !is KaErrorType }
                    if (type != null) {
                        val fqName = type.symbol?.classId?.asSingleFqName()

                        val stability = stabilityInferencer.ktStabilityOf(type)
                        val isUnstable = stability.knownUnstable()
                        val isFromCompose = fqName?.startsWith(COMPOSE_PACKAGE_NAME) == true
                        val isIgnored = fqName.shouldBeIgnored()

                        if (!isIgnored && !isFromCompose && isUnstable) {
                            problemContextReceivers += receiver to stability
                        }
                    }
                }
            }

            for (valueParameter in element.valueParameters) {
                analyze(valueParameter) {
                    val returnType = valueParameter.returnType.takeIf { it !is KaErrorType }
                    if (returnType != null) {
                        val fqName = valueParameter.returnType.symbol?.classId?.asSingleFqName()

                        val isRequired = valueParameter.defaultValue == null
                        val stability = stabilityInferencer.ktStabilityOf(returnType)
                        val isUnstable = stability.knownUnstable()
                        val isFromCompose = fqName?.startsWith(COMPOSE_PACKAGE_NAME) == true
                        val isIgnored = fqName.shouldBeIgnored()

                        if (!isIgnored && !isFromCompose && isUnstable && (isRequired || isStrongSkippingEnabled)) {
                            problemParams += valueParameter to stability
                        }
                    }
                }
            }

            showMessages(element, holder, problemParams, problemContextReceivers, extensionReceiverStability, dispatchReceiverStability)
        }


    }

    private fun showMessages(
        function: KtNamedFunction,
        holder: AnnotationHolder,
        problemParams: Map<KtParameter, KtStability>,
        problemContextReceivers: MutableMap<KtContextReceiver, KtStability>,
        extensionReceiverStability: KtStability? = null,
        dispatchReceiverStability: KtStability? = null,
    ) {

        val severity = when {
            isStrongSkippingEnabled && isStrongSkippingFailFastEnabled -> HighlightSeverity.ERROR
            isStrongSkippingEnabled -> HighlightSeverity.WARNING
            else -> HighlightSeverity.ERROR
        }
        val aboutFunctionMessage = if (isStrongSkippingEnabled) "Functions with ref compared paramaters" else "Non skippable function"

        val suppressAnnotation = function.annotationEntries
            .filter { it.getQualifiedName()?.contains(SUPPRESS) == true }
            .firstOrNull {
                it.valueArgumentList?.arguments.orEmpty().any { it.getArgumentExpression()?.text?.let { text -> SUPPRESS_NAMES.any { text.contains(it) } } == true }
            }

        val hasUnstableParams = problemParams.isNotEmpty()
                || problemContextReceivers.isNotEmpty()
                || extensionReceiverStability != null
                || dispatchReceiverStability != null

        val usedSuppressNames = suppressAnnotation?.valueArgumentList?.arguments.orEmpty()
            .mapNotNull { it.getArgumentExpression()?.text }
            .flatMap { text -> SUPPRESS_NAMES.filter { text.contains(it) } }

        val isUnstable = suppressAnnotation == null && hasUnstableParams
        val isNowStable = usedSuppressNames.isNotEmpty() && !hasUnstableParams

        val newSuppressName = if (isStrongSkippingEnabled) PARAMS_COMPARED_BY_REF else NON_SKIPPABLE_COMPOSABLE
        when {
            isUnstable -> {
                val kind = AnnotationHostKind("fun", function.name.orEmpty(), newLineNeeded = true)
                val intentionAction = KotlinSuppressIntentionAction(function, newSuppressName, kind)

                holder.newAnnotation(severity, aboutFunctionMessage)
                    .range(function.nameIdentifier?.originalElement ?: function.originalElement)
                    .withFix(intentionAction)
                    .create()

                problemParams.forEach { (param, stability) ->
                    holder.newAnnotation(severity, "Parameter: $stability")
                        .range(param.originalElement)
                        .withFix(intentionAction)
                        .create()
                }

                problemContextReceivers.forEach { (receiver, stability) ->
                    holder.newAnnotation(severity, "Context receiver: $stability")
                        .range(receiver.originalElement)
                        .withFix(intentionAction)
                        .create()
                }

                if (extensionReceiverStability != null) {
                    holder.newAnnotation(severity, "Extension receiver: $extensionReceiverStability")
                        .range(function.receiverTypeReference?.originalElement ?: function.originalElement)
                        .withFix(intentionAction)
                        .create()
                }

                if (dispatchReceiverStability != null) {
                    holder.newAnnotation(severity, "Wrapper class: $dispatchReceiverStability")
                        .range(function.nameIdentifier?.originalElement ?: function.originalElement)
                        .withFix(intentionAction)
                        .create()
                }
            }

            isNowStable -> {
                holder.makeSuppressUnused(suppressAnnotation, newSuppressName)
            }
        }

        if (isStrongSkippingEnabled && usedSuppressNames.contains(NON_SKIPPABLE_COMPOSABLE)) {
            holder.makeSuppressUnused(suppressAnnotation, NON_SKIPPABLE_COMPOSABLE)
        }
    }

    private fun AnnotationHolder.makeSuppressUnused(
        suppressAnnotation: KtAnnotationEntry?,
        newSuppressName: String
    ) {
        if (suppressAnnotation?.valueArguments?.size == 1) {
            newAnnotation(HighlightSeverity.WARNING, "Remove unused @Suppress")
                .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                .range(suppressAnnotation.originalElement)
                .newFix(RemoveAnnotationFix("Remove unused @Suppress", suppressAnnotation))
                .registerFix()
                .create()
            return
        }
        suppressAnnotation?.valueArgumentList?.arguments.orEmpty()
            .filter { it.getArgumentExpression()?.text?.contains(newSuppressName) == true }
            .forEach { annotationValue ->
                newAnnotation(
                    HighlightSeverity.WARNING,
                    "Remove unused $newSuppressName"
                )
                    .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                    .range(annotationValue.getArgumentExpression()!!.originalElement)
                    .newFix(RemoveArgumentFix(annotationValue))
                    .registerFix()
                    .create()
            }
    }

    private fun FqName?.shouldBeIgnored() = ignoredClasses.any { this?.asString()?.matches(it) == true }

    private fun KtNamedFunction.isRestartable(): Boolean = when {
        isLocal -> false
        hasModifier(KtTokens.INLINE_KEYWORD) -> false
        hasAnnotation(NonRestartableComposable) -> false
        hasAnnotation(ExplicitGroupsComposable) -> false
        analyze(this) { !returnType.isUnitType } -> false
        else -> true
    }

    companion object {
        const val SUPPRESS = "Suppress"
        const val NON_SKIPPABLE_COMPOSABLE = "NonSkippableComposable"
        const val PARAMS_COMPARED_BY_REF = "ParamsComparedByRef"
        internal val SUPPRESS_NAMES = setOf(PARAMS_COMPARED_BY_REF, NON_SKIPPABLE_COMPOSABLE)
    }
}
