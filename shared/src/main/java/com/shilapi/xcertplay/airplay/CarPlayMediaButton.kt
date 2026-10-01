package com.shilapi.xcertplay.airplay

import android.view.KeyEvent

/**
 * Hardware media keys → CarPlay media HID presses (indices into [AirPlayHid]'s media report).
 *
 * Hardware play and pause keys both map to the toggle: BYD picks PLAY or PAUSE from its own idea of
 * the play state, and a wrong guess would make the button do nothing. Explicit play and pause
 * commands from media controllers use [PLAY] and [PAUSE].
 */
object CarPlayMediaButton {
    const val PLAY = 1
    const val PAUSE = 2
    const val PLAY_PAUSE = 3
    const val NEXT = 4
    const val PREVIOUS = 5

    /** BYD's steering-wheel play/pause key; the firmware normally rewrites it to MEDIA_PLAY/PAUSE. */
    const val KEYCODE_BYD_AUTO_MEDIA_PLAY_PAUSE = 353

    /** BYD's steering-wheel voice key: a short press, and the code the wheel sends for a long press. */
    const val KEYCODE_BYD_AUTO_MEDIA_VOICE = 304
    const val KEYCODE_BYD_AUTO_MEDIA_VOICE_LONG = 312

    /**
     * Whether [keyCode] is a voice key that opens Siri. The BYD wheel sends each press as an
     * instant down/up pair, so a long press arrives as its own key rather than as a held one.
     */
    fun opensSiri(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_VOICE_ASSIST ||
        keyCode == KEYCODE_BYD_AUTO_MEDIA_VOICE || keyCode == KEYCODE_BYD_AUTO_MEDIA_VOICE_LONG

    /** The CarPlay press for [keyCode], or null when the key is not a media key CarPlay handles. */
    fun forKeyCode(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_NEXT -> NEXT
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> PREVIOUS
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_HEADSETHOOK,
        KEYCODE_BYD_AUTO_MEDIA_PLAY_PAUSE -> PLAY_PAUSE
        else -> null
    }

    /**
     * Usage index 1 in [AirPlayHid]'s telephony report: the Hook Switch, which ends a call the
     * iPhone is running through CarPlay.
     */
    const val TELEPHONY_HOOK_SWITCH = 1

    /**
     * The telephony HID press for [keyCode], or null when the key is not a call key.
     *
     * Only the end-call key is claimed. The answer key already works through the car's own phone
     * stack on the head units seen so far, so taking it over would risk breaking it.
     */
    fun telephonyForKeyCode(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_ENDCALL -> TELEPHONY_HOOK_SWITCH
        else -> null
    }

    /**
     * Keys worth naming in the log while diagnosing steering-wheel call buttons: the standard call
     * keys plus every BYD wheel code, so a press that does nothing still shows up.
     */
    fun describeKey(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_CALL,
        KeyEvent.KEYCODE_ENDCALL,
        KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_VOICE_ASSIST,
        KEYCODE_BYD_AUTO_MEDIA_VOICE,
        KEYCODE_BYD_AUTO_MEDIA_VOICE_LONG,
        KEYCODE_BYD_AUTO_MEDIA_PLAY_PAUSE -> KeyEvent.keyCodeToString(keyCode)
        else -> null
    }
}
