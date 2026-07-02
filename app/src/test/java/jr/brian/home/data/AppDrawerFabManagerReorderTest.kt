package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppDrawerFabManagerReorderTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: AppDrawerFabManager
    private val store = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("app_drawer_fab_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.apply() } answers { }
        every { editor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.putInt(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg<String>()); editor
        }
        every { prefs.getString(any(), any()) } answers {
            store[firstArg()] as? String ?: secondArg()
        }
        every { prefs.getInt(any(), any()) } answers {
            store[firstArg()] as? Int ?: secondArg()
        }
        every { prefs.getBoolean(any(), any()) } answers {
            store[firstArg()] as? Boolean ?: secondArg()
        }

        manager = AppDrawerFabManager(context)
    }

    @Test
    fun `reorderPages does nothing when not in explicit mode`() {
        manager.setFabExplicitPages(false)
        manager.setFabVisiblePages(setOf(0, 1, 2))
        manager.reorderPages(mapOf(0 to 2, 1 to 0, 2 to 1))
        assertEquals(setOf(0, 1, 2), manager.fabVisiblePages.value)
    }

    @Test
    fun `reorderPages remaps fab visible pages in explicit mode`() {
        manager.setFabExplicitPages(true)
        manager.setFabVisiblePages(setOf(0, 2))
        manager.reorderPages(mapOf(0 to 2, 1 to 0, 2 to 1))
        // old 0 -> new 1, old 2 -> new 0
        assertEquals(setOf(0, 1), manager.fabVisiblePages.value)
    }

    @Test
    fun `removePage shifts later indices down in explicit mode`() {
        manager.setFabExplicitPages(true)
        manager.setFabVisiblePages(setOf(0, 1, 3))
        manager.removePage(1)
        assertEquals(setOf(0, 2), manager.fabVisiblePages.value)
    }

    @Test
    fun `removePage does nothing when not in explicit mode`() {
        manager.setFabExplicitPages(false)
        manager.setFabVisiblePages(setOf(0, 1, 3))
        manager.removePage(1)
        assertEquals(setOf(0, 1, 3), manager.fabVisiblePages.value)
    }
}
