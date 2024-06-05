plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.kotlin.compose)
    implementation(libs.gradle.android)
    implementation(libs.gradle.publish)
    implementation(libs.gradle.buildconfig)
}