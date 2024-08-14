plugins {
    kotlin("jvm")
    id("convention.publish")
}

dependencies {
    implementation(libs.detekt.core)
    implementation(project(":common"))
}
