package com.chattriggers.ctjs.internal.engine.module

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

internal object ModuleAssetManager {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Serializable
    private data class Manifest(
        val version: Int = 1,
        val modules: Map<String, List<String>> = emptyMap(),
        val outputs: Map<String, Output> = emptyMap(),
    )

    @Serializable
    private data class Output(
        val owner: String,
        val sha256: String,
    )

    private data class Source(
        val module: String,
        val file: File,
    )

    data class Result(
        val copied: Int,
        val removed: Int,
        val collisions: Map<String, List<String>>,
    )

    fun reconcile(
        modules: List<Module>,
        assetsRoot: File,
        manifestFile: File,
        warning: (String) -> Unit = {},
    ): Result {
        assetsRoot.mkdirs()
        manifestFile.parentFile?.mkdirs()

        val previous = readManifest(manifestFile, warning)
        val modulePaths = linkedMapOf<String, MutableList<String>>()
        val candidates = linkedMapOf<String, MutableList<Source>>()

        modules.forEach { module ->
            val moduleName = module.name.lowercase()
            val sourceRoot = File(module.folder, "assets")
            val paths = modulePaths.getOrPut(moduleName) { mutableListOf() }
            if (!sourceRoot.isDirectory)
                return@forEach

            sourceRoot.walkTopDown().filter(File::isFile).forEach { source ->
                val relative = source.relativeTo(sourceRoot).invariantSeparatorsPath
                val destination = File(assetsRoot, relative).canonicalFile
                require(destination.toPath().startsWith(assetsRoot.canonicalFile.toPath())) {
                    "Module ${module.name} asset escapes the CTJS assets directory: $relative"
                }
                paths += relative
                candidates.getOrPut(relative) { mutableListOf() } += Source(module.name, source)
            }
        }

        val collisions = candidates
            .filterValues { it.size > 1 }
            .mapValues { (_, sources) -> sources.map(Source::module) }
        collisions.forEach { (path, owners) ->
            warning("Module asset '$path' is provided by ${owners.joinToString()}; ${owners.last()} takes precedence")
        }

        val desiredOutputs = linkedMapOf<String, Output>()
        var copied = 0
        candidates.forEach { (relative, sources) ->
            val winner = sources.last()
            val destination = File(assetsRoot, relative)
            destination.parentFile?.mkdirs()
            val sourceHash = sha256(winner.file)
            if (!destination.isFile || sha256(destination) != sourceHash) {
                Files.copy(winner.file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                copied++
            }
            desiredOutputs[relative] = Output(winner.module, sourceHash)
        }

        var removed = 0
        (previous.outputs.keys - desiredOutputs.keys).forEach { relative ->
            val destination = File(assetsRoot, relative)
            val previousOutput = previous.outputs.getValue(relative)
            if (destination.isFile && sha256(destination) == previousOutput.sha256) {
                if (destination.delete()) {
                    removed++
                    removeEmptyParents(destination.parentFile, assetsRoot.canonicalFile)
                }
            } else if (destination.exists()) {
                warning("Preserving modified former module asset '$relative'")
            }
        }

        val manifest = Manifest(
            modules = modulePaths.mapValues { (_, paths) -> paths.distinct().sorted() },
            outputs = desiredOutputs,
        )
        writeManifest(manifestFile, manifest)
        return Result(copied, removed, collisions)
    }

    private fun readManifest(file: File, warning: (String) -> Unit): Manifest {
        if (!file.isFile)
            return Manifest()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            warning("Ignoring invalid module asset manifest ${file.absolutePath}: ${e.message}")
            Manifest()
        }
    }

    private fun writeManifest(file: File, manifest: Manifest) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json.encodeToString(Manifest.serializer(), manifest))
        try {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0)
                    break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun removeEmptyParents(start: File?, root: File) {
        var current = start?.canonicalFile
        while (current != null && current != root && current.toPath().startsWith(root.toPath())) {
            if (current.list()?.isNotEmpty() != false || !current.delete())
                return
            current = current.parentFile
        }
    }
}
