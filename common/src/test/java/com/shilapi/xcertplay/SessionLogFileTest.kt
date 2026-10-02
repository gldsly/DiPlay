package com.shilapi.xcertplay

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rotation archives by renaming inside the log directory, and the line that triggered it survives. */
class SessionLogFileTest {
    private fun freshDirectory(): File = Files.createTempDirectory("diplay-log").toFile()

    private fun fullLog(directory: File, fill: String): File {
        val file = File(directory, "diplay.log")
        file.parentFile?.mkdirs()
        file.appendText(fill.repeat((SessionLogFile.MAX_BYTES / fill.length).toInt() + 1))
        return file
    }

    @Test
    fun `rotation shifts the archive chain and drops the oldest`() {
        val directory = freshDirectory()
        val file = fullLog(directory, "A")
        File(directory, "previous.log").writeText("one")
        File(directory, "previous-6.log").writeText("six")
        File(directory, "previous-7.log").writeText("seven")

        SessionLogFile(file).append("fresh line")

        assertEquals("six", File(directory, "previous-7.log").readText())
        assertEquals("one", File(directory, "previous-2.log").readText())
        assertTrue(File(directory, "previous.log").readText().startsWith("AAAA"))
        assertTrue(file.readText().contains("fresh line"))
    }

    @Test
    fun `the line that reached the cap is written after the rotation`() {
        val directory = freshDirectory()
        val file = fullLog(directory, "B")

        SessionLogFile(file).append("survivor")

        assertTrue(file.readText().contains("survivor"))
        assertTrue(File(directory, "previous.log").readText().startsWith("BBBB"))
    }

    @Test
    fun `a short log does not rotate`() {
        val directory = freshDirectory()
        val file = File(directory, "diplay.log")
        SessionLogFile(file).append("only")
        assertFalse(File(directory, "previous.log").exists())
        assertEquals("only\n", file.readText())
    }

    @Test
    fun `a closed log ignores further lines`() {
        val directory = freshDirectory()
        val file = File(directory, "diplay.log")
        val log = SessionLogFile(file)
        log.append("before")
        log.close()
        log.append("after")
        assertEquals("before\n", file.readText())
    }
}
