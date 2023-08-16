pluginManagement {
    includeBuild("./gradle-plugin")
    includeBuild("./build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "Compilers"
//include(":app")

include(":compiler-plugin")

include(":compiler-plugin:composable-skippability-checker")
include(":compiler-plugin:composable-skippability-checker:plugin")

include(":compiler-plugin:compose-test-tag")
include(":compiler-plugin:compose-test-tag:applier")
include(":compiler-plugin:compose-test-tag:drawer")
include(":compiler-plugin:compose-test-tag:cleaner")
include(":compiler-plugin:compose-test-tag:cleaner:plugin")
include(":compiler-plugin:compose-test-tag:applier:plugin")
include(":compiler-plugin:compose-test-tag:applier:runtime")
include(":compiler-plugin:compose-test-tag:drawer:plugin")
include(":compiler-plugin:compose-test-tag:drawer:runtime")

include(":compiler-plugin:compose-source-information")
include(":compiler-plugin:compose-source-information:cleaner")
include(":compiler-plugin:compose-source-information:cleaner:plugin")

include(":compiler-plugin:recompose")
include(":compiler-plugin:recompose:highlighter")
include(":compiler-plugin:recompose:highlighter:plugin")
include(":compiler-plugin:recompose:highlighter:runtime")
include(":compiler-plugin:recompose:logger")
include(":compiler-plugin:recompose:logger:plugin")
include(":compiler-plugin:recompose:logger:runtime")


include(":rules")
include(":rules:common")
include(":rules:detekt")
//include(":rules:ktlint")
