plugins {
    id("convention.android.library")
    id("convention.publish")
}

android {
    namespace = "com.vk.recompose.highlighter"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
}