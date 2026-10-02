package com.shilapi.xcertplay

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class DiagnosticLevelPersistenceTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)

    @Before fun clearPreferences() {
        prefs.edit().clear().apply()
    }

    @Test fun aFreshInstallLogsAtTheNormalLevel() {
        assertEquals(AirPlayPersistence.DIAGNOSTIC_LEVEL_NORMAL, AirPlayPersistence.loadDiagnosticLevel(context))
    }

    @Test fun theChosenLevelSurvivesARestart() {
        AirPlayPersistence.saveDiagnosticLevel(context, AirPlayPersistence.DIAGNOSTIC_LEVEL_DEBUG)
        assertEquals(AirPlayPersistence.DIAGNOSTIC_LEVEL_DEBUG, AirPlayPersistence.loadDiagnosticLevel(context))
        AirPlayPersistence.saveDiagnosticLevel(context, AirPlayPersistence.DIAGNOSTIC_LEVEL_NORMAL)
        assertEquals(AirPlayPersistence.DIAGNOSTIC_LEVEL_NORMAL, AirPlayPersistence.loadDiagnosticLevel(context))
    }

    @Test fun anUnknownStoredLevelFallsBackToNormal() {
        prefs.edit().putInt("diagnostic_level", 7).apply()
        assertEquals(AirPlayPersistence.DIAGNOSTIC_LEVEL_NORMAL, AirPlayPersistence.loadDiagnosticLevel(context))
    }

    @Test fun anOutOfRangeValueIsStoredAsTheNormalLevel() {
        AirPlayPersistence.saveDiagnosticLevel(context, -3)
        assertEquals(AirPlayPersistence.DIAGNOSTIC_LEVEL_NORMAL, AirPlayPersistence.loadDiagnosticLevel(context))
    }

    @Test fun theReplacedBooleanCarriesOver() {
        prefs.edit().putBoolean("verbose_diagnostics", true).apply()
        assertEquals(AirPlayPersistence.DIAGNOSTIC_LEVEL_DEBUG, AirPlayPersistence.loadDiagnosticLevel(context))
    }

    @Test fun anExplicitLevelWinsOverTheReplacedBoolean() {
        prefs.edit().putBoolean("verbose_diagnostics", true).putInt("diagnostic_level", 0).apply()
        assertEquals(AirPlayPersistence.DIAGNOSTIC_LEVEL_NORMAL, AirPlayPersistence.loadDiagnosticLevel(context))
    }
}
