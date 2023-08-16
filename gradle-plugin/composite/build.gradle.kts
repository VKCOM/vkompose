plugins {
    `kotlin-dsl`
    id("convention.publish")
}

gradlePlugin {
    plugins {
        create("vkompose") {
            id = "com.vk.vkompose"
            implementationClass = "com.vk.gradle.plugin.compose.utils.VkomposePlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
    implementation(project(":sub-plugin:composable-skippability-checker"))
    implementation(project(":sub-plugin:compose-test-tag:applier"))
    implementation(project(":sub-plugin:compose-test-tag:cleaner"))
    implementation(project(":sub-plugin:compose-test-tag:drawer"))
    implementation(project(":sub-plugin:compose-source-information:cleaner"))
    implementation(project(":sub-plugin:recompose:highlighter"))
    implementation(project(":sub-plugin:recompose:logger"))
}
