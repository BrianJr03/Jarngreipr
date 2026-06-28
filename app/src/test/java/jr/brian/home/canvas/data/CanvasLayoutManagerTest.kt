package jr.brian.home.canvas.data

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasLayoutManagerTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var manager: CanvasLayoutManager

    private val store = mutableMapOf<String, String?>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg()
            mockEditor
        }
        every { mockEditor.remove(any()) } answers {
            store.remove(firstArg<String>())
            mockEditor
        }
        every { mockEditor.apply() } returns Unit
        every { mockPrefs.getString(any(), any()) } answers {
            store[firstArg()] ?: secondArg()
        }

        manager = CanvasLayoutManager(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        store.clear()
    }

    @Test
    fun `getLayout returns defaults for new page`() {
        val layout = manager.getLayout(0)
        assertEquals(CanvasScrollOrientation.VERTICAL, layout.orientation)
        assertEquals(CanvasLayout.DEFAULT_COLUMNS, layout.columns)
        assertEquals(CanvasLayout.DEFAULT_ROWS, layout.rows)
        assertTrue(layout.items.isEmpty())
    }

    @Test
    fun `addItem stores item and persists JSON for the page`() = runTest {
        val item = CanvasItem.AppItem(id = "a", col = 0, row = 0, packageName = "com.x")

        manager.addItem(1, item)

        assertEquals(listOf(item), manager.getLayout(1).items)
        verify { mockEditor.putString("layout_1", any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `addItem with existing id replaces the item in place`() {
        val original = CanvasItem.AppItem("a", col = 0, row = 0, packageName = "com.x")
        val replacement = CanvasItem.AppItem("a", col = 2, row = 2, packageName = "com.y")

        manager.addItem(0, original)
        manager.addItem(0, replacement)

        val items = manager.getLayout(0).items
        assertEquals(1, items.size)
        assertEquals(replacement, items.single())
    }

    @Test
    fun `moveItem updates col and row preserving the variant`() {
        val widget = CanvasItem.WidgetItem(
            id = "w", col = 0, row = 0, colSpan = 2, rowSpan = 2, widgetId = 11
        )
        manager.addItem(0, widget)

        manager.moveItem(0, "w", col = 3, row = 4)

        val moved = manager.getLayout(0).items.single()
        assertTrue(moved is CanvasItem.WidgetItem)
        assertEquals(3, moved.col)
        assertEquals(4, moved.row)
        assertEquals(2, moved.colSpan)
        assertEquals(2, moved.rowSpan)
        assertEquals(11, (moved as CanvasItem.WidgetItem).widgetId)
    }

    @Test
    fun `resizeItem updates spans and clamps below 1`() {
        val widget = CanvasItem.WidgetItem("w", 0, 0, colSpan = 1, rowSpan = 1, widgetId = 1)
        manager.addItem(0, widget)

        manager.resizeItem(0, "w", colSpan = 0, rowSpan = -3)

        val resized = manager.getLayout(0).items.single()
        assertEquals(1, resized.colSpan)
        assertEquals(1, resized.rowSpan)
    }

    @Test
    fun `removeItem deletes only the matching id`() {
        manager.addItem(0, CanvasItem.AppItem("a", 0, 0, packageName = "com.a"))
        manager.addItem(0, CanvasItem.AppItem("b", 1, 0, packageName = "com.b"))

        manager.removeItem(0, "a")

        val ids = manager.getLayout(0).items.map { it.id }
        assertEquals(listOf("b"), ids)
    }

    @Test
    fun `clear removes the page entry from store and state`() {
        manager.addItem(2, CanvasItem.AppItem("a", 0, 0, packageName = "com.a"))

        manager.clear(2)

        assertTrue(manager.getLayout(2).items.isEmpty())
        verify { mockEditor.remove("layout_2") }
    }

    @Test
    fun `setOrientation persists the new orientation`() {
        manager.setOrientation(0, CanvasScrollOrientation.HORIZONTAL)

        assertEquals(CanvasScrollOrientation.HORIZONTAL, manager.getLayout(0).orientation)
        verify { mockEditor.putString("layout_0", any()) }
    }

    @Test
    fun `setGrid clamps columns and rows to MIN and MAX`() {
        manager.setGrid(0, columns = 999, rows = -5)

        val layout = manager.getLayout(0)
        assertEquals(CanvasLayout.MAX_AXIS, layout.columns)
        assertEquals(CanvasLayout.MIN_AXIS, layout.rows)
    }

    @Test
    fun `layouts are isolated between pages`() {
        manager.addItem(0, CanvasItem.AppItem("a0", 0, 0, packageName = "com.a"))
        manager.addItem(1, CanvasItem.AppItem("a1", 0, 0, packageName = "com.b"))

        assertEquals("a0", manager.getLayout(0).items.single().id)
        assertEquals("a1", manager.getLayout(1).items.single().id)
    }

    @Test
    fun `persisted JSON survives reload through a fresh manager instance`() = runTest {
        manager.addItem(
            3,
            CanvasItem.FolderItem("f", col = 1, row = 2, folderId = "folder-1")
        )
        manager.setOrientation(3, CanvasScrollOrientation.HORIZONTAL)

        val fresh = CanvasLayoutManager(mockContext)

        val reloaded = fresh.getLayout(3)
        assertEquals(CanvasScrollOrientation.HORIZONTAL, reloaded.orientation)
        assertEquals(1, reloaded.items.size)
        val item = reloaded.items.single()
        assertTrue(item is CanvasItem.FolderItem)
        assertEquals("folder-1", (item as CanvasItem.FolderItem).folderId)
    }

    @Test
    fun `replaceLayout overwrites the page's layout in place`() {
        manager.addItem(0, CanvasItem.AppItem("a", 0, 0, packageName = "com.a"))

        val replacement = CanvasLayout(
            orientation = CanvasScrollOrientation.HORIZONTAL,
            columns = 5,
            rows = 7,
            items = listOf(CanvasItem.RssLauncherItem("rss", 0, 0))
        )
        manager.replaceLayout(0, replacement)

        val layout = manager.getLayout(0)
        assertEquals(CanvasScrollOrientation.HORIZONTAL, layout.orientation)
        assertEquals(5, layout.columns)
        assertEquals(1, layout.items.size)
        assertTrue(layout.items.single() is CanvasItem.RssLauncherItem)
    }

    @Test
    fun `clearAll drops every page from store and state`() {
        manager.addItem(0, CanvasItem.AppItem("a", 0, 0, packageName = "com.a"))
        manager.addItem(2, CanvasItem.AppItem("b", 0, 0, packageName = "com.b"))

        manager.clearAll()

        assertTrue(manager.getLayout(0).items.isEmpty())
        assertTrue(manager.getLayout(2).items.isEmpty())
    }

    @Test
    fun `layoutsByPage flow emits updated state when item added`() = runTest {
        manager.layoutsByPage.test {
            val initial = awaitItem()
            assertNull(initial[5])

            manager.addItem(5, CanvasItem.RssLauncherItem("rss", 0, 0))

            val updated = awaitItem()
            assertEquals(1, updated[5]?.items?.size)
            assertTrue(updated[5]?.items?.single() is CanvasItem.RssLauncherItem)
        }
    }
}
