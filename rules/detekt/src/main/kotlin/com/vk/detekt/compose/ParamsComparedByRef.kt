package com.vk.detekt.compose

import com.vk.rules.compose.FunctionSkippabilityChecker
import com.vk.rules.compose.SkippabilityResult
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleId
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import io.gitlab.arturbosch.detekt.api.internal.isSuppressedBy
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries

@RequiresTypeResolution
class ParamsComparedByRef(config: Config) : Rule(config) {

    private val checker = FunctionSkippabilityChecker(true)
    private val ignoredClasses: List<Regex> by config(emptyList<String>()) {
        it.map(String::toRegex)
    }

    override val issue = Issue(
        "ParamsComparedByRef",
        Severity.Performance,
        "Fix the parameters of the @Composable skip function so that it compares by equals rather than by reference.",
        Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val result = checker.analyze(function, bindingContext, ignoredClasses)

        if (result !is SkippabilityResult.Unstable) return
        // backward compatibility
        if (function.isSuppressedBy("NonSkippableComposable", emptySet())) return

        Reporter.reportSkippabilitySmells(this, "Function paramaters will be compared by refs", function, result)
    }

}