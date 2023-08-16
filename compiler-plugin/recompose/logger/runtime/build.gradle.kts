plugins {
    id("convention.android.library")
    id("convention.publish")
}

android {
    namespace = "com.vk.recompose.logger"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
}