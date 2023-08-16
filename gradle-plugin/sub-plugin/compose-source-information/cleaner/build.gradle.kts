plugins {
    `kotlin-dsl`
    id("convention.publish")
}

gradlePlugin {
    plugins {
        create("compose-source-information-cleaner") {
            id = "com.vk.compose-source-information-cleaner"
            implementationClass = "com.vk.gradle.plugin.compose.source.information.cleaner.SourceInformationCleanerPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
}
