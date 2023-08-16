package com.vk.compiler.plugin.composable.skippability.checker.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SkippabilityCheckerFirExtensionRegistrar : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SkippabilityCheckerFirCheckersExtension
    }

    private class SkippabilityCheckerFirCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
        override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
            override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = setOf(
                SkippabilitySimpleFunctionChecker
            )
        }
    }

}