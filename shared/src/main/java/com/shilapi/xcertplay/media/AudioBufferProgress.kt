package com.shilapi.xcertplay.media

/** Tracks the unsigned AudioTrack head across wrap without flushing already queued sound. */
internal class AudioBufferProgress(private val frameBytes: Int) {
    private var writtenBytes = 0L
    private var playedFrames = 0L
    private var lastHead = 0L

    fun written(bytes: Int) { writtenBytes += bytes }

    /**
     * Called when the renderer builds a fresh AudioTrack. A rebuilt track restarts its playback head
     * at 0, so keeping the old head and totals would make the next accounting jump by a wrap-sized
     * amount: [queuedBytes] would then stay clamped at 0 for the rest of the session and every
     * underrun after the rebuild would look like an empty hardware buffer.
     */
    fun reset() {
        writtenBytes = 0L
        playedFrames = 0L
        lastHead = 0L
    }

    fun queuedBytes(rawHead: Int): Long {
        val head = rawHead.toLong() and 0xffff_ffffL
        playedFrames += (head - lastHead) and 0xffff_ffffL
        lastHead = head
        return (writtenBytes - playedFrames * frameBytes).coerceAtLeast(0)
    }

    fun shouldRebuffer(isMedia: Boolean, playing: Boolean, underrunSinceStart: Boolean,
        compressedQueueEmpty: Boolean, rawHead: Int): Boolean =
        isMedia && playing && underrunSinceStart && compressedQueueEmpty && queuedBytes(rawHead) == 0L
}
