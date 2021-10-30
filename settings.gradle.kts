rootProject.name = "Nader Plugins"

include(":spacespam")
include(":detachedcamera")
include(":OneClick")
include(":autoMouse")
include(":WildyAutoHop")
include(":npcstatus")
include(":NPCOverheadDialog")
//include(":chinglassblow")
//include(":CustomSwapper")

for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}
