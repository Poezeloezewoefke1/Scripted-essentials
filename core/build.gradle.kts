// Platform-neutral code: no Minecraft, no Bukkit, no Fabric.
//
// Adventure and SnakeYAML are compileOnly because every supported platform already provides
// them: Paper bundles both, and the Fabric module brings them in itself. Keeping them off this
// module's runtime classpath means shading core into a platform jar pulls in nothing else.
plugins {
    java
}

dependencies {
    val adventure = rootProject.property("adventureVersion")
    compileOnly("net.kyori:adventure-api:$adventure")
    compileOnly("net.kyori:adventure-text-minimessage:$adventure")
    compileOnly("net.kyori:adventure-text-serializer-legacy:$adventure")
    compileOnly("net.kyori:adventure-text-serializer-plain:$adventure")
    compileOnly("org.yaml:snakeyaml:${rootProject.property("snakeyamlVersion")}")

    testImplementation("net.kyori:adventure-api:$adventure")
    testImplementation("net.kyori:adventure-text-minimessage:$adventure")
    testImplementation("net.kyori:adventure-text-serializer-legacy:$adventure")
    testImplementation("net.kyori:adventure-text-serializer-plain:$adventure")
    testImplementation("org.yaml:snakeyaml:${rootProject.property("snakeyamlVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
