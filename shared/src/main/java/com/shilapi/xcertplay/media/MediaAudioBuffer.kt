package com.shilapi.xcertplay.media

/**
 * Jitter buffer for streams mapped to the media channel. Wireless CarPlay delivers audio over the same Wi-Fi link as
 * video; radio gaps of several hundred milliseconds are normal, so music needs a buffer that
 * outlasts them. Calls, Siri and navigation prompts keep the small low-latency buffer.
 */
object MediaAudioBuffer {
    const val DEFAULT_MILLIS = 300

    /** Stored choice that sizes the prebuffer from the arrival gaps this link actually shows. */
    const val AUTO_MILLIS = 0

    /** Fixed windows, lowest latency first. */
    val presets = listOf(100, 200, DEFAULT_MILLIS, 500, 1000)

    /** Everything the picker offers, in display order: the fixed windows, then auto. */
    val choices = presets + AUTO_MILLIS

    private const val HEADROOM_MILLIS = 200 // room above the start level so bursts after a gap fit
    private const val MIN_TRACK_BUFFER_BYTES = 16 * 1024
    private const val MIN_START_BUFFER_BYTES = 4 * 1024

    /**
     * Auto mode: the track is allocated for the largest start window the measurement can pick, so the
     * threshold can move during playback without reallocating the AudioTrack.
     */
    const val AUTO_CAPACITY_MILLIS = 700
    const val AUTO_MIN_START_MILLIS = 100
    const val AUTO_MAX_START_MILLIS = 500

    /** Twice the worst recent gap, so a burst that already happened once still fits. */
    private const val AUTO_HEADROOM = 2

    /** Decay applied to the remembered worst gap, in percent, so one stall does not pin the delay. */
    const val AUTO_DECAY_PERCENT = 98L

    fun sanitize(millis: Int): Int = millis.takeIf { it in choices } ?: DEFAULT_MILLIS

    /** Track capacity to request. Auto reserves room for the largest window it may select. */
    fun capacityMillis(storedMillis: Int): Int =
        sanitize(storedMillis).let { if (it == AUTO_MILLIS) AUTO_CAPACITY_MILLIS else it }

    /** Start window auto mode picks for the worst gap seen recently. */
    fun autoStartMillis(worstGapMillis: Long): Int =
        (worstGapMillis * AUTO_HEADROOM)
            .coerceIn(AUTO_MIN_START_MILLIS.toLong(), AUTO_MAX_START_MILLIS.toLong())
            .toInt()

    fun bytesForMillis(sampleRate: Int, channels: Int, millis: Int): Int =
        (sampleRate.toLong() * channels.coerceIn(1, 2) * 2L * millis / 1000L).toInt()

    data class Plan(val trackBufferBytes: Int, val startBytes: Int)

    /**
     * AudioTrack capacity and the amount to queue before play() for one output stream.
     *
     * [mediaMillis] is the stored preference: [AUTO_MILLIS] allocates for the largest window auto mode
     * can select, and an unrecognised value falls back to the default window.
     */
    fun plan(isMedia: Boolean, sampleRate: Int, channels: Int, minBufferBytes: Int, mediaMillis: Int): Plan {
        val lowLatency = Plan(
            trackBufferBytes = maxOf(minBufferBytes * 4, MIN_TRACK_BUFFER_BYTES),
            startBytes = maxOf(minBufferBytes, MIN_START_BUFFER_BYTES),
        )
        if (!isMedia) return lowLatency
        val windowMillis = capacityMillis(mediaMillis)
        val bytesPerSecond = sampleRate.toLong() * channels.coerceIn(1, 2) * 2
        val start = (bytesPerSecond * windowMillis / 1000).toInt()
        val capacity = (bytesPerSecond * (windowMillis + HEADROOM_MILLIS) / 1000).toInt()
        return Plan(
            trackBufferBytes = maxOf(capacity, lowLatency.trackBufferBytes),
            startBytes = maxOf(start, lowLatency.startBytes),
        )
    }

    /**
     * The device may grant a smaller AudioTrack than requested. Writes block while the track is
     * paused and full, so the start level must stay below the real capacity or play() never runs.
     */
    fun startBytesFor(plannedStartBytes: Int, actualCapacityBytes: Int, writeChunkBytes: Int): Int {
        if (actualCapacityBytes <= 0) return plannedStartBytes
        return minOf(plannedStartBytes, actualCapacityBytes - writeChunkBytes).coerceAtLeast(writeChunkBytes)
    }
}
