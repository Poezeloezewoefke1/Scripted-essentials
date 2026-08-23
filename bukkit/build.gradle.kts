plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${rootProject.property("paperApiVersion")}")
    implementation(project(":core"))
}

tasks.processResources {
    val properties = mapOf("version" to project.version)
    inputs.properties(properties)
    filesMatching("plugin.yml") { expand(properties) }
}

tasks.shadowJar {
    archiveBaseName.set("ScriptedEssentials-Paper")
    archiveClassifier.set("")
    // Paper already provides Adventure and SnakeYAML; only this project's own core goes in.
    dependencies {
        include(project(":core"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
