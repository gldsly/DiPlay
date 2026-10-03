package com.shilapi.xcertplay

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * The settings panel that the projection host opens over CarPlay is left for CarPlay, and a session
 * ended from that panel sends Back to this app's home page instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@LooperMode(LooperMode.Mode.PAUSED)
class SettingsPanelReturnTest {
    private fun openPanel(page: String, fromProjection: Boolean): DiPlayActivity {
        val intent = Intent(RuntimeEnvironment.getApplication(), DiPlayActivity::class.java)
            .putExtra("page", page)
            .putExtra(DiPlayActivity.EXTRA_FROM_PROJECTION, fromProjection)
        return Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup().get()
    }

    private fun DiPlayActivity.readField(name: String): Any? =
        DiPlayActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.get(this)

    private fun DiPlayActivity.call(name: String): Any? =
        DiPlayActivity::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(this)

    @Test
    fun `the panel opened over CarPlay is left for CarPlay`() {
        val activity = openPanel("settings", fromProjection = true)

        assertEquals("CarPlay", activity.call("headerActionLabel"))
        activity.call("leavePage")

        assertTrue(activity.isFinishing)
        assertEquals("settings", activity.readField("page"))
    }

    @Test
    fun `the panel opened from the app home still leaves for the home page`() {
        val activity = openPanel("settings", fromProjection = false)

        activity.call("leavePage")

        assertFalse(activity.isFinishing)
        assertEquals("home", activity.readField("page"))
    }

    @Test
    fun `disconnecting from the panel sends Back to the home page`() {
        val activity = openPanel("settings", fromProjection = true)

        activity.call("disconnectFromPanel")
        activity.call("leavePage")

        assertFalse(activity.isFinishing)
        assertEquals("home", activity.readField("page"))
        assertEquals(false, activity.readField("returnToProjection"))
    }

    @Test
    fun `a sub-page inside the panel keeps the CarPlay return`() {
        val activity = openPanel("settings", fromProjection = true)
        DiPlayActivity::class.java.getDeclaredField("page").apply { isAccessible = true }
            .set(activity, "connection")

        assertEquals("CarPlay", activity.call("headerActionLabel"))
    }
}
