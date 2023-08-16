package com.vk.detekt.compose

import com.vk.rules.compose.FunctionSkippabilityChecker
import com.vk.rules.compose.SkippabilityResult
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtNamedFunction

@RequiresTypeResolution
class NonSkippableComposableRule(config: Config) : Rule(config) {

    private val checker = FunctionSkippabilityChecker()
    private val ignoredClasses: List<Regex> by config(emptyList<String>()) {
        it.map(String::toRegex)
    }

    override val issue = Issue(
        "NonSkippableComposable",
        Severity.Performance,
        "Fix parameters of restartable @Composable function to make it skippable",
        Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val result = checker.analyze(function, bindingContext, ignoredClasses)

        if (result !is SkippabilityResult.Unstable) return

        val smells = buildList {
            add(
                CodeSmell(
                    issue,
                    Entity.from(function),
                    "Non skippable composable function"
                )
            )

            addAll(
                result.unstableParams.map { (param, stability) ->
                    CodeSmell(
                        issue,
                        Entity.from(param),
                        "Parameter: $stability"
                    )
                }
            )

            addAll(
                result.unstableContextReceiver.map { (receiver, stability) ->
                    CodeSmell(
                        issue,
                        Entity.from(receiver),
                        "Context receiver: $stability"
                    )
                }
            )

            val dispatchReceiverStability = result.dispatchReceiverStability
            if (dispatchReceiverStability != null) {
                CodeSmell(
                    issue,
                    Entity.from(
                        function.nameIdentifier?.originalElement ?: function.originalElement
                    ),
                    "Wrapper class: $dispatchReceiverStability"
                )
            }

            val extensionReceiverStability = result.extensionReceiverStability
            if (extensionReceiverStability != null) {
                CodeSmell(
                    issue,
                    Entity.from(
                        function.receiverTypeReference?.originalElement ?: function.originalElement
                    ),
                    "Extension receiver: $extensionReceiverStability"
                )
            }
        }

        report(smells)
    }
}