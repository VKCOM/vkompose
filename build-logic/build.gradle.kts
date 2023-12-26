plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.android)
    implementation(libs.gradle.publish)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}