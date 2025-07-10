
plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}