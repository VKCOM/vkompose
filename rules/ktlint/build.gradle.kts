plugins {
    id("convention.kotlin")
    id("convention.publish")
}

dependencies {
    implementation(libs.ktlint.core)
    implementation(project(":rules:common"))
}
