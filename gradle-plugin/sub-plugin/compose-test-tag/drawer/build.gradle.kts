plugins {
    `kotlin-dsl`
    id("convention.publish")
    id("convention.buildconfig")
}

gradlePlugin {
    plugins {
        create("compose-test-tag-drawer") {
            id = "com.vk.compose-test-tag-drawer"
            implementationClass = "com.vk.gradle.plugin.compose.test.tag.drawer.ComposeTestTagDrawerPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
}
