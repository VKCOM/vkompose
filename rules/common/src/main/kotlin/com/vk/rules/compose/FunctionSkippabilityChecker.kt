package com.vk.rules.compose

import com.vk.rules.compose.utils.findParentOfType
import com.vk.rules.compose.utils.hasAnnotation
import com.vk.rules.compose.utils.resolveDescriptor
import com.vk.rules.compose.utils.resolveToDescriptorIfAny
import com.vk.rules.compose.utils.resolveType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isUnit

class FunctionSkippabilityChecker {

    private val stabilityInferencer = StabilityInferencer()

    fun analyze(
        function: KtNamedFunction,
        bindingContext: BindingContext,
        ignoredClasses: List<Regex>
    ) : SkippabilityResult {

        if (!function.hasAnnotation(ComposeClassString.Composable)
            || !function.isRestartable(bindingContext)
            || function.hasAnnotation(ComposeClassString.NonSkippableComposable)
        ) {
            return SkippabilityResult.None
        }

        val functionDescriptor = function.resolveToDescriptorIfAny(bindingContext)
        functionDescriptor?.module?.let(stabilityInferencer::setFunctionModule)
        stabilityInferencer.setBindingContext(bindingContext)
        val nonSkippableParams = mutableMapOf<KtParameter, KtStability>()
        val nonSkippableContextReceivers = mutableMapOf<KtContextReceiver, KtStability>()


        val extensionReceiverType = functionDescriptor?.extensionReceiverParameter?.type
        var extensionReceiverStability: KtStability? = null
        val extensionFqName = extensionReceiverType?.fqName
        if (!extensionFqName.shouldBeIgnored(ignoredClasses) && extensionFqName?.startsWith(COMPOSE_PACKAGE_NAME) == false && stabilityInferencer.ktStabilityOf(extensionReceiverType).knownUnstable()) {
            extensionReceiverStability = stabilityInferencer.ktStabilityOf(extensionReceiverType)
        }

        val parentClassType = function.findParentOfType<KtClass>()?.resolveToDescriptorIfAny(bindingContext)?.defaultType
        var dispatchReceiverStability: KtStability? = null
        val dispatchFqName = parentClassType?.fqName
        if (!dispatchFqName.shouldBeIgnored(ignoredClasses) && dispatchFqName?.startsWith(COMPOSE_PACKAGE_NAME) == false && stabilityInferencer.ktStabilityOf(parentClassType).knownUnstable()) {
            dispatchReceiverStability = stabilityInferencer.ktStabilityOf(parentClassType)
        }

        for (receiver in function.contextReceivers) {
            val type = receiver.resolveType(bindingContext) ?: continue
            val stability = stabilityInferencer.ktStabilityOf(type)
            val isUnstable = stability.knownUnstable()
            val fqName = type.fqName
            val isFromCompose = fqName?.startsWith(COMPOSE_PACKAGE_NAME) == true
            val isIgnored = fqName.shouldBeIgnored(ignoredClasses)

            if (!isIgnored && !isFromCompose && isUnstable) {
                nonSkippableContextReceivers += receiver to stability
            }
        }


        for (valueParameter in function.valueParameters) {
            val returnType = valueParameter.resolveDescriptor(bindingContext)?.type ?: continue

            val isRequired = valueParameter.defaultValue == null
            val stability = stabilityInferencer.ktStabilityOf(returnType)
            val isUnstable = stability.knownUnstable()
            val fqName = valueParameter.resolveDescriptor(bindingContext)?.type?.fqName
            val isFromCompose = fqName?.startsWith(COMPOSE_PACKAGE_NAME) == true
            val isIgnored = fqName.shouldBeIgnored(ignoredClasses)

            if (!isIgnored && !isFromCompose && isUnstable && isRequired) {
                nonSkippableParams += valueParameter to stability
            }
        }

        val isUnstable = nonSkippableParams.isNotEmpty()
                || nonSkippableContextReceivers.isNotEmpty()
                || extensionReceiverStability != null
                || dispatchReceiverStability != null


        if (isUnstable) {
            return SkippabilityResult.Unstable(
                nonSkippableParams,
                nonSkippableContextReceivers,
                extensionReceiverStability,
                dispatchReceiverStability
            )
        }

        return SkippabilityResult.None
    }

    private fun FqName?.shouldBeIgnored(ignoredClasses: List<Regex>, ) = ignoredClasses.any { this?.asString()?.matches(it) == true }

    private fun KtNamedFunction.isRestartable(bindingContext: BindingContext): Boolean = when {
        isLocal -> false
        hasModifier(KtTokens.INLINE_KEYWORD) -> false
        hasAnnotation(ComposeClassString.NonRestartableComposable) -> false
        hasAnnotation(ComposeClassString.ExplicitGroupsComposable) -> false
        resolveToDescriptorIfAny(bindingContext)?.returnType?.isUnit() == false -> false
        else -> true
    }

}
