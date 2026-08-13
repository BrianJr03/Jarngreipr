package jr.brian.home.esde.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The per-system SAF tree store is a single JSON blob keyed by system name.
 * These tests lock the two invariants that make it usable as a picker:
 * different systems' trees don't collide, and clearing one doesn't touch the
 * others. Robolectric supplies a real SharedPreferences under the hood.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class EsdePrefsSafTreeTest {

    private fun newPrefs(): ESDEPreferencesManager {
        val app = RuntimeEnvironment.getApplication()
        // Wipe any prefs left behind by a previous test in the same JVM.
        app.getSharedPreferences("esde_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        return ESDEPreferencesManager(app)
    }

    @Test
    fun `getSafTreeUri returns what setSafTreeUri wrote`() {
        val prefs = newPrefs()
        prefs.setSafTreeUri("ps2", "content://x/tree/primary%3ARoms%2Fps2")
        assertEquals(
            "content://x/tree/primary%3ARoms%2Fps2",
            prefs.getSafTreeUri("ps2"),
        )
    }

    @Test
    fun `two systems' trees are stored independently`() {
        val prefs = newPrefs()
        prefs.setSafTreeUri("ps2", "content://x/tree/primary%3ARoms%2Fps2")
        prefs.setSafTreeUri("psx", "content://x/tree/primary%3ARoms%2Fpsx")

        assertEquals(
            "content://x/tree/primary%3ARoms%2Fps2",
            prefs.getSafTreeUri("ps2"),
        )
        assertEquals(
            "content://x/tree/primary%3ARoms%2Fpsx",
            prefs.getSafTreeUri("psx"),
        )
    }

    @Test
    fun `clearing one system's tree does not disturb another's`() {
        val prefs = newPrefs()
        prefs.setSafTreeUri("ps2", "content://x/tree/primary%3ARoms%2Fps2")
        prefs.setSafTreeUri("psx", "content://x/tree/primary%3ARoms%2Fpsx")

        prefs.clearSafTreeUri("ps2")

        assertNull(prefs.getSafTreeUri("ps2"))
        assertNotNull(
            "clearing ps2 must leave psx intact",
            prefs.getSafTreeUri("psx"),
        )
    }

    @Test
    fun `clearing a system with no tree is a no-op`() {
        val prefs = newPrefs()
        prefs.setSafTreeUri("psx", "content://x/tree/primary%3ARoms%2Fpsx")

        prefs.clearSafTreeUri("ps2")

        assertEquals(
            "content://x/tree/primary%3ARoms%2Fpsx",
            prefs.getSafTreeUri("psx"),
        )
    }
}
