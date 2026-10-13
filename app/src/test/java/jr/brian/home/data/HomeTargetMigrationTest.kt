package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.model.MainScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Guards persistence of the two remaining [HomeButtonManager] preferences —
 * `interceptionEnabled` and `mainScreen`. The former gates the whole feature;
 * the latter governs which display `MainActivity` occupies when a long-press
 * Home gesture routes to `BOTH`.
 *
 * The old target-selector preference and its migration default were removed
 * when gesture-based routing landed; this file replaces the former
 * `HomeTargetMigrationTest` with coverage of what remains.
 */
class HomeTargetMigrationTest {
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
        every { editor.putString(any(), any()) } answers {
            val value = secondArg<String?>()
            if (value == null) storage.remove(firstArg()) else storage[firstArg()] = value
            editor
        }
        every { editor.apply() } returns Unit
        every { prefs.getBoolean(any(), any()) } answers {
            storage[firstArg()] as? Boolean ?: secondArg()
        }
        every { prefs.getString(any(), any()) } answers {
            storage[firstArg()] as? String ?: secondArg()
        }
    }

    @Test
    fun `interceptionEnabled defaults to false when nothing is persisted`() {
        manager = HomeButtonManager(context)
        assert(!manager.interceptionEnabled.value)
    }

    @Test
    fun `setInterceptionEnabled persists the boolean`() {
        manager = HomeButtonManager(context)

        manager.setInterceptionEnabled(true)

        assertEquals(true, manager.interceptionEnabled.value)
        assertEquals(true, storage["home_interception_enabled"])
    }

    @Test
    fun `mainScreen defaults to BOTTOM when nothing is persisted`() {
        manager = HomeButtonManager(context)
        assertEquals(MainScreen.BOTTOM, manager.mainScreen.value)
    }

    @Test
    fun `setMainScreen persists the enum name`() {
        manager = HomeButtonManager(context)

        manager.setMainScreen(MainScreen.TOP)

        assertEquals(MainScreen.TOP, manager.mainScreen.value)
        assertEquals("TOP", storage["home_main_screen"])
    }

    @Test
    fun `stored mainScreen is loaded on construction`() {
        storage["home_main_screen"] = MainScreen.TOP.name

        manager = HomeButtonManager(context)

        assertEquals(MainScreen.TOP, manager.mainScreen.value)
    }

    @Test
    fun `unknown stored mainScreen falls back to BOTTOM`() {
        storage["home_main_screen"] = "SOMETHING_UNKNOWN"

        manager = HomeButtonManager(context)

        assertEquals(MainScreen.BOTTOM, manager.mainScreen.value)
    }
}
