package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.internal.engine.JSContextFactory
import com.chattriggers.ctjs.internal.engine.JSLoader
import org.apache.commons.io.FileUtils
import org.mozilla.javascript.Context
import java.io.File
import java.net.URLClassLoader
import java.util.*

object ModuleManager {
    val cachedModules = mutableListOf<Module>()
    val modulesFolder = File(CTJS.MODULES_FOLDER)
    private val pendingOldModules = mutableListOf<Module>()

    fun setup() {
        modulesFolder.mkdirs()

        // Get existing modules
        val installedModules = getFoldersInDir(modulesFolder).map(::parseModule).distinctBy {
            it.name.lowercase()
        }

        // Check if those modules have updates
        installedModules.forEach(ModuleUpdater::updateModule)
        cachedModules.addAll(installedModules)

        // Import required modules
        installedModules.distinct().forEach { module ->
            module.metadata.requires?.forEach { ModuleUpdater.importModule(it, module.name) }
        }

        sortModules()

        loadAssetsAndJars(cachedModules)
    }

    private fun loadAssetsAndJars(modules: List<Module>) {
        // Load assets
        loadAssets(modules)

        // Normalize all metadata
        modules.forEach {
            it.metadata.entry = it.metadata.entry?.replace('/', File.separatorChar)?.replace('\\', File.separatorChar)
            it.metadata.mixinEntry =
                it.metadata.mixinEntry?.replace('/', File.separatorChar)?.replace('\\', File.separatorChar)
        }

        // Get all jars
        val jars = modules.map { module ->
            module.folder.walk().filter {
                it.isFile && it.extension == "jar"
            }.map {
                it.toURI().toURL()
            }.toList()
        }.flatten()

        JSLoader.setup(jars)
    }

    @JvmOverloads
    fun entryPass(modules: List<Module> = cachedModules, completionListener: (percentComplete: Float) -> Unit = {}) {
        JSLoader.entrySetup()

        val total = modules.count { it.metadata.entry != null }
        var completed = 0

        // Load the modules
        modules.filter {
            it.metadata.entry != null
        }.forEach {
            JSLoader.entryPass(it, File(it.folder, it.metadata.entry!!).toURI())
            completed++
            completionListener(completed.toFloat() / total)
        }
    }

    private fun getFoldersInDir(dir: File): List<File> {
        if (!dir.isDirectory) return emptyList()

        return dir.listFiles()?.filter {
            it.isDirectory
        } ?: listOf()
    }

    fun parseModule(directory: File): Module {
        val metadataFile = File(directory, "metadata.json")
        var metadata = ModuleMetadata()

        if (metadataFile.exists()) {
            try {
                metadata = CTJS.json.decodeFromString(metadataFile.readText())
            } catch (e: Exception) {
                "Module $directory has invalid metadata.json".printToConsole(LogType.ERROR)
            }
        }

        return Module(directory.name, metadata, directory)
    }

    data class ImportedModule(val module: Module?, val dependencies: List<Module>)

    fun importModule(moduleName: String): ImportedModule {
        val newModules = ModuleUpdater.importModule(moduleName)

        loadAssetsAndJars(newModules)

        newModules.forEach {
            if (it.metadata.mixinEntry != null)
                ChatLib.chat("&cModule ${it.name} has dynamic mixins which require a restart to take effect")
        }

        entryPass(newModules)

        return ImportedModule(newModules.getOrNull(0), newModules.drop(1))
    }

    fun deleteModule(name: String): Boolean {
        val module = cachedModules.find { it.name.lowercase() == name.lowercase() } ?: return false

        val file = File(modulesFolder, module.name)
        check(file.exists()) { "Expected module to have an existing folder!" }

        val context = JSContextFactory.enterContext()
        try {
            val classLoader = context.applicationClassLoader as URLClassLoader

            classLoader.close()

            if (file.deleteRecursively()) {
                CTJS.load()
                return true
            }
        } finally {
            Context.exit()
        }

        return false
    }

    fun reportOldVersions() {
        pendingOldModules.forEach(::reportOldVersion)
        pendingOldModules.clear()
    }

    fun tryReportOldVersion(module: Module) {
        if (World.isLoaded()) {
            reportOldVersion(module)
        } else {
            pendingOldModules.add(module)
        }
    }

    private fun reportOldVersion(module: Module) {
        ChatLib.chat(
            "&cWarning: the module \"${module.name}\" was made for an older version of CT, " +
                "so it may not work correctly."
        )
    }

    private fun loadAssets(modules: List<Module>) {
        modules.map {
            File(it.folder, "assets")
        }.filter {
            it.exists() && !it.isFile
        }.map {
            it.listFiles()?.toList() ?: emptyList()
        }.flatten().forEach {
            FileUtils.copyFileToDirectory(it, CTJS.assetsDir)
        }
    }

    fun teardown() {
        cachedModules.clear()
        JSLoader.clearTriggers()
    }

    private fun sortModules() {
        // Topological sort, Depth-first search
        // https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search

        val sortedModules = LinkedList<Module>()
        val permanentMarks = mutableSetOf<Module>()
        val temporaryMarks = LinkedHashSet<Module>()
        val unmarkedModules = cachedModules.toMutableSet()

        fun visit(module: Module) {
            if (module in permanentMarks)
                return

            if (module in temporaryMarks)
                error("Detected a module dependency cycle: ${temporaryMarks.joinToString(" -> ") { it.name }}")

            temporaryMarks.add(module)

            cachedModules.filter { module.name in it.requiredBy }.forEach(::visit)

            temporaryMarks.remove(module)
            permanentMarks.add(module)
            unmarkedModules.remove(module)

            // The Wikipedia algorithm sorts them with the dependants first, but we want them
            // last, so append to the end instead of the front
            sortedModules.add(module)
        }

        while (cachedModules.size != permanentMarks.size) {
            val module = unmarkedModules.take(1).single()
            unmarkedModules.remove(module)
            visit(module)
        }

        check(sortedModules.size == cachedModules.size)
        cachedModules.clear()
        cachedModules.addAll(sortedModules)
    }
}
