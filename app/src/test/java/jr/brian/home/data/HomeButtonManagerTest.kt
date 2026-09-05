package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Guards the off-by-default contract and StateFlow persistence for
 * [HomeButtonManager] — the persisted gate the accessibility service consults
 * on every hardware Home press.
 *
 * Mirrors the mockk/SharedPreferences pattern used by the other data-layer
 * tests in this package (see GridSettingsManagerTest).
 */
class HomeButtonManagerTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: HomeButtonManager

    private val storage = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        storage.clear()

        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } answers {
            storage[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { editor.apply() } returns Unit
        every { prefs.getBoolean(any(), any()) } answers {
            storage[firstArg()] as? Boolean ?: secondArg()
        }
    }

    @Test
    fun `interception defaults to off when no value has ever been persisted`() {
        manager = HomeButtonManager(context)

        assertFalse(manager.interceptionEnabled.value)
        assertFalse(HomeButtonManager.DEFAULT_INTERCEPTION_ENABLED)
    }

    @Test
    fun `setInterceptionEnabled true updates state and persists`() {
        manager = HomeButtonManager(context)

        manager.setInterceptionEnabled(true)

        assertTrue(manager.interceptionEnabled.value)
        verify { editor.putBoolean("home_interception_enabled", true) }
        verify { editor.apply() }
    }

    @Test
    fun `setInterceptionEnabled false updates state and persists`() {
        storage["home_interception_enabled"] = true
        manager = HomeButtonManager(context)
        assertTrue(manager.interceptionEnabled.value)

        manager.setInterceptionEnabled(false)

        assertFalse(manager.interceptionEnabled.value)
        verify { editor.putBoolean("home_interception_enabled", false) }
    }

    @Test
    fun `initial state loads persisted true`() {
        storage["home_interception_enabled"] = true

        manager = HomeButtonManager(context)

        assertTrue(manager.interceptionEnabled.value)
    }

    @Test
    fun `uses its own preferences file`() {
        HomeButtonManager(context)

        verify { context.getSharedPreferences("home_button_prefs", Context.MODE_PRIVATE) }
    }
}
