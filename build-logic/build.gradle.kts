plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.android)
    implementation(libs.gradle.publish)
    implementation(libs.gradle.buildconfig)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}