plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.vk.idea.plugin.vkompose"
version = "0.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.1.4")
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
        sinceBuild.set("223")
        untilBuild.set("231.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    runIde {
        ideDir.set(file(project.properties["studio.path"]?.toString().orEmpty()))

    }


    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencyLocking {
    lockAllConfigurations()
}
