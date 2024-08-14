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
import io.gitlab.arturbosch.detekt.api.internal.isSuppressedBy
import org.jetbrains.kotlin.psi.KtNamedFunction

@RequiresTypeResolution
class NonSkippableComposable(config: Config) : Rule(config) {

    private val checker = FunctionSkippabilityChecker(false)
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
        // backward compatibility
        if (function.isSuppressedBy("ParamsComparedByRef", emptySet())) return

        Reporter.reportSkippabilitySmells(this, "Non skippable composable function", function, result)
    }

}