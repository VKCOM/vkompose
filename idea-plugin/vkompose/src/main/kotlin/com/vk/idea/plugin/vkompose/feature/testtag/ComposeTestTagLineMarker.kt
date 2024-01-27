package com.vk.idea.plugin.vkompose.feature.testtag

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.vk.idea.plugin.vkompose.utils.ComposeClassName.Composable
import com.vk.idea.plugin.vkompose.settings.ComposeSettingStateComponent
import com.vk.idea.plugin.vkompose.utils.Icons
import com.vk.idea.plugin.vkompose.utils.hasAnnotation
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch


class ComposeTestTagLineMarker : LineMarkerProvider {

    private val settings = ComposeSettingStateComponent.getInstance()
    private val testTagRegex = "applyTestTag|testTag".toRegex()

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!settings.isTestTagHintShowed || element !is KtCallExpression) return null

        val declaration = element.searchDeclaredFunction()
        if (declaration?.hasAnnotation(Composable) == true
            && declaration.hasComposeModifier()
            && element.containsObjectModifierWithoutTag()
        ) {
            return TestTagLineMarkerInfo(element, createTag(element, declaration))
        }

        return null
    }

    private fun createTag(
        call: KtCallExpression,
        dec: KtNamedFunction
    ): String =
        "${call.containingKtFile.name}-" + with(call.getParentOfExpression()) { ("${this?.name}(${this?.startOffsetToFunKeyword()})-") } + "${dec.name}(${call.startOffset})"

    private fun KtCallExpression.getParentOfExpression(): KtNamedFunction? {
        val funcParent = getParentOfType<KtNamedFunction>(false) ?: return null
        val lambdaPropertyParent =
            getParentOfType<KtProperty>(false)?.takeIf { it.isComposableLambda() } ?: return funcParent

        // call expression in lambda. but ir represents lambda in different way, therefore we should don't use names of function and lambda
        return if (lambdaPropertyParent.startOffset >= funcParent.startOffset) null else funcParent
    }

    private fun KtCallExpression.searchDeclaredFunction(): KtNamedFunction? =
        calleeExpression?.references?.firstOrNull()?.resolve() as? KtNamedFunction

    private fun KtDeclaration.isComposableLambda(): Boolean {
        val type = (resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType
        return type?.isFunctionType == true && type.annotations.any { annotation -> annotation.fqName == Composable }
    }

    private fun KtNamedFunction.hasComposeModifier(): Boolean =
        valueParameters.any {
            it.text.startsWith("modifier") && it.typeFqName()?.asString() == MODIFIER
        }


    private fun KtNamedFunction.startOffsetToFunKeyword(): Int? = funKeyword?.startOffset

    private fun KtParameter.typeFqName(): FqName? = this.descriptor?.type?.fqName

    private fun KtCallExpression.containsObjectModifierWithoutTag(): Boolean {
        val resolvedCall = resolveToCall()
        var containsDeclaredModifier = valueArguments.isEmpty()
        val explicitArgumentWithoutTag = valueArguments.any {
            val argumentExpression = it.getArgumentExpression()?.text
            val param = resolvedCall?.getArgumentMapping(it) as? ArgumentMatch
            val isModifierParam = param?.valueParameter?.name?.asString()  == "modifier"

            if (isModifierParam) {
                containsDeclaredModifier = true
            }

            isModifierParam
                    && it?.searchTopReceiverObject()?.fqName?.asString() == MODIFIER_COMPANION
                    && argumentExpression?.contains(testTagRegex) == false
        }

        if (valueArguments.isEmpty() || !containsDeclaredModifier) {
            return searchDeclaredFunction()?.valueParameters?.any { param ->
                param?.name == "modifier"
                        && param.defaultValue?.searchTopReceiverObject()?.fqName?.asString() == MODIFIER_COMPANION
                        && param.defaultValue?.text?.contains(testTagRegex) == false
            } == true
        }

        return explicitArgumentWithoutTag
    }

    private fun searchNameReference(expression: KtExpression): KtNameReferenceExpression? = when (expression) {
        is KtNameReferenceExpression -> expression
        is KtDotQualifiedExpression -> searchNameReference(expression.receiverExpression)
        else -> null
    }

    private fun KtExpression.searchTopReceiverObject(): KtObjectDeclaration? {
        val reference = searchNameReference(this)
        return reference?.references?.firstOrNull()?.resolve() as? KtObjectDeclaration
    }

    private fun KtValueArgument.searchTopReceiverObject(): KtObjectDeclaration? {
        val expression = getArgumentExpression() ?: return null

        return expression.searchTopReceiverObject()
    }

    private class TestTagLineMarkerInfo(element: KtCallExpression, val tag: String) : LineMarkerInfo<KtCallExpression>(
        element,
        element.textRange,
        Icons.ComposeTestTag,
        { "Copy possible testTag: $tag" },
        null,
        GutterIconRenderer.Alignment.CENTER,
        { "Copy possible testTag: $tag" },
    ) {
        override fun createGutterRenderer(): GutterIconRenderer {
            return object : LineMarkerGutterIconRenderer<KtCallExpression>(this) {
                override fun getClickAction(): AnAction = ComposeCopyTestTagAction(tag)
            }
        }
    }

    private companion object {
        private const val MODIFIER = "androidx.compose.ui.Modifier"
        private const val MODIFIER_COMPANION =  "$MODIFIER.Companion"
    }

}
