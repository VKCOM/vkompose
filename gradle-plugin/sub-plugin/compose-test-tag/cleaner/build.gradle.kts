plugins {
    `kotlin-dsl`
    id("convention.publish")
    id("convention.buildconfig")
}

gradlePlugin {
    plugins {
        create("compose-test-tag-applier") {
            id = "com.vk.compose-test-tag-cleaner"
            implementationClass = "com.vk.gradle.plugin.compose.test.tag.cleaner.ComposeTestTagCleanerPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
}
