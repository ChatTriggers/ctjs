package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.CTEvents
import com.chattriggers.ctjs.internal.engine.module.ModuleManager.cachedModules
import com.chattriggers.ctjs.internal.engine.module.ModuleManager.modulesFolder
import com.chattriggers.ctjs.internal.utils.Initializer
import com.chattriggers.ctjs.internal.utils.toVersion
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object ModuleUpdater : Initializer {
    private val changelogs = mutableListOf<ModuleMetadata>()
    private var shouldReportChangelog = false

    override fun init() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> shouldReportChangelog = true }

        CTEvents.RENDER_OVERLAY.register { _, _ ->
            if (shouldReportChangelog) {
                changelogs.forEach(::reportChangelog)
                changelogs.clear()
            }
        }
    }

    private fun tryReportChangelog(module: ModuleMetadata) {
        if (shouldReportChangelog) {
            reportChangelog(module)
        } else {
            changelogs.add(module)
        }
    }

    private fun reportChangelog(module: ModuleMetadata) {
        ChatLib.chat("&a[ChatTriggers] ${module.name} has updated to version ${module.version}")
        ChatLib.chat("&aChangelog: &r${module.changelog}")
    }

    fun updateModule(module: Module) {
        if (!Config.autoUpdateModules) return

        val metadata = module.metadata

        try {
            if (metadata.name == null) return

            "Checking for update in ${metadata.name}".printToConsole()

            val url = "${CTJS.WEBSITE_ROOT}/api/modules/${metadata.name}/metadata?modVersion=${CTJS.MOD_VERSION}"
            val connection = CTJS.makeWebRequest(url)

            val newMetadataText = connection.getInputStream().bufferedReader().readText()
            val newMetadata = CTJS.json.decodeFromString<ModuleMetadata>(newMetadataText)

            if (newMetadata.version == null) {
                ("Remote version of module ${metadata.name} has no version numbers, so it will " +
                    "not be updated!").printToConsole(LogType.WARN)
                return
            } else if (metadata.version != null && metadata.version.toVersion() >= newMetadata.version.toVersion()) {
                return
            }

            downloadModule(metadata.name)
            "Updated module ${metadata.name}".printToConsole()

            module.metadata = File(module.folder, "metadata.json").let {
                CTJS.json.decodeFromString<ModuleMetadata>(it.readText())
            }

            if (Config.moduleChangelog && module.metadata.changelog != null) {
                tryReportChangelog(module.metadata)
            }
        } catch (e: Exception) {
            "Can't find page for ${metadata.name}".printToConsole(LogType.WARN)
        }
    }

    fun importModule(moduleName: String, requiredBy: String? = null): List<Module> {
        val alreadyImported = cachedModules.any {
            if (it.name.equals(moduleName, ignoreCase = true)) {
                if (requiredBy != null) {
                    it.metadata.isRequired = true
                    it.requiredBy.add(requiredBy)
                }

                true
            } else false
        }

        if (alreadyImported) return emptyList()

        val (realName, modVersion) = downloadModule(moduleName) ?: return emptyList()

        val moduleDir = File(modulesFolder, realName)
        val module = ModuleManager.parseModule(moduleDir)
        module.targetModVersion = modVersion.toVersion()

        if (requiredBy != null) {
            module.metadata.isRequired = true
            module.requiredBy.add(requiredBy)
        }

        cachedModules.add(module)
        return listOf(module) + (module.metadata.requires?.map {
            importModule(it, module.name)
        }?.flatten() ?: emptyList())
    }

    data class DownloadResult(val name: String, val modVersion: String)

    private fun downloadModule(name: String): DownloadResult? {
        val downloadZip = File(modulesFolder, "currDownload.zip")

        try {
            val url = "${CTJS.WEBSITE_ROOT}/api/modules/$name/scripts?modVersion=${CTJS.MOD_VERSION}"
            val connection = CTJS.makeWebRequest(url)
            FileUtils.copyInputStreamToFile(connection.getInputStream(), downloadZip)
            FileSystems.newFileSystem(downloadZip.toPath()).use {
                val rootFolder = Files.newDirectoryStream(it.rootDirectories.first()).iterator()
                if (!rootFolder.hasNext()) throw Exception("Too small")
                val moduleFolder = rootFolder.next()
                if (rootFolder.hasNext()) throw Exception("Too big")

                val realName = moduleFolder.fileName.toString().trimEnd(File.separatorChar)
                File(modulesFolder, realName).apply { mkdir() }
                Files.walk(moduleFolder).forEach { path ->
                    val resolvedPath = Paths.get(CTJS.MODULES_FOLDER, path.toString())
                    if (Files.isDirectory(resolvedPath)) {
                        return@forEach
                    }
                    Files.copy(path, resolvedPath, StandardCopyOption.REPLACE_EXISTING)
                }
                return DownloadResult(realName, connection.getHeaderField("CT-Version"))
            }
        } catch (exception: Exception) {
            exception.printTraceToConsole()
        } finally {
            downloadZip.delete()
        }

        return null
    }
}
