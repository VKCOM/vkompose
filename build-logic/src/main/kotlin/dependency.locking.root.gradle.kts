val subprojectsTasks = subprojects
    .filter { it.file("build.gradle.kts").exists() }
    .onEach {
        it.dependencyLocking {
            lockAllConfigurations()
        }
    }.map { "${it.path}:dependencies" }


tasks.register("generateAllLocks").configure {
    notCompatibleWithConfigurationCache("it is normal for dependency report tasks")
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) { "should be called with --write-locks flag" }
    }
    dependsOn(subprojectsTasks)
}

