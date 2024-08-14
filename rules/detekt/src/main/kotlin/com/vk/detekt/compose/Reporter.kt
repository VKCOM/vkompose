package com.vk.detekt.compose

import com.vk.rules.compose.SkippabilityResult
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtNamedFunction

internal object Reporter {
    fun reportSkippabilitySmells(
        rule: Rule,
        message: String,
        function: KtNamedFunction,
        result: SkippabilityResult.Unstable
    ) {
        val smells = buildList {
            add(
                CodeSmell(
                    rule.issue,
                    Entity.from(function),
                    message
                )
            )

            addAll(
                result.problemParams.map { (param, stability) ->
                    CodeSmell(
                        rule.issue,
                        Entity.from(param),
                        "Parameter: $stability"
                    )
                }
            )

            addAll(
                result.problemContextReceiver.map { (receiver, stability) ->
                    CodeSmell(
                        rule.issue,
                        Entity.from(receiver),
                        "Context receiver: $stability"
                    )
                }
            )

            val dispatchReceiverStability = result.dispatchReceiverStability
            if (dispatchReceiverStability != null) {
                CodeSmell(
                    rule.issue,
                    Entity.from(
                        function.nameIdentifier?.originalElement ?: function.originalElement
                    ),
                    "Wrapper class: $dispatchReceiverStability"
                )
            }

            val extensionReceiverStability = result.extensionReceiverStability
            if (extensionReceiverStability != null) {
                CodeSmell(
                    rule.issue,
                    Entity.from(
                        function.receiverTypeReference?.originalElement ?: function.originalElement
                    ),
                    "Extension receiver: $extensionReceiverStability"
                )
            }
        }

        rule.report(smells)
    }
}