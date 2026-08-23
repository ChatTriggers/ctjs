import org.gradle.kotlin.dsl.support.unzipTo
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.Copy
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

buildscript {
    dependencies {
        classpath(libs.versioning)
    }
}

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.loom)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
}

if (!project.hasProperty("full")) {
    project.gradle.startParameter.excludedTaskNames.add("kspKotlin")
}

version = property("mod_version").toString()

repositories {
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://repo.essential.gg/repository/maven-public")
}

dependencies {
    // To change the versions see the gradle/libs.versions.toml
    minecraft(libs.minecraft)
    implementation(libs.bundles.fabric)

    implementation(libs.bundles.included) { include(this) }
    implementation(libs.bundles.essential) {
        exclude("gg.essential", "universalcraft-1.18.1-fabric")
        include(this)
    }

    implementation(libs.modmenu)
    runtimeOnly(libs.devauth)
    dokkaPlugin(libs.versioning)

    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    implementation(project(":typing-generator"))
    ksp(project(":typing-generator"))
}

loom {
    accessWidenerPath.set(file("src/main/resources/ctjs.accesswidener"))
}

base {
    archivesName.set(property("archives_base_name") as String)
}

val prismInstanceDir = providers.gradleProperty("prismInstanceDir")
    .orElse(providers.environmentVariable("CTJSR_PRISM_INSTANCE"))
    .orElse("C:/Users/pisag/AppData/Roaming/PrismLauncher/instances/CTJSR-26.1.2")

val vigilanceModuleDir = providers.gradleProperty("vigilanceModuleDir")
    .orElse(providers.environmentVariable("CTJSR_VIGILANCE_MODULE_DIR"))
    .orElse("../Vigilance")
val testaddonModuleDir = providers.gradleProperty("testaddonModuleDir")
    .orElse(providers.environmentVariable("CTJSR_TESTADDON_MODULE_DIR"))
    .orElse("../testaddon")

// Minecraft 26.1+ is unobfuscated. The non-remap Loom plugin intentionally does not
// register remapJar, so its jar task is the directly loadable production artifact.
val runtimeJar = tasks.named<Jar>("jar")

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    processResources {
        val flkVersion = libs.versions.fabric.kotlin.get()
        val fapiVersion = libs.versions.fabric.api.get()
        val loaderVersion = libs.versions.loader.get()

        inputs.property("version", project.version)
        inputs.property("fabric_kotlin_version", flkVersion)
        inputs.property("fabric_api_version", fapiVersion)
        inputs.property("loader_version", loaderVersion)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "fabric_kotlin_version" to flkVersion,
                "fabric_api_version" to fapiVersion,
                "loader_version" to loaderVersion
            )
        }
    }

    withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
            moduleName.set("ctjs")
        }

        @OptIn(ExperimentalAbiValidation::class)
        abiValidation {
            enabled.set(true)
            filters {
                exclude {
                    byNames.add("com.chattriggers.ctjs.internal.**")
                }
            }
            legacyDump {
                referenceDumpDir.set(layout.projectDirectory.dir("api"))
            }
        }
    }

    jar {
        from("LICENSE") {
            rename { "${name}_${base.archivesName.get()}" }
        }
    }

    val deployPrism = register("deployPrism") {
        group = "development"
        description = "Deploys the loadable CTJS Reloaded jar to the configured PrismLauncher instance."
        dependsOn(runtimeJar)

        inputs.file(runtimeJar.flatMap { it.archiveFile })
        inputs.property("prismInstanceDir", prismInstanceDir)

        doLast {
            val instanceDirectory = file(prismInstanceDir.get()).canonicalFile
            require(instanceDirectory.isDirectory) {
                "PrismLauncher instance directory does not exist: $instanceDirectory"
            }

            val minecraftDirectory = instanceDirectory.resolve("minecraft").canonicalFile
            require(minecraftDirectory.isDirectory) {
                "Minecraft directory does not exist in the PrismLauncher instance: $minecraftDirectory"
            }

            val modsDirectory = minecraftDirectory.resolve("mods").canonicalFile
            require(modsDirectory.isDirectory) {
                "Mods directory does not exist in the PrismLauncher instance: $modsDirectory"
            }

            val sourceJar = runtimeJar.get().archiveFile.get().asFile.canonicalFile
            require(sourceJar.isFile) { "Runtime jar does not exist: $sourceJar" }
            require(sourceJar.name.startsWith("ctjs-reloaded-") && sourceJar.name.endsWith(".jar")) {
                "Unexpected remapped jar name: ${sourceJar.name}"
            }
            require(listOf("-sources.jar", "-dev.jar", "-javadoc.jar").none(sourceJar.name::endsWith)) {
                "Refusing to deploy a non-runtime jar: ${sourceJar.name}"
            }

            modsDirectory.listFiles()
                ?.filter { it.isFile && it.name.startsWith("ctjs-reloaded-") && it.name.endsWith(".jar") }
                ?.forEach { oldJar ->
                    require(oldJar.delete()) { "Failed to delete previously deployed jar: $oldJar" }
                    logger.lifecycle("Removed previous CTJS Reloaded jar: ${oldJar.absolutePath}")
                }

            val deployedJar = modsDirectory.resolve(sourceJar.name)
            Files.copy(sourceJar.toPath(), deployedJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            logger.lifecycle("Deployed CTJS Reloaded jar: ${deployedJar.canonicalPath}")
        }
    }

    fun registerModuleDeployTask(taskName: String, moduleName: String, sourceDirectory: Provider<String>) =
        register<Copy>(taskName) {
            group = "development"
            description = "Deploys the $moduleName ChatTriggers module to the configured PrismLauncher instance."

            from(sourceDirectory.map(::file))
            into(prismInstanceDir.map {
                file(it).resolve("minecraft/config/ChatTriggers/modules/$moduleName")
            })

            doFirst {
                val source = file(sourceDirectory.get()).canonicalFile
                require(source.isDirectory) { "ChatTriggers module source does not exist: $source" }

                val minecraftDirectory = file(prismInstanceDir.get()).resolve("minecraft").canonicalFile
                require(minecraftDirectory.isDirectory) {
                    "Minecraft directory does not exist in the PrismLauncher instance: $minecraftDirectory"
                }
            }

            doLast {
                val destination = file(prismInstanceDir.get())
                    .resolve("minecraft/config/ChatTriggers/modules/$moduleName")
                    .canonicalFile
                logger.lifecycle("Deployed ChatTriggers module $moduleName: ${destination.absolutePath}")
            }
        }

    val deployVigilanceModule = registerModuleDeployTask(
        "deployVigilanceModule",
        "Vigilance",
        vigilanceModuleDir,
    )
    val deployTestaddonModule = registerModuleDeployTask(
        "deployTestaddonModule",
        "testaddon",
        testaddonModuleDir,
    )

    register("deployModules") {
        group = "development"
        description = "Deploys the Vigilance wrapper and testaddon modules to PrismLauncher."
        dependsOn(deployVigilanceModule, deployTestaddonModule)
    }

    register("buildAndDeploy") {
        group = "development"
        description = "Builds CTJS Reloaded and deploys its remapped jar to PrismLauncher."
        dependsOn("build", deployPrism)
    }
    deployPrism.configure { mustRunAfter("build") }

    dokkaHtml {
        // Just use the module name here since the MC version doesn't affect CT's API
        // across the same mod version
        moduleVersion.set(project.version.toString())
        moduleName.set("ctjs")

        val docVersionsDir = projectDir.resolve("build/javadocs")
        val currentVersion = project.version.toString()
        val currentDocsDir = docVersionsDir.resolve(currentVersion)
        outputs.upToDateWhen { docVersionsDir.exists() }

        outputDirectory.set(file(currentDocsDir))

        pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
            version = project.version.toString()
            olderVersionsDir = docVersionsDir
            renderVersionsNavigationOnAllPages = true
        }

        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(true)

        val branch = providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim()
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(25)

                perPackageOption {
                    matchingRegex.set("com\\.chattriggers\\.ctjs\\.internal(\$|\\.).*")
                    suppress.set(true)
                }

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(URI("https://github.com/ChatTriggers/ctjs/blob/$branch/src/main/kotlin").toURL())
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
}

fun downloadFile(url: String): ByteArray {
    return (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        doOutput = true
    }.inputStream.readAllBytes()
}
