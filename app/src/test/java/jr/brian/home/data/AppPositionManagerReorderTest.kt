package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.model.app.AppPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPositionManagerReorderTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: AppPositionManager
    private val store = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.apply() } answers { }
        every { editor.commit() } returns true
        every { editor.putString(any(), any()) } answers {
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
        every { prefs.getBoolean(any(), any()) } answers {
            store[firstArg()] as? Boolean ?: secondArg()
        }

        manager = AppPositionManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        store.clear()
    }

    private fun pos(pkg: String, x: Float) = AppPosition(pkg, x, x, 64f)

    @Test
    fun `reorderPages remaps positions and flags to new indices`() = runTest {
        manager.savePosition(0, pos("com.a", 10f))
        manager.savePosition(1, pos("com.b", 20f))
        manager.savePosition(2, pos("com.c", 30f))
        manager.setFreeMode(0, true)
        manager.setFreeMode(2, true)

        manager.reorderPages(mapOf(0 to 2, 1 to 0, 2 to 1))

        assertEquals("com.c", manager.getPosition(0, "com.c")?.packageName)
        assertEquals("com.a", manager.getPosition(1, "com.a")?.packageName)
        assertEquals("com.b", manager.getPosition(2, "com.b")?.packageName)

        val freeMode = manager.isFreeModeByPage.value
        assertEquals(true, freeMode[0]) // was old 2
        assertEquals(true, freeMode[1]) // was old 0
        // new index 2 (came from old 1, which had no explicit free mode) — drops out
        assertFalse(freeMode[2] ?: false)
    }

    @Test
    fun `reorderPages clears prefs for old keys not in the new map`() = runTest {
        manager.savePosition(0, pos("com.a", 1f))
        manager.savePosition(1, pos("com.b", 2f))
        manager.savePosition(2, pos("com.c", 3f))

        manager.reorderPages(mapOf(0 to 1))

        assertNull(store["positions_2"])
        assertTrue((store["positions_0"] as String).contains("com.b"))
        assertNull(store["positions_1"])
    }

    @Test
    fun `removePage shifts later positions down`() = runTest {
        manager.savePosition(0, pos("com.a", 1f))
        manager.savePosition(1, pos("com.b", 2f))
        manager.savePosition(2, pos("com.c", 3f))
        manager.setFreeMode(2, true)

        manager.removePage(1)

        assertEquals("com.a", manager.getPosition(0, "com.a")?.packageName)
        assertEquals("com.c", manager.getPosition(1, "com.c")?.packageName)
        assertNull(manager.getPosition(2, "com.c"))
        assertEquals(true, manager.isFreeModeByPage.value[1])
        assertFalse(manager.isFreeModeByPage.value.containsKey(2))
    }
}
