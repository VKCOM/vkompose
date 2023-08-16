plugins {
    id("convention.android.library")
    id("convention.publish")
}

android {
    namespace = "com.vk.compose.test.tag.drawer"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.foundation)
}