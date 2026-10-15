package jr.brian.home.data

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Guards the Phase 3 opt-in toggle. Default must be true so an
 * upgrade keeps users on the legacy frontend; subsequent writes
 * persist and the StateFlow reflects them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LegacyFrontendPrefsTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("legacy_frontend_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `default is true so upgrades keep the old frontend`() {
        val prefs = LegacyFrontendPrefs(app)
        assertTrue(prefs.legacyFrontendEnabled.value)
    }

    @Test
    fun `setting to false flips the StateFlow and persists`() {
        val prefs = LegacyFrontendPrefs(app)
        prefs.setLegacyFrontendEnabled(false)
        assertFalse(prefs.legacyFrontendEnabled.value)

        // A fresh instance (next process boot) should still see false.
        val fresh = LegacyFrontendPrefs(app)
        assertEquals(false, fresh.legacyFrontendEnabled.value)
    }

    @Test
    fun `toggling back to true is observable in the StateFlow`() {
        val prefs = LegacyFrontendPrefs(app)
        prefs.setLegacyFrontendEnabled(false)
        prefs.setLegacyFrontendEnabled(true)
        assertTrue(prefs.legacyFrontendEnabled.value)
    }
}
