package jr.brian.frontend.scrape

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
 * Pins the Phase 4a credential surface: writes persist, the state
 * flow mirrors, and the "has credentials" booleans only flip true
 * when BOTH fields of a pair are present.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UserScraperCredentialsTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("frontend_scraper_user_creds", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `default state is empty on fresh install`() {
        val prefs = UserScraperCredentials(app)
        val s = prefs.state.value
        assertEquals("", s.ssUserId)
        assertEquals("", s.ssUserPassword)
        assertEquals("", s.steamGridDbApiKey)
        assertFalse(s.hasScreenScraperUser)
        assertFalse(s.hasSteamGridDbKey)
    }

    @Test
    fun `setScreenScraperUser persists and flips hasScreenScraperUser`() {
        val prefs = UserScraperCredentials(app)
        prefs.setScreenScraperUser("alice", "s3cret")

        val s = prefs.state.value
        assertEquals("alice", s.ssUserId)
        assertEquals("s3cret", s.ssUserPassword)
        assertTrue(s.hasScreenScraperUser)

        // Fresh instance (next process boot) sees the same values.
        val fresh = UserScraperCredentials(app)
        assertEquals("alice", fresh.state.value.ssUserId)
    }

    @Test
    fun `setSteamGridDbKey persists and flips hasSteamGridDbKey`() {
        val prefs = UserScraperCredentials(app)
        prefs.setSteamGridDbKey("sgdb-key-123")
        assertTrue(prefs.state.value.hasSteamGridDbKey)
    }

    @Test
    fun `clearScreenScraperUser wipes both parts of the pair`() {
        val prefs = UserScraperCredentials(app)
        prefs.setScreenScraperUser("alice", "s3cret")
        prefs.clearScreenScraperUser()
        assertFalse(prefs.state.value.hasScreenScraperUser)
    }

    @Test
    fun `has flags require BOTH fields of their pair`() {
        val prefs = UserScraperCredentials(app)
        prefs.setScreenScraperUser("alice", "")
        assertFalse(
            "A set user with no password must not count as configured",
            prefs.state.value.hasScreenScraperUser,
        )
    }
}
