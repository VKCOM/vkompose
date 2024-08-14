plugins {
    kotlin("jvm")
    id("convention.publish")
}

dependencies {
    api(libs.kotlin.compiler.embeddable)
}
