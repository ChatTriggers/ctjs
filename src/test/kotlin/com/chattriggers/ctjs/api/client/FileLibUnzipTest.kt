package com.chattriggers.ctjs.api.client

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileLibUnzipTest {
    private val root = createTempDirectory("ctjs-unzip-").toFile()

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    @Test
    fun `normal nested files and empty directories extract`() {
        val zip = zip(
            "root.txt" to "root",
            "nested/child.txt" to "child",
            "empty/" to null,
        )
        val output = root.resolve("output")

        FileLib.unzip(zip.path, output.path)

        assertEquals("root", output.resolve("root.txt").readText())
        assertEquals("child", output.resolve("nested/child.txt").readText())
        assertTrue(output.resolve("empty").isDirectory)
        assertTrue(zip.delete())
    }

    @Test
    fun `zip slip and absolute entries are rejected without partial output`() {
        listOf("../evil.txt", "/absolute.txt", "C:/absolute.txt").forEachIndexed { index, unsafe ->
            val zip = zip("safe.txt" to "safe", unsafe to "evil", name = "unsafe-$index.zip")
            val output = root.resolve("unsafe-output-$index")
            assertFailsWith<IOException> { FileLib.unzip(zip.path, output.path) }
            assertFalse(output.exists())
            assertFalse(root.parentFile.resolve("evil.txt").exists())
            assertTrue(zip.delete())
        }
    }

    @Test
    fun `malformed archive leaves no output or file lock`() {
        val zip = root.resolve("malformed.zip").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val output = root.resolve("malformed-output")

        assertFailsWith<IOException> { FileLib.unzip(zip.path, output.path) }
        assertFalse(output.exists())
        assertTrue(zip.delete())
    }

    private fun zip(vararg entries: Pair<String, String?>, name: String = "input.zip"): File {
        val file = root.resolve(name)
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (path, contents) ->
                zip.putNextEntry(ZipEntry(path))
                if (contents != null)
                    zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }
}
