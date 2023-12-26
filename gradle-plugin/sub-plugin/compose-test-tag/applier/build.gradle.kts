plugins {
    `kotlin-dsl`
    id("convention.publish")
    id("convention.buildconfig")
}

gradlePlugin {
    plugins {
        create("compose-test-tag-applier") {
            id = "com.vk.compose-test-tag-applier"
            implementationClass = "com.vk.gradle.plugin.compose.test.tag.applier.ComposeTestTagApplierPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.kotlin.api)
}
