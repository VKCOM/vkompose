import java.net.URI

plugins {
    id("com.vanniktech.maven.publish")
}

val String.prop: String
    get() = properties[this].toString()

group = "GROUP".prop
version = "VERSION_NAME".prop

mavenPublishing {
    publishToMavenCentral()
    coordinates("GROUP".prop, "POM_ARTIFACT_ID".prop, "VERSION_NAME".prop)

    pom {
        name = "POM_NAME".prop
        description = "POM_DESCRIPTION".prop
        inceptionYear = "POM_INCEPTION_YEAR".prop
        url = "POM_URL".prop
        licenses {
            license {
                name = "POM_LICENSE_NAME".prop
                url = "POM_LICENSE_URL".prop
                distribution = "POM_LICENSE_DIST".prop
            }
        }
        developers {
            developer {
                id = "POM_DEVELOPER_ID".prop
                name = "POM_DEVELOPER_NAME".prop
                url = "POM_DEVELOPER_URL".prop
            }
        }
        scm {
            url = "POM_SCM_URL".prop
            connection = "POM_SCM_CONNECTION".prop
            developerConnection = "POM_SCM_DEV_CONNECTION".prop
        }
    }
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
            url = properties["PROPERTY_MAVEN_ROOT"]?.toString()?.takeIf { it.isNotBlank() }?.let(::uri) ?: URI.create("")
        }
    }
}
