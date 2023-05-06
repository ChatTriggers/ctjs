plugins {
    kotlin("jvm") version "1.8.21"
    id("fabric-loom") version "1.2-SNAPSHOT"
    id("io.github.juuxel.loom-quiltflower") version "1.8.0"
}

version = property("mod_version")!!
group = property("mod_group")!!

repositories {
    // TODO: Host Rhino elsewhere
    maven("https://repo.essential.gg/repository/maven-public")
}

dependencies {
	minecraft("com.mojang:minecraft:${property("minecraft_version")}")
	mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
	modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    modImplementation("com.chattriggers:rhino:1.8.6")
    modImplementation("com.fasterxml.jackson.core:jackson-core:2.13.2")
    modImplementation("com.fifesoft:rsyntaxtextarea:3.2.0")

    // 1.18 versions are good enough for Elementa and Vigilance, but not UC, so we
    // need to exclude this version
    configurations.modApi { exclude("gg.essential", "universalcraft-1.18.1-fabric") }

    modImplementation("gg.essential:vigilance-1.18.1-fabric:284")
    modImplementation("gg.essential:universalcraft-1.19.4-fabric:262")
    modImplementation("gg.essential:elementa-1.18.1-fabric:587")
}

base {
	archivesName.set(property("archives_base_name") as String)
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    jar {
        from("LICENSE") {
            rename { "${name}_${base.archivesName.get()}"}
        }
    }
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
