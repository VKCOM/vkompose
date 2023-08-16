plugins {
    `kotlin-dsl`
    id("convention.publish")
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
