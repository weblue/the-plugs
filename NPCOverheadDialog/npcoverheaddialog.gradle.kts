version = "1.0.0"

project.extra["PluginName"] = "NPC Overhead Dialog"
project.extra["PluginDescription"] = "Sprinkles in some dialog to spice up the immersion"

dependencies {
    annotationProcessor(Libraries.lombok)
    annotationProcessor(Libraries.pf4j)

    compileOnly("com.openosrs:runelite-api:ProjectVersions.rlVersion")
    compileOnly("com.openosrs:runelite-client:ProjectVersions.rlVersion")
}

tasks {
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