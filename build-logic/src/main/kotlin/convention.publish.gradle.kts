plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    pomFromGradleProperties()
    if (hasProperty("signingInMemoryKey")) {
        signAllPublications()
    }
}

publishing {
    repositories {
        maven {
            name = "LocalRoot"
            url = uri(rootProject.layout.projectDirectory.dir("libs"))
        }
        maven {
            name = "PropertyRoot"
            url = uri(properties["PROPERTY_MAVEN_ROOT"].toString())
        }
        maven {
            url = uri(System.getenv("REMOTE_REPO_PUBLIC_URL").orEmpty())
            name = "PublicRemote"
            credentials {
                username = System.getenv("REMOTE_REPO_PUBLIC_USER").orEmpty()
                password = System.getenv("REMOTE_REPO_PUBLIC_PASSWORD").orEmpty()
            }
        }
    }
}
