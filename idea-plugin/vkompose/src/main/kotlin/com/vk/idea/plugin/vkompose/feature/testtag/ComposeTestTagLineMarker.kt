package com.vk.idea.plugin.vkompose.feature.testtag

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.vk.idea.plugin.vkompose.utils.ComposeClassName.Composable
import com.vk.idea.plugin.vkompose.utils.Icons
import com.vk.idea.plugin.vkompose.utils.hasAnnotation
import com.vk.idea.plugin.vkompose.utils.isInFileHeader
import com.vk.idea.plugin.vkompose.utils.safeCast
import java.awt.datatransfer.StringSelection
import javax.swing.Icon
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.projectStructure.ExternalCompilerVersionProvider
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
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

class ComposeTestTagLineMarker : LineMarkerProviderDescriptor() {
    override fun getName(): String = "Generated test tag"
    override fun getIcon(): Icon = Icons.ComposeTestTag
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>,
    ) {
        if (elements.firstOrNull()?.language != KotlinLanguage.INSTANCE) return

        elements.asSequence()
            .filterIsInstance<LeafPsiElement>()
            .filter { it.elementType == KtTokens.IDENTIFIER }
            .filterNot(LeafPsiElement::isInFileHeader)
            .forEach { leaf ->
                val nameReference = leaf.parent?.safeCast<KtNameReferenceExpression>() ?: return@forEach
                val callExpression = nameReference.parent?.safeCast<KtCallExpression>() ?: return@forEach
                val declaration = callExpression.searchDeclaredFunction() ?: return@forEach

                if (declaration.hasAnnotation(Composable)
                    && declaration.hasComposeModifier()
                    && callExpression.containsObjectModifierWithoutTag()
                ) {
                    val tag = createTag(callExpression, declaration)
                    result.add(
                        TestTagLineMarkerInfo(
                            element = leaf,
                            tag = tag,
                            message = "Copy possible testTag: $tag"
                        ),
                    )
                }
            }
    }

    private fun createTag(
        call: KtCallExpression,
        declaration: KtNamedFunction,
    ): String {
        val isKotlin2Using = ExternalCompilerVersionProvider.findLatest(call.project)?.kotlinVersion?.isAtLeast(2, 0) == true
        return buildString {
            append(call.containingKtFile.name.removeSuffix(".kt"))
            append("-")
            call.getParentOfExpression().let { parent ->
                append(parent?.name)
                append("(")
                if (isKotlin2Using) {
                    append(parent?.startOffset)
                } else {
                    append(parent?.startOffsetToFunKeyword())
                }
                append(")-")
            }
            append(declaration.name)
            append("(")
            append(call.startOffset)
            append(")")
        }
    }

    private fun KtCallExpression.getParentOfExpression(): KtNamedFunction? {
        val funcParent = getParentOfType<KtNamedFunction>(false) ?: return null
        val lambdaPropertyParent = getParentOfType<KtProperty>(false)
            ?.takeIf { it.isComposableLambda() }
            ?: return funcParent

        // call expression in lambda. but ir represents lambda in different way, therefore we should don't use names of function and lambda
        return if (lambdaPropertyParent.startOffset >= funcParent.startOffset) null else funcParent
    }

    private fun KtCallExpression.searchDeclaredFunction(): KtNamedFunction? =
        calleeExpression?.references?.firstOrNull()?.resolve()?.safeCast()

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
            val isModifierParam = param?.valueParameter?.name?.asString() == "modifier"

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
        return searchNameReference(this)?.references?.firstOrNull()?.resolve()?.safeCast()
    }

    private fun KtValueArgument.searchTopReceiverObject(): KtObjectDeclaration? {
        val expression = getArgumentExpression() ?: return null

        return expression.searchTopReceiverObject()
    }

    private class TestTagLineMarkerInfo(
        element: LeafPsiElement,
        tag: String,
        message: String,
    ) : LineMarkerInfo<LeafPsiElement>(
        /* element = */ element,
        /* range = */ element.textRange,
        /* icon = */ Icons.ComposeTestTag,
        /* tooltipProvider = */ { message },
        /* navHandler = */ createNavHandler(tag),
        /* alignment = */ GutterIconRenderer.Alignment.CENTER,
        /* accessibleNameProvider = */ { message },
    )

    private companion object {
        const val MODIFIER = "androidx.compose.ui.Modifier"
        const val MODIFIER_COMPANION = "$MODIFIER.Companion"
        val testTagRegex = "applyTestTag|testTag".toRegex()
    }
}

private fun createNavHandler(tag: String): GutterIconNavigationHandler<LeafPsiElement> {
    val transferableText = StringSelection(tag)
    val copyPasteManager = CopyPasteManager.getInstance()
    return GutterIconNavigationHandler { _, _ ->
        copyPasteManager.setContents(transferableText)
    }
}
