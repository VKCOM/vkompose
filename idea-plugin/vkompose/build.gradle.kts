enum class Version(val ideaVersion: String, val versionName: String, val sinceBuild: String, val untilBuild: String? = null) {
    Hedgehog("2023.1.4", "Hedgehog", "223", "231.*"),
    Iguana("2023.2", "Iguana", "232"),
    Jellyfish("2023.3", "Jellyfish", "232"),
    Koala("2024.1", "Koala", "241"),
}

val currentVersion = Version.Koala

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.vk.idea.plugin.vkompose"
version = "0.2.4-${currentVersion.versionName}"

repositories {
    mavenCentral()
}

intellij {
    version.set(currentVersion.ideaVersion)
    plugins.set(listOf("org.jetbrains.kotlin"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }

    patchPluginXml {
        sinceBuild.set(currentVersion.sinceBuild)
        currentVersion.untilBuild?.let(untilBuild::set)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    runIde {
        val idePath = project.properties["studio.path"]?.toString().orEmpty()
        if (idePath.isNotEmpty()) {
            ideDir.set(file(idePath))
        }
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencyLocking {
    lockAllConfigurations()
}
