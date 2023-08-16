plugins {
    alias(libs.plugins.kotlin.jvm)
    id("convention.publish")
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
}