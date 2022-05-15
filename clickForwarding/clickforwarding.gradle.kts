import ProjectVersions.rlVersion

version = "1.0.0"

project.extra["PluginName"] = "Click listener"
project.extra["PluginDescription"] = "Listens for clicks external to the client"

plugins {
    id("com.github.johnrengelman.shadow") version "6.1.0"
    java
}

dependencies {
    implementation("com.github.kwhat:jnativehook:2.2.2")
    annotationProcessor(Libraries.lombok)
    annotationProcessor(Libraries.pf4j)
}

tasks {
    build {
        finalizedBy("shadowJar")
    }

    shadowJar {
        archiveClassifier.set("shaded")
    }

    jar {
        manifest {
            attributes(mapOf(
                    "Plugin-Version" to project.version,
                    "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                    "Plugin-Provider" to project.extra["PluginProvider"],
                    "Plugin-Description" to project.extra["PluginDescription"],
                    "Plugin-License" to project.extra["PluginLicense"]
            ))
        }
    }
}