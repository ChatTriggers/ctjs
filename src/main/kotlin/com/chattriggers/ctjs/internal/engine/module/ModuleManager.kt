package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.internal.engine.GenerationModuleClassLoader
import com.chattriggers.ctjs.internal.engine.JSLoader
import com.chattriggers.ctjs.internal.lifecycle.RuntimeGenerations
import com.chattriggers.ctjs.internal.lifecycle.RuntimeOwner
import java.io.File
import java.util.*

object ModuleManager {
    val cachedModules = mutableListOf<Module>()
    val modulesFolder = File(CTJS.MODULES_FOLDER)
    private val pendingOldModules = mutableListOf<Module>()

    fun setup(): PreparedSetup {
        try {
            modulesFolder.mkdirs()

            val installedModules = getFoldersInDir(modulesFolder).map(::parseModule).distinctBy {
                it.name.lowercase()
            }

            installedModules.forEach(ModuleUpdater::updateModule)
            cachedModules.addAll(installedModules)

            installedModules.distinct().forEach { module ->
                module.metadata.requires?.forEach { ModuleUpdater.importModule(it, module.name) }
            }

            val sorted = ModuleDependencyResolver.sort(cachedModules)
            cachedModules.clear()
            cachedModules.addAll(sorted)

            loadAssetsAndNormalize(cachedModules)
            return PreparedSetup(JSLoader.prepareGeneration(findJars(cachedModules)))
        } catch (e: Throwable) {
            cachedModules.clear()
            throw e
        }
    }

    internal fun publishPrepared(prepared: PreparedSetup, owner: RuntimeOwner): Boolean {
        val runtime = prepared.takeRuntime() ?: return false
        return JSLoader.publishPrepared(runtime, owner)
    }

    fun publishInitial(prepared: PreparedSetup): Boolean =
        publishPrepared(prepared, RuntimeGenerations.currentOwner())

    private fun loadAssetsAndNormalize(modules: List<Module>) {
        loadAssets(modules)

        // Normalize all metadata
        modules.forEach {
            it.metadata.entry = it.metadata.entry?.replace('/', File.separatorChar)?.replace('\\', File.separatorChar)
            it.metadata.mixinEntry =
                it.metadata.mixinEntry?.replace('/', File.separatorChar)?.replace('\\', File.separatorChar)
        }

    }

    private fun findJars(modules: List<Module>) =
        modules.map { module ->
            module.folder.walk().filter {
                it.isFile && it.extension == "jar"
            }.map {
                it.toURI().toURL()
            }.toList()
        }.flatten()

    @JvmOverloads
    fun entryPass(modules: List<Module> = cachedModules, completionListener: (percentComplete: Float) -> Unit = {}) {
        JSLoader.rebindMixinCallbacks(modules.filter { it.metadata.mixinEntry != null })
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

    class ImportedModule internal constructor(
        val module: Module?,
        val dependencies: List<Module>,
        internal val loader: GenerationModuleClassLoader?,
    ) {
        operator fun component1() = module
        operator fun component2() = dependencies
    }

    fun importModule(moduleName: String): ImportedModule {
        val importedModule = prepareImport(moduleName)
        activateImport(importedModule)
        return importedModule
    }

    internal fun prepareImport(moduleName: String): ImportedModule {
        val newModules = ModuleUpdater.importModule(moduleName)

        loadAssetsAndNormalize(newModules)
        val loader = JSLoader.addGenerationJars(findJars(newModules))

        return ImportedModule(newModules.getOrNull(0), newModules.drop(1), loader)
    }

    internal fun activateImport(importedModule: ImportedModule): Boolean {
        if (!JSLoader.isActiveLoader(importedModule.loader))
            return false

        val newModules = ModuleDependencyResolver.sort(
            listOfNotNull(importedModule.module) + importedModule.dependencies
        )

        newModules.forEach {
            if (it.metadata.mixinEntry != null)
                ChatLib.chat("&cModule ${it.name} has dynamic mixins which require a restart to take effect")
        }

        entryPass(newModules)
        return true
    }

    fun deleteModule(name: String): Boolean {
        val module = cachedModules.find { it.name.lowercase() == name.lowercase() } ?: return false

        val file = File(modulesFolder, module.name)
        check(file.exists()) { "Expected module to have an existing folder!" }

        if (file.deleteRecursively()) {
            CTJS.load()
            return true
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
        ModuleAssetManager.reconcile(
            modules,
            CTJS.assetsDir,
            File(CTJS.assetsDir.parentFile, ".module-assets.json"),
        ) { it.printToConsole(LogType.WARN) }
    }

    fun teardown() {
        cachedModules.clear()
        JSLoader.clearTriggers()
    }

    class PreparedSetup internal constructor(
        private var runtime: JSLoader.PreparedGenerationRuntime?,
    ) : AutoCloseable {
        internal fun takeRuntime(): JSLoader.PreparedGenerationRuntime? = synchronized(this) {
            runtime.also { runtime = null }
        }

        override fun close() {
            takeRuntime()?.close()
        }
    }

}
