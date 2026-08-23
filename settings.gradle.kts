rootProject.name = "ScriptedEssentials"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
    }
}

include("core")
include("bukkit")

// The Fabric module needs Fabric Loom, which resolves Minecraft and mappings from
// maven.fabricmc.net. It joins the build once it exists; skipFabric=true leaves it out for
// environments that cannot reach that repository.
if (file("fabric/build.gradle.kts").exists()
    && providers.gradleProperty("skipFabric").orNull != "true") {
    include("fabric")
}
