package com.vk.detekt.compose

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class VkomposeRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "vkompose"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            NonSkippableComposable(config),
            ParamsComparedByRef(config)
        ),
    )
}