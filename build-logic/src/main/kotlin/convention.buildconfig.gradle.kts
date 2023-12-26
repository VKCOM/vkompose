plugins {
    id("com.github.gmazzo.buildconfig")
}

buildConfig {
    buildConfigField("VERSION", properties["VERSION_NAME"].toString())
}