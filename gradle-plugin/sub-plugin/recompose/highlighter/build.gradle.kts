plugins {
    `kotlin-dsl`
    id("convention.publish")
}

gradlePlugin {
    plugins {
        create("recompose-highlighter") {
            id = "com.vk.recompose-highlighter"
            implementationClass = "com.vk.gradle.plugin.recompose.highlighter.RecomposeHighlighterPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
}
