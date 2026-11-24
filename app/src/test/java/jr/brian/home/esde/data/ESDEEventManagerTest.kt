package jr.brian.home.esde.data

import android.app.Application
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the companion-mode gate on [ESDEEventManager.startPolling] added in
 * Phase 0. Running the 500ms poll loop at all is only worthwhile when the
 * user has configured external ES-DE — otherwise it's a wake-up every
 * half-second for the life of the launcher reading files that never exist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ESDEEventManagerTest {

    private lateinit var app: Application
    private lateinit var setupPreferences: SetupPreferences
    private lateinit var listener: ESDEEventListenerImpl
    private lateinit var manager: ESDEEventManager

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("SetupPrefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        setupPreferences = SetupPreferences(app)
        listener = ESDEEventListenerImpl()
        manager = ESDEEventManager(listener, setupPreferences)
    }

    @After
    fun tearDown() {
        manager.stopWatching()
    }

    @Test
    fun `startPolling is a no-op when scriptsPath is blank`() {
        // SetupPreferences.scriptsPath defaults to empty — nothing configured
        setupPreferences.scriptsPath = ""

        manager.startPolling()

        assertFalse(
            "startPolling must not start the thread when ES-DE is not configured",
            manager.isPollingForTest(),
        )
    }

    @Test
    fun `startPolling starts the background thread when scriptsPath is set`() {
        setupPreferences.scriptsPath = "/storage/emulated/0/ES-DE/scripts"

        manager.startPolling()

        assertTrue(
            "startPolling must start the thread when ES-DE is configured",
            manager.isPollingForTest(),
        )
    }

    @Test
    fun `stopPolling releases the thread and resets the flag`() {
        setupPreferences.scriptsPath = "/storage/emulated/0/ES-DE/scripts"
        manager.startPolling()
        assertTrue(manager.isPollingForTest())

        manager.stopPolling()

        assertFalse(manager.isPollingForTest())
    }
}
