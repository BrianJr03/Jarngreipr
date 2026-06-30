package jr.brian.home.canvas.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasScrollOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasLayoutManagerReorderTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: CanvasLayoutManager
    private val store = mutableMapOf<String, String?>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.apply() } returns Unit
        every { editor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg<String>()); editor
        }
        every { editor.clear() } answers {
            store.clear(); editor
        }
        every { prefs.getString(any(), any()) } answers {
            store[firstArg()] ?: secondArg()
        }
        every { prefs.all } answers { store.toMap() }

        manager = CanvasLayoutManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        store.clear()
    }

    @Test
    fun `reorderPages remaps layouts to new indices preserving items`() = runTest {
        manager.addItem(0, CanvasItem.AppItem(id = "a", packageName = "com.a"))
        manager.addItem(1, CanvasItem.AppItem(id = "b", packageName = "com.b"))
        manager.addItem(2, CanvasItem.AppItem(id = "c", packageName = "com.c"))

        manager.reorderPages(mapOf(0 to 2, 1 to 0, 2 to 1))

        assertEquals("a", manager.getLayout(1).items.single().id)
        assertEquals("b", manager.getLayout(2).items.single().id)
        assertEquals("c", manager.getLayout(0).items.single().id)
    }

    @Test
    fun `reorderPages preserves both vertical and horizontal arrangements`() = runTest {
        val itemId = "a"
        manager.addItem(0, CanvasItem.AppItem(id = itemId, packageName = "com.a"))
        val before = manager.getLayout(0)
        val verticalBefore = before.verticalArrangement[itemId]
        val horizontalBefore = before.horizontalArrangement[itemId]
        assertNotNull(verticalBefore)
        assertNotNull(horizontalBefore)

        manager.reorderPages(mapOf(2 to 0))

        val after = manager.getLayout(2)
        assertEquals(verticalBefore, after.verticalArrangement[itemId])
        assertEquals(horizontalBefore, after.horizontalArrangement[itemId])
    }

    @Test
    fun `reorderPages drops pages not in map and clears prefs`() = runTest {
        manager.addItem(0, CanvasItem.AppItem(id = "a", packageName = "com.a"))
        manager.addItem(1, CanvasItem.AppItem(id = "b", packageName = "com.b"))

        manager.reorderPages(mapOf(0 to 1))

        assertEquals("b", manager.getLayout(0).items.single().id)
        assertTrue(manager.getLayout(1).items.isEmpty())
        assertNull(store["layout_1"])
    }

    @Test
    fun `removePage shifts later indices down`() = runTest {
        manager.addItem(0, CanvasItem.AppItem(id = "a", packageName = "com.a"))
        manager.addItem(1, CanvasItem.AppItem(id = "b", packageName = "com.b"))
        manager.addItem(2, CanvasItem.AppItem(id = "c", packageName = "com.c"))

        manager.removePage(1)

        assertEquals("a", manager.getLayout(0).items.single().id)
        assertEquals("c", manager.getLayout(1).items.single().id)
        assertTrue(manager.getLayout(2).items.isEmpty())
    }

    @Test
    fun `removePage preserves orientations on shifted pages`() = runTest {
        manager.addItem(0, CanvasItem.AppItem(id = "a", packageName = "com.a"))
        manager.addItem(1, CanvasItem.AppItem(id = "b", packageName = "com.b"))
        manager.setOrientation(1, CanvasScrollOrientation.VERTICAL)

        manager.removePage(0)

        val shifted = manager.getLayout(0)
        assertEquals("b", shifted.items.single().id)
        assertEquals(CanvasScrollOrientation.VERTICAL, shifted.activeOrientation)
        assertFalse(shifted.verticalArrangement.isEmpty())
        assertFalse(shifted.horizontalArrangement.isEmpty())
    }
}
