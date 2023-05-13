plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("fabric-loom") version "1.2-SNAPSHOT"
    id("io.github.juuxel.loom-quiltflower") version "1.8.0"
}

version = property("mod_version")!!
group = property("mod_group")!!
val yarnMappings = property("yarn_mappings")!!

repositories {
    // TODO: Host Rhino elsewhere
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")

    include(modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")!!)
    include(modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")!!)

    include(modImplementation("net.fabricmc:fabric-language-kotlin:1.9.4+kotlin.1.8.21")!!)
    include(modImplementation("com.chattriggers:rhino:1.8.6")!!)
    include(modImplementation("com.fasterxml.jackson.core:jackson-core:2.13.2")!!)
    include(modImplementation("com.fifesoft:rsyntaxtextarea:3.2.0")!!)
    include(modImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")!!)
    include(modImplementation("com.github.char:Koffee:3a78d8a437")!!)

    // 1.18 versions are good enough for Elementa and Vigilance, but not UC, so we
    // need to exclude this version
    configurations.modApi { exclude("gg.essential", "universalcraft-1.18.1-fabric") }

    include(modImplementation("gg.essential:vigilance-1.18.1-fabric:284")!!)
    include(modImplementation("gg.essential:universalcraft-1.19.4-fabric:262")!!)
    include(modImplementation("gg.essential:elementa-1.18.1-fabric:587")!!)
}

base {
    archivesName.set(property("archives_base_name") as String)
}

tasks {
    processResources {
        inputs.property("yarn_mappings", yarnMappings)
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version, "yarn_mappings" to yarnMappings)
        }
    }

    withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    jar {
        from("LICENSE") {
            rename { "${name}_${base.archivesName.get()}" }
        }
    }
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
