plugins {
    id("com.github.gmazzo.buildconfig")
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    buildConfigField("VERSION", properties["VERSION_NAME"].toString())
}