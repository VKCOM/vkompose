enum class Version(val ideaVersion: String, val versionName: String, val sinceBuild: String, val untilBuild: String? = null) {
    Koala("2024.1", "Koala", "241"),
    Ladybug("2024.2", "Ladybug", "242"),
    Meerkat("2024.3", "Meerkat", "243"),

}

val currentVersion = Version.Koala

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.vk.idea.plugin.vkompose"
version = "0.4.0-${currentVersion.versionName}"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.runIde {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.kotlin.plugin.use.k2=true")
    }
}

intellijPlatform {
    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    pluginConfiguration.ideaVersion {
        sinceBuild.set(currentVersion.sinceBuild)
        currentVersion.untilBuild?.let(untilBuild::set)
    }

}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(currentVersion.ideaVersion)
        bundledPlugin("org.jetbrains.kotlin")

        val idePath = project.properties["studio.path"]?.toString().orEmpty()
        if (idePath.isNotEmpty()) {
            local(idePath)
        }
        instrumentationTools()
    }
}

tasks.composedJar.configure {
    destinationDirectory.set(rootDir)
}

dependencyLocking {
    lockAllConfigurations()
}
