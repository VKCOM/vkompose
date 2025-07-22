import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.library")
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    lint {
        // crashed for no reason
        disable.addAll(
            setOf(
                "MutableCollectionMutableState",
                "ComposableModifierFactory",
                "ModifierFactoryExtensionFunction",
                "ModifierFactoryReturnType",
                "ModifierFactoryUnreferencedReceiver",
                "AutoboxingStateCreation"
            )
        )
    }
}