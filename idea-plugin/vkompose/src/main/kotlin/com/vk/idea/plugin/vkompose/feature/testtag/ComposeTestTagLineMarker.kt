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
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

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
                    createTag(callExpression, declaration)?.let { tag ->
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
    }

    private fun createTag(
        call: KtCallExpression,
        declaration: KtNamedFunction,
    ): String? {
        val isKotlin2Using = KotlinPluginModeProvider.isK2Mode()
        val parentOfExpression = call.getParentOfExpression() ?: return null
        return buildString {
            append(call.containingKtFile.name.removeSuffix(".kt"))
            append("-")
            parentOfExpression.let { parent ->
                append(parent.name)
                append("(")
                if (isKotlin2Using) {
                    append(parent.startOffset)
                } else {
                    append(parent.startOffsetToFunKeyword())
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

    private fun KtDeclaration.isComposableLambda(): Boolean = analyze(this) {
        return returnType.isFunctionType && returnType.annotations.any { annotation -> annotation.classId?.asSingleFqName() == Composable }
    }

    private fun KtNamedFunction.hasComposeModifier(): Boolean =
        valueParameters.any {
            analyze(it) {
                val fqName =  it.symbol.returnType.expandedSymbol?.classId?.asSingleFqName()?.asString()
                it.text.startsWith("modifier") && fqName == MODIFIER
            }
        }


    private fun KtNamedFunction.startOffsetToFunKeyword(): Int? = funKeyword?.startOffset

    private fun KtCallExpression.containsObjectModifierWithoutTag(): Boolean = analyze(this) {
        val resolvedCall = resolveToCall()
        val functionCall = resolvedCall?.singleFunctionCallOrNull()
        var containsDeclaredModifier = valueArguments.isEmpty()
        val explicitArgumentWithoutTag = valueArguments.any { arg ->
            val argumentExpression = arg.getArgumentExpression()?.text
            val param = functionCall?.argumentMapping?.get(arg.getArgumentExpression())
            val isModifierParam = param?.name?.asString() == "modifier"

            if (isModifierParam) {
                containsDeclaredModifier = true
            }

            isModifierParam
                    && arg?.searchTopReceiverObject()?.fqName?.asString() == MODIFIER_COMPANION
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
