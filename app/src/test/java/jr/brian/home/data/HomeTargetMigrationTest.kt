package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.model.HomeTarget
import jr.brian.home.model.MainScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Guards the pre-selector → target-selector migration contract on
 * [HomeButtonManager.resolveHomeTarget].
 *
 * The pre-change `routeHome()` fired MainActivity on the external display and
 * — when the frontend was enabled — FrontEndActivity on the primary display.
 * That behaviour maps onto the new model as:
 *
 *  - `frontendEnabled == true` → [HomeTarget.BOTH] (with [MainScreen.BOTTOM]
 *    so MainActivity stays on the bottom, matching the old dual-launch)
 *  - `frontendEnabled == false` → [HomeTarget.BOTTOM] (matches the old single
 *    MainActivity launch on the bottom)
 *
 * The migration must not fire when the user has already picked a target via
 * Settings — otherwise flipping the frontend toggle would silently overwrite
 * their explicit choice.
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
    fun `raw homeTarget is null when the user has never picked one`() {
        manager = HomeButtonManager(context)
        assertNull(manager.homeTarget.value)
    }

    @Test
    fun `mainScreen defaults to BOTTOM when nothing is persisted`() {
        manager = HomeButtonManager(context)
        assertEquals(MainScreen.BOTTOM, manager.mainScreen.value)
    }

    @Test
    fun `migration default with frontend enabled is BOTH`() {
        manager = HomeButtonManager(context)
        assertEquals(
            HomeTarget.BOTH,
            manager.resolveHomeTarget(frontendEnabled = true)
        )
    }

    @Test
    fun `migration default with frontend disabled is BOTTOM`() {
        manager = HomeButtonManager(context)
        assertEquals(
            HomeTarget.BOTTOM,
            manager.resolveHomeTarget(frontendEnabled = false)
        )
    }

    @Test
    fun `explicit user selection overrides the migration default`() {
        storage["home_target"] = HomeTarget.TOP.name
        manager = HomeButtonManager(context)

        assertEquals(HomeTarget.TOP, manager.resolveHomeTarget(frontendEnabled = true))
        assertEquals(HomeTarget.TOP, manager.resolveHomeTarget(frontendEnabled = false))
    }

    @Test
    fun `explicit BOTH is coerced to BOTTOM when the frontend is disabled`() {
        // Guards the case where the user picks BOTH, then later disables the
        // frontend — the second activity in BOTH is FrontEndActivity, so BOTH
        // becomes non-viable and must collapse rather than crash on launch.
        storage["home_target"] = HomeTarget.BOTH.name
        manager = HomeButtonManager(context)

        assertEquals(HomeTarget.BOTH, manager.resolveHomeTarget(frontendEnabled = true))
        assertEquals(HomeTarget.BOTTOM, manager.resolveHomeTarget(frontendEnabled = false))
    }

    @Test
    fun `setHomeTarget persists the enum name`() {
        manager = HomeButtonManager(context)

        manager.setHomeTarget(HomeTarget.TOP)

        assertEquals(HomeTarget.TOP, manager.homeTarget.value)
        assertEquals("TOP", storage["home_target"])
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
    fun `unknown stored target name falls back to migration default`() {
        // Guards against a garbage / renamed enum value in shared prefs
        // (e.g. downgrade → upgrade with a bad hand-edit) leaking into the
        // routing plan as null-crashy state.
        storage["home_target"] = "SOMETHING_UNKNOWN"

        manager = HomeButtonManager(context)

        assertNull(manager.homeTarget.value)
        assertEquals(HomeTarget.BOTH, manager.resolveHomeTarget(frontendEnabled = true))
        assertEquals(HomeTarget.BOTTOM, manager.resolveHomeTarget(frontendEnabled = false))
    }
}
