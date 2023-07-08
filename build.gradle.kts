import org.gradle.kotlin.dsl.support.unzipTo
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:1.8.20")
    }
}

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("gg.essential.multi-version")

    // Apply defaults individually since "gg.essential.defaults" includes
    // mixin-extras which requires Essential
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
    id("gg.essential.defaults.repo")

    id("io.github.juuxel.loom-quiltflower") version "1.10.0"
    id("org.jetbrains.dokka") version "1.8.20"
}

val modVersion = property("mod_version")!!.toString()
val mcVersion = platform.toString()

version = "${modVersion}_$mcVersion"
group = property("mod_group")!!
val yarnMappings = property("yarn_mappings")!!

repositories {
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

dependencies {
    include(modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")!!)
    include(modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")!!)

    include(modImplementation("net.fabricmc:fabric-language-kotlin:1.9.4+kotlin.1.8.21")!!)
    include(modImplementation("com.github.ChatTriggers:rhino:64e93fb497")!!)
    include(modImplementation("com.fasterxml.jackson.core:jackson-core:2.13.2")!!)
    include(modImplementation("com.fifesoft:rsyntaxtextarea:3.2.0")!!)
    include(modImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")!!)
    include(modImplementation("com.github.char:Koffee:3a78d8a437")!!)

    include(implementation(annotationProcessor("com.github.llamalad7.mixinextras:mixinextras-fabric:0.2.0-beta.8")!!)!!)

    // 1.18 versions are good enough for Elementa and Vigilance, but not UC, so we
    // need to exclude this version
    configurations.modApi { exclude("gg.essential", "universalcraft-1.18.1-fabric") }

    include(modImplementation("gg.essential:vigilance-1.18.1-fabric:286")!!)
    include(modImplementation("gg.essential:universalcraft-$platform:277")!!)
    include(modImplementation("gg.essential:elementa-1.18.1-fabric:592")!!)

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.1.2")

    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.8.20")
}

loom {
    accessWidenerPath.set(rootProject.file("src/main/resources/chattriggers.accesswidener"))
}

base {
    archivesName.set(property("archives_base_name") as String)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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

    compileKotlin {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
            freeCompilerArgs = listOf("-Xcontext-receivers")
        }
    }

    jar {
        from("LICENSE") {
            rename { "${name}_${base.archivesName.get()}" }
        }
    }

    build {
        doLast {
            allprojects {
                copy {
                    from("build/libs")
                    into(rootProject.file("build"))
                }
            }
        }
    }
}

tasks.dokkaHtml {
    val docVersionsDir = projectDir.resolve("build/javadocs")
    val currentVersion = project.version.toString()
    val currentDocsDir = docVersionsDir.resolve(currentVersion)
    outputs.upToDateWhen { docVersionsDir.exists() }

    outputDirectory.set(file(currentDocsDir))

    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        version = currentVersion
        olderVersionsDir = docVersionsDir
        renderVersionsNavigationOnAllPages = true
    }

    suppressInheritedMembers.set(true)

    val branch = getBranch()
    dokkaSourceSets {
        configureEach {
            jdkVersion.set(17)

            for (pkg in setOf("engine.langs", "engine.loader", "engine.module", "utils", "listeners", "loader", "launch", "commands", "minecraft.wrappers.objects.threading")) {
                perPackageOption {
                    matchingRegex.set("${"com.chattriggers.ctjs.$pkg".replace(".", "\\.")}(\$|\\.).*")
                    suppress.set(true)
                }
            }

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/ChatTriggers/ctjs/blob/$branch/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }

    doFirst {
        val archiveBase = "https://www.chattriggers.com/javadocs-archive/"
        val versions = String(downloadFile(archiveBase + "versions")).lines().map(String::trim)
        val tmpFile = File(temporaryDir, "oldVersionsZip.zip")

        versions.filter(String::isNotEmpty).map(String::trim).forEach { version ->
            val zipBytes = downloadFile("$archiveBase$version.zip")
            tmpFile.writeBytes(zipBytes)
            unzipTo(docVersionsDir, tmpFile)
        }

        tmpFile.delete()
    }

    doLast {
        // At this point we have a structure that looks something like this:
        // javadocs
        //   \-- 2.2.0-1.8.9
        //   \-- 3.0.0
        //         \-- older
        //
        // The "older" directory contains all old versions, so we want to
        // delete the top-level older versions and move everything inside the
        // latest directory to the top level so the GitHub actions workflow
        // doesn't need to figure out the correct version name

        docVersionsDir.listFiles()?.forEach {
            if (it.name != version)
                it.deleteRecursively()
        }

        val latestVersionDir = docVersionsDir.listFiles()!!.single()
        latestVersionDir.listFiles()!!.forEach {
            it.renameTo(File(it.parentFile.parentFile, it.name))
        }
        latestVersionDir.deleteRecursively()
    }
}

fun downloadFile(url: String): ByteArray {
    return (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        doOutput = true
    }.inputStream.readAllBytes()
}

fun getBranch(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}
