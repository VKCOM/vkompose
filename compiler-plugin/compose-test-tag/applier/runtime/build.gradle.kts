plugins {
    id("convention.android.library")
    id("convention.publish")
}

android {
    namespace = "com.vk.compose.test.tag.applier"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
}