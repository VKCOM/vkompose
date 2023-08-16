pluginManagement {
    includeBuild("../build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "gradle-plugin"

include(":sub-plugin")
include(":composite")

include(":sub-plugin:composable-skippability-checker")

include(":sub-plugin:compose-test-tag")
include(":sub-plugin:compose-test-tag:applier")
include(":sub-plugin:compose-test-tag:cleaner")
include(":sub-plugin:compose-test-tag:drawer")

include(":sub-plugin:compose-source-information")
include(":sub-plugin:compose-source-information:cleaner")

include(":sub-plugin:recompose")
include(":sub-plugin:recompose:highlighter")
include(":sub-plugin:recompose:logger")

