package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppVisibilityManagerReorderTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: AppVisibilityManager
    private val prefsData = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        editor = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { context.getSharedPreferences("app_visibility_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.apply() } answers { }
        every { editor.putString(any(), any()) } answers {
            prefsData[firstArg()] = secondArg(); editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            prefsData[firstArg()] = secondArg(); editor
        }
        every { editor.putInt(any(), any()) } answers {
            prefsData[firstArg()] = secondArg(); editor
        }
        every { editor.remove(any()) } answers {
            prefsData.remove(firstArg<String>()); editor
        }
        every { prefs.getString(any(), any()) } answers {
            prefsData[firstArg()] as? String ?: secondArg()
        }
        every { prefs.getBoolean(any(), any()) } answers {
            prefsData[firstArg()] as? Boolean ?: secondArg()
        }
        every { prefs.getInt(any(), any()) } answers {
            prefsData[firstArg()] as? Int ?: secondArg()
        }

        manager = AppVisibilityManager(context)
    }

    @Test
    fun `reorderPages remaps hidden apps to new indices`() = runTest {
        manager.hideApp(0, "com.app0")
        manager.hideApp(1, "com.app1")
        manager.hideApp(2, "com.app2")

        // newPosition -> oldPosition: 0<-2, 1<-0, 2<-1
        manager.reorderPages(mapOf(0 to 2, 1 to 0, 2 to 1))

        assertEquals(setOf("com.app2"), manager.getHiddenApps(0))
        assertEquals(setOf("com.app0"), manager.getHiddenApps(1))
        assertEquals(setOf("com.app1"), manager.getHiddenApps(2))
    }

    @Test
    fun `reorderPages drops pages not in map`() = runTest {
        manager.hideApp(0, "com.app0")
        manager.hideApp(1, "com.app1")
        manager.hideApp(2, "com.app2")

        // Only keep new index 0 (which gets old 1)
        manager.reorderPages(mapOf(0 to 1))

        assertEquals(setOf("com.app1"), manager.getHiddenApps(0))
        assertEquals(emptySet<String>(), manager.getHiddenApps(1))
        assertEquals(emptySet<String>(), manager.getHiddenApps(2))
        assertNull(prefsData["hidden_apps_1"])
        assertNull(prefsData["hidden_apps_2"])
    }

    @Test
    fun `removePage shifts later indices down by one`() = runTest {
        manager.hideApp(0, "com.app0")
        manager.hideApp(1, "com.app1")
        manager.hideApp(2, "com.app2")

        manager.removePage(1)

        assertEquals(setOf("com.app0"), manager.getHiddenApps(0))
        assertEquals(setOf("com.app2"), manager.getHiddenApps(1))
        assertEquals(emptySet<String>(), manager.getHiddenApps(2))
    }

    @Test
    fun `removePage removes only the targeted page when last`() = runTest {
        manager.hideApp(0, "com.app0")
        manager.hideApp(2, "com.app2")

        manager.removePage(2)

        assertEquals(setOf("com.app0"), manager.getHiddenApps(0))
        assertFalse(manager.hiddenAppsByPage.value.containsKey(2))
    }

    @Test
    fun `removePage on empty manager is a no-op`() = runTest {
        manager.removePage(0)
        assertEquals(emptyMap<Int, Set<String>>(), manager.hiddenAppsByPage.value)
    }
}
