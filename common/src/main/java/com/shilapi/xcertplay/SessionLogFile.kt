package com.shilapi.xcertplay

import java.io.Closeable
import java.io.File

/** Bounded, private diagnostics. Each write is redacted before touching storage. */
internal class SessionLogFile(val file: File) : Closeable {
    /**
     * Periodic stats lines are only written while this is on. Audio, video and receive stats arrive
     * every few seconds per stream and matter only while investigating, so an ordinary session keeps
     * to state changes, warnings and errors. Toggled in Settings - Diagnostics.
     */
    var verbose: Boolean = false

    private val lock = Any()
    private var closed = false
    fun reset(header: String) = synchronized(lock) {
        if (!closed) {
            file.parentFile?.mkdirs()
            rotate()
            file.writeText("")
            append(header)
        }
    }
    fun append(line: String) = synchronized(lock) {
        if (closed) return@synchronized
        if (!verbose && isPeriodicStats(line)) return@synchronized
        val safe = DiagnosticRedactor.redact(line) ?: return@synchronized
        runCatching {
            if (file.length() > MAX_BYTES) {
                rotate()
                file.writeText("")
            }
            file.appendText(safe + "\n")
        }
        Unit
    }
    /**
     * Renames the archives inside their own directory instead of copying them. The previous version
     * copied up to 3.5 MB while holding the write lock, and the line that triggers a rotation can be
     * an audio stats line, which arrives on the playback thread.
     */
    private fun rotate() {
        if (!file.exists() || file.length() == 0L) return
        File(file.parentFile, ARCHIVE_NAMES.last()).delete()
        for (index in ARCHIVE_NAMES.lastIndex downTo 1) {
            val source = File(file.parentFile, ARCHIVE_NAMES[index - 1])
            if (!source.exists()) continue
            val destination = File(file.parentFile, ARCHIVE_NAMES[index])
            if (!source.renameTo(destination)) source.copyTo(destination, overwrite = true)
        }
        val oldest = File(file.parentFile, ARCHIVE_NAMES.first())
        if (!file.renameTo(oldest)) file.copyTo(oldest, overwrite = true)
    }
    override fun close() = synchronized(lock) { closed = true }
    companion object {
        const val MAX_BYTES = 512 * 1024L
        private val ARCHIVE_NAMES = listOf("previous.log") + (2..7).map { "previous-$it.log" }
        val REPORT_NAMES = ARCHIVE_NAMES.reversed() + "diplay.log"

        /** The prefix the host adds before handing a line over; see formattedLogLine. */
        private val TIMESTAMP = Regex("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+")

        /** Lines the stream builders emit every few seconds, with or without that prefix. */
        internal fun isPeriodicStats(line: String): Boolean {
            val body = line.replaceFirst(TIMESTAMP, "")
            return body.startsWith("audio stats") ||
                body.startsWith("video stats") ||
                body.startsWith("Video: video stats") ||
                body.startsWith("Receive:")
        }
    }
}
