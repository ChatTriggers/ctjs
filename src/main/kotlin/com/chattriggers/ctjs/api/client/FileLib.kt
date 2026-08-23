package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.CTJS
import net.minecraft.util.Util
import java.io.*
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FileLib {
    /**
     * Writes a file to folder in modules.
     *
     * @param importName name of the import
     * @param fileName name of the file
     * @param toWrite string to write in file
     * @param recursive whether to create folders to the file location if they don't exist
     */
    @JvmStatic
    @JvmOverloads
    fun write(importName: String, fileName: String, toWrite: String, recursive: Boolean = false) {
        write(absoluteLocation(importName, fileName), toWrite, recursive)
    }

    /**
     * Writes a file to anywhere on the system.
     * Use "./" for the ".minecraft" folder.
     *
     * @param fileLocation the location and file name
     * @param toWrite string to write in file
     * @param recursive whether to create folders to the file location if they don't exist
     */
    @JvmStatic
    @JvmOverloads
    fun write(fileLocation: String, toWrite: String, recursive: Boolean = false) {
        File(fileLocation).apply {
            if (recursive && !exists()) {
                parentFile.mkdirs()
            }
        }.writeText(toWrite)
    }

    /**
     * Writes a file to folder in modules.
     *
     * @param importName name of the import
     * @param fileName name of the file
     * @param toAppend string to append in file
     */
    @JvmStatic
    fun append(importName: String, fileName: String, toAppend: String) {
        append(absoluteLocation(importName, fileName), toAppend)
    }

    /**
     * Writes a file to anywhere on the system.
     * Use "./" for the ".minecraft" folder.
     *
     * @param fileLocation the location and file name
     * @param toAppend string to append in file
     */
    @JvmStatic
    fun append(fileLocation: String, toAppend: String) {
        File(fileLocation).appendText(toAppend)
    }

    /**
     * Reads a file from folder in modules.
     * Returns null if file is not found.
     *
     * @param importName name of the import
     * @param fileName name of the file
     * @return the string in the file, or null if not found
     */
    @JvmStatic
    fun read(importName: String, fileName: String): String? {
        return read(File(absoluteLocation(importName, fileName)))
    }

    /**
     * Reads a file from anywhere on the system.
     * Use "./" for the ".minecraft" folder.
     * Returns null if file is not found.
     *
     * @param fileLocation the location and file name
     * @return the string in the file, or null if not found
     */
    @JvmStatic
    fun read(fileLocation: String): String? {
        return read(File(fileLocation))
    }

    /**
     * Reads a file from anywhere on the system using java.io.File.
     *
     * @param file the java.io.File to read
     * @return the string in the file, or null if not found
     */
    @JvmStatic
    fun read(file: File): String? {
        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Determines if a file or directory exists at the specified location
     *
     * @param importName name of the import
     * @param fileName name of the file
     * @return if the file exists
     */
    @JvmStatic
    fun exists(importName: String, fileName: String): Boolean {
        return exists(absoluteLocation(importName, fileName))
    }

    /**
     * Determines if a file or directory exists at the specified location
     *
     * @param fileLocation the path of the file
     * @return if the file exists
     */
    @JvmStatic
    fun exists(fileLocation: String): Boolean {
        return File(fileLocation).exists()
    }

    /**
     * Determines if a file or directory exists at the specified location
     *
     * @param importName name of the import
     * @param fileName name of the file
     * @return if the location is a directory
     */
    @JvmStatic
    fun isDirectory(importName: String, fileName: String): Boolean {
        return isDirectory(absoluteLocation(importName, fileName))
    }

    /**
     * Determines if a file or directory exists at the specified location
     *
     * @param fileLocation the path of the file
     * @return if the location is a directory
     */
    @JvmStatic
    fun isDirectory(fileLocation: String): Boolean {
        return File(fileLocation).isDirectory
    }

    /**
     * Gets the contents of a url as a string.
     *
     * @param theUrl the url to get the data from
     * @param userAgent the user agent to use in the connection
     * @return the string stored in the url content
     */
    @Throws(UnknownHostException::class)
    @JvmStatic
    @JvmOverloads
    fun getUrlContent(theUrl: String, userAgent: String? = "Mozilla/5.0"): String {
        val conn = CTJS.makeWebRequest(theUrl, userAgent)

        return conn.getInputStream().use {
            it.readBytes()
        }.toString(Charset.forName("UTF-8"))
    }

    /**
     * Deletes a file at the specified location
     *
     * @param importName name of the import
     * @param fileName name of the file
     * @return if the file was deleted
     */
    @JvmStatic
    fun delete(importName: String, fileName: String): Boolean {
        return delete(absoluteLocation(importName, fileName))
    }

    /**
     * Deletes a file at the specified location
     *
     * @param fileLocation the path of the file
     * @return if the file was deleted
     */
    @JvmStatic
    fun delete(fileLocation: String): Boolean {
        return File(fileLocation).delete()
    }

    /**
     * Deletes a directory at the specified location
     *
     * @param dir the directory to delete
     * @return if the directory was deleted
     */
    @JvmStatic
    fun deleteDirectory(dir: String): Boolean {
        return deleteDirectory(File(dir))
    }

    /**
     * Deletes a directory at the specified location
     *
     * @param dir the directory to delete
     * @return if the directory was deleted
     */
    @JvmStatic
    fun deleteDirectory(dir: File): Boolean {
        return dir.deleteRecursively()
    }

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exist).
     * @param zipFilePath the zip file path
     * @param destDirectory the destination directory
     * @throws IOException IOException
     */
    @Throws(IOException::class)
    @JvmStatic
    fun unzip(zipFilePath: String, destDirectory: String) {
        val destination = File(destDirectory).toPath().toAbsolutePath().normalize()
        val parent = destination.parent ?: throw IOException("Destination has no parent: $destination")
        Files.createDirectories(parent)
        val staging = Files.createTempDirectory(parent, ".ctjs-unzip-")

        try {
            var entries = 0
            ZipInputStream(FileInputStream(zipFilePath)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    val output = safeZipDestination(staging, entry)
                    if (entry.isDirectory) {
                        Files.createDirectories(output)
                    } else {
                        output.parent?.let(Files::createDirectories)
                        Files.newOutputStream(output).buffered().use { stream ->
                            zip.copyTo(stream)
                        }
                    }
                    zip.closeEntry()
                }
            }
            if (entries == 0)
                throw IOException("Zip archive contains no entries")

            commitExtractedFiles(staging, destination)
        } catch (e: Exception) {
            throw if (e is IOException) e else IOException("Failed to extract '$zipFilePath'", e)
        } finally {
            staging.toFile().deleteRecursively()
        }
    }

    private fun safeZipDestination(root: Path, entry: ZipEntry): Path {
        val name = entry.name.replace('\\', '/')
        if (
            name.startsWith('/') ||
            name.startsWith("//") ||
            WINDOWS_ABSOLUTE_PATH.matches(name)
        ) {
            throw IOException("Refusing absolute zip entry '${entry.name}'")
        }

        val output = root.resolve(name).normalize()
        if (!output.startsWith(root))
            throw IOException("Refusing zip entry outside destination '${entry.name}'")
        return output
    }

    private fun commitExtractedFiles(staging: Path, destination: Path) {
        Files.createDirectories(destination)
        Files.walk(staging).use { paths ->
            paths.sorted().forEach { source ->
                if (source == staging)
                    return@forEach
                val target = destination.resolve(staging.relativize(source)).normalize()
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target)
                } else {
                    target.parent?.let(Files::createDirectories)
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private val WINDOWS_ABSOLUTE_PATH = Regex("^[A-Za-z]:/.*")

    private fun absoluteLocation(importName: String, fileLocation: String): String {
        return CTJS.MODULES_FOLDER + File.separator + importName + File.separator + fileLocation
    }

    /**
     * Encodes a string to a base64 string
     *
     * @param toEncode string to encode
     * @return base64 encoded string
     */
    @JvmStatic
    fun encodeBase64(toEncode: String): String {
        return Base64.getEncoder().encodeToString(toEncode.toByteArray())
    }

    /**
     * Decodes a base64 string to a string
     *
     * @param toDecode base64 encoded string to decode
     * @return decoded string
     */
    @JvmStatic
    fun decodeBase64(toDecode: String): String {
        return String(Base64.getDecoder().decode(toDecode))
    }

    /**
     * Opens a url in the default browser
     *
     * @param url the url to open
     */
    @JvmStatic
    fun open(url: String) {
        Util.getPlatform().openUri(url)
    }

    /**
     * Opens a path in the file explorer
     *
     * @param path the path to open
     */
    @JvmStatic
    fun open(path: File) {
        Util.getPlatform().openFile(path)
    }
}
