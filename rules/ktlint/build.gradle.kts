plugins {
    alias(libs.plugins.kotlin.jvm)
    id("convention.publish")
}

dependencies {
    implementation(libs.ktlint.core)
    implementation(project(":rules:common"))
}
