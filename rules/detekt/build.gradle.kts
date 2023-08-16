plugins {
    alias(libs.plugins.kotlin.jvm)
    id("convention.publish")
}

dependencies {
    implementation(libs.detekt.core)
    implementation(project(":rules:common"))
}
