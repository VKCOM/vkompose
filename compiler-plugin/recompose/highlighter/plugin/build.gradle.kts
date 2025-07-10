plugins {
    id("convention.kotlin")
    id("convention.publish")
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
}