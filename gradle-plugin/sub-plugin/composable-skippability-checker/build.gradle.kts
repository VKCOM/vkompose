plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
    id("convention.publish")
    id("convention.buildconfig")
}

gradlePlugin {
    plugins {
        create("composable-skippability-checker") {
            id = "com.vk.composable-skippability-checker"
            implementationClass = "com.vk.gradle.plugin.composable.skippability.checker.ComposableSkippabilityCheckerPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
}
