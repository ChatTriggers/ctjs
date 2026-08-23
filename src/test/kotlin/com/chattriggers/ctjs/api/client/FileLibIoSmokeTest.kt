package com.chattriggers.ctjs.api.client

import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileLibIoSmokeTest {
    private val root = createTempDirectory("ctjs-filelib-").toFile()

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    @Test
    fun `write append read exists and delete preserve public semantics`() {
        val file = root.resolve("nested/data.txt")
        FileLib.write(file.path, "one", recursive = true)
        FileLib.append(file.path, "-two")
        assertEquals("one-two", FileLib.read(file))
        assertTrue(FileLib.exists(file.path))
        assertTrue(FileLib.delete(file.path))
        assertFalse(FileLib.exists(file.path))
    }

    @Test
    fun `missing read returns null`() {
        assertNull(FileLib.read(root.resolve("missing.txt")))
    }
}
