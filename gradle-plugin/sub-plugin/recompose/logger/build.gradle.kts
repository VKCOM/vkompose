plugins {
    `kotlin-dsl`
    id("convention.publish")
    id("convention.buildconfig")
}

gradlePlugin {
    plugins {
        create("recompose-logger") {
            id = "com.vk.recompose-logger"
            implementationClass = "com.vk.gradle.plugin.recompose.logger.RecomposeLoggerPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
}
