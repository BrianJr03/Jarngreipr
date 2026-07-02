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
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.satisfiesInvariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals(CanvasScrollOrientation.HORIZONTAL, layout.activeOrientation)
        assertEquals(CanvasLayout.DEFAULT_COLUMNS, layout.verticalColumns)
        assertEquals(CanvasLayout.DEFAULT_ROWS, layout.horizontalRows)
        assertTrue(layout.items.isEmpty())
        assertTrue(layout.verticalArrangement.isEmpty())
        assertTrue(layout.horizontalArrangement.isEmpty())
    }

    @Test
    fun `addItem places item in both arrangements regardless of active orientation`() = runTest {
        val item = CanvasItem.AppItem(id = "a", packageName = "com.x")

        manager.addItem(1, item)

        val layout = manager.getLayout(1)
        assertEquals(listOf(item), layout.items)
        assertNotNull(layout.verticalArrangement["a"])
        assertNotNull(layout.horizontalArrangement["a"])
        assertTrue(layout.satisfiesInvariant())
        verify { mockEditor.putString("layout_1", any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `addItem preserves placements when called again with same id`() {
        val item = CanvasItem.AppItem("a", packageName = "com.x")
        manager.addItem(0, item)
        val firstVertical = manager.getLayout(0).verticalArrangement["a"]
        val firstHorizontal = manager.getLayout(0).horizontalArrangement["a"]

        val replacement = CanvasItem.AppItem("a", packageName = "com.y")
        manager.addItem(0, replacement)

        val layout = manager.getLayout(0)
        assertEquals(1, layout.items.size)
        assertEquals(replacement, layout.items.single())
        assertEquals(firstVertical, layout.verticalArrangement["a"])
        assertEquals(firstHorizontal, layout.horizontalArrangement["a"])
    }

    @Test
    fun `addItem auto-places into next free cell so a second add doesn't overlap`() {
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        manager.addItem(0, CanvasItem.AppItem("b", packageName = "com.b"))

        val layout = manager.getLayout(0)
        val a = layout.verticalArrangement.getValue("a")
        val b = layout.verticalArrangement.getValue("b")
        assertTrue("vertical placements must not overlap", !a.overlaps(b))
        val ah = layout.horizontalArrangement.getValue("a")
        val bh = layout.horizontalArrangement.getValue("b")
        assertTrue("horizontal placements must not overlap", !ah.overlaps(bh))
    }

    @Test
    fun `moveItem mutates the active orientation's arrangement only`() {
        manager.addItem(0, CanvasItem.WidgetItem(id = "w", widgetId = 11))
        manager.setOrientation(0, CanvasScrollOrientation.VERTICAL)
        val beforeHorizontal = manager.getLayout(0).horizontalArrangement.getValue("w")

        manager.moveItem(0, "w", col = 1, row = 3)

        val layout = manager.getLayout(0)
        val movedVertical = layout.verticalArrangement.getValue("w")
        assertEquals(1, movedVertical.col)
        assertEquals(3, movedVertical.row)
        assertEquals(
            "horizontal arrangement must not change when moving in vertical",
            beforeHorizontal,
            layout.horizontalArrangement.getValue("w")
        )
    }

    @Test
    fun `resizeItem mutates the active orientation's arrangement only`() {
        manager.addItem(0, CanvasItem.WidgetItem("w", widgetId = 1))
        manager.setOrientation(0, CanvasScrollOrientation.VERTICAL)
        val beforeHorizontal = manager.getLayout(0).horizontalArrangement.getValue("w")

        manager.resizeItem(0, "w", colSpan = 3, rowSpan = 4)

        val layout = manager.getLayout(0)
        val resizedVertical = layout.verticalArrangement.getValue("w")
        assertEquals(3, resizedVertical.colSpan)
        assertEquals(4, resizedVertical.rowSpan)
        assertEquals(
            "horizontal arrangement must not change when resizing in vertical",
            beforeHorizontal,
            layout.horizontalArrangement.getValue("w")
        )
    }

    @Test
    fun `resizeItem clamps spans below 1`() {
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        manager.resizeItem(0, "a", colSpan = 0, rowSpan = -3)

        val rect = manager.getLayout(0).activeArrangement.getValue("a")
        assertEquals(1, rect.colSpan)
        assertEquals(1, rect.rowSpan)
    }

    @Test
    fun `removeItem deletes from items and both arrangements`() {
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        manager.addItem(0, CanvasItem.AppItem("b", packageName = "com.b"))

        manager.removeItem(0, "a")

        val layout = manager.getLayout(0)
        assertEquals(listOf("b"), layout.items.map { it.id })
        assertNull(layout.verticalArrangement["a"])
        assertNull(layout.horizontalArrangement["a"])
        assertTrue(layout.satisfiesInvariant())
    }

    @Test
    fun `clear removes the page entry from store and state`() {
        manager.addItem(2, CanvasItem.AppItem("a", packageName = "com.a"))

        manager.clear(2)

        assertTrue(manager.getLayout(2).items.isEmpty())
        verify { mockEditor.remove("layout_2") }
    }

    @Test
    fun `setOrientation does not perturb either arrangement`() {
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        val verticalBefore = manager.getLayout(0).verticalArrangement
        val horizontalBefore = manager.getLayout(0).horizontalArrangement

        manager.setOrientation(0, CanvasScrollOrientation.VERTICAL)

        val layout = manager.getLayout(0)
        assertEquals(CanvasScrollOrientation.VERTICAL, layout.activeOrientation)
        assertEquals(verticalBefore, layout.verticalArrangement)
        assertEquals(horizontalBefore, layout.horizontalArrangement)
    }

    @Test
    fun `setGrid updates both axis bounds and clamps to MIN MAX`() {
        manager.setGrid(0, columns = 999, rows = -5)

        val layout = manager.getLayout(0)
        assertEquals(CanvasLayout.MAX_AXIS, layout.verticalColumns)
        assertEquals(CanvasLayout.MIN_AXIS, layout.horizontalRows)
    }

    @Test
    fun `layouts are isolated between pages`() {
        manager.addItem(0, CanvasItem.AppItem("a0", packageName = "com.a"))
        manager.addItem(1, CanvasItem.AppItem("a1", packageName = "com.b"))

        assertEquals("a0", manager.getLayout(0).items.single().id)
        assertEquals("a1", manager.getLayout(1).items.single().id)
    }

    @Test
    fun `persisted JSON survives reload through a fresh manager instance`() = runTest {
        manager.addItem(3, CanvasItem.FolderItem("f", folderId = "folder-1"))
        manager.setOrientation(3, CanvasScrollOrientation.HORIZONTAL)

        val fresh = CanvasLayoutManager(mockContext)

        val reloaded = fresh.getLayout(3)
        assertEquals(CanvasScrollOrientation.HORIZONTAL, reloaded.activeOrientation)
        assertEquals(1, reloaded.items.size)
        val item = reloaded.items.single()
        assertTrue(item is CanvasItem.FolderItem)
        assertEquals("folder-1", (item as CanvasItem.FolderItem).folderId)
        assertTrue(reloaded.satisfiesInvariant())
    }

    @Test
    fun `replaceLayout repairs missing arrangement entries and persists`() {
        val incoming = CanvasLayout(
            items = listOf(
                CanvasItem.AppItem("a", packageName = "com.a"),
                CanvasItem.AppItem("b", packageName = "com.b")
            ),
            verticalArrangement = mapOf("a" to GridRect(0, 0, 1, 1)),
            horizontalArrangement = emptyMap()
        )
        manager.replaceLayout(0, incoming)

        val layout = manager.getLayout(0)
        assertTrue(layout.satisfiesInvariant())
        assertNotNull(layout.verticalArrangement["b"])
        assertNotNull(layout.horizontalArrangement["a"])
        assertNotNull(layout.horizontalArrangement["b"])
    }

    @Test
    fun `replaceLayout drops orphan arrangement entries`() {
        val incoming = CanvasLayout(
            items = listOf(CanvasItem.AppItem("a", packageName = "com.a")),
            verticalArrangement = mapOf(
                "a" to GridRect(0, 0, 1, 1),
                "ghost" to GridRect(2, 2, 1, 1)
            ),
            horizontalArrangement = mapOf(
                "a" to GridRect(0, 0, 1, 1),
                "ghost" to GridRect(2, 2, 1, 1)
            )
        )
        manager.replaceLayout(0, incoming)

        val layout = manager.getLayout(0)
        assertTrue(layout.satisfiesInvariant())
        assertNull(layout.verticalArrangement["ghost"])
        assertNull(layout.horizontalArrangement["ghost"])
    }

    @Test
    fun `clearAll drops every page from store and state`() {
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        manager.addItem(2, CanvasItem.AppItem("b", packageName = "com.b"))

        manager.clearAll()

        assertTrue(manager.getLayout(0).items.isEmpty())
        assertTrue(manager.getLayout(2).items.isEmpty())
    }

    @Test
    fun `layoutsByPage flow emits updated state when item added`() = runTest {
        manager.layoutsByPage.test {
            val initial = awaitItem()
            assertNull(initial[5])

            manager.addItem(5, CanvasItem.RssLauncherItem("rss"))

            val updated = awaitItem()
            assertEquals(1, updated[5]?.items?.size)
            assertTrue(updated[5]?.items?.single() is CanvasItem.RssLauncherItem)
        }
    }

    // --- Phase 2 acceptance: orientation independence ----------------------

    @Test
    fun `addItem in horizontal active orientation populates both arrangements`() {
        manager.setOrientation(0, CanvasScrollOrientation.HORIZONTAL)

        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))

        val layout = manager.getLayout(0)
        assertEquals(CanvasScrollOrientation.HORIZONTAL, layout.activeOrientation)
        assertNotNull(layout.verticalArrangement["a"])
        assertNotNull(layout.horizontalArrangement["a"])
        assertTrue(layout.satisfiesInvariant())
    }

    @Test
    fun `switching active orientation reveals the previously-inactive arrangement unchanged`() {
        manager.setOrientation(0, CanvasScrollOrientation.HORIZONTAL)
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        val verticalAtAdd = manager.getLayout(0).verticalArrangement
        val horizontalAtAdd = manager.getLayout(0).horizontalArrangement

        manager.setOrientation(0, CanvasScrollOrientation.VERTICAL)

        val layout = manager.getLayout(0)
        assertEquals(CanvasScrollOrientation.VERTICAL, layout.activeOrientation)
        assertEquals("vertical arrangement preserved across orientation switch", verticalAtAdd, layout.verticalArrangement)
        assertEquals("horizontal arrangement preserved across orientation switch", horizontalAtAdd, layout.horizontalArrangement)
        assertEquals(verticalAtAdd, layout.activeArrangement)
    }

    @Test
    fun `addItem honors atCellInActive when active orientation is horizontal`() {
        manager.setOrientation(0, CanvasScrollOrientation.HORIZONTAL)

        manager.addItem(
            pageIndex = 0,
            item = CanvasItem.AppItem("a", packageName = "com.a"),
            atCellInActive = GridCell(2, 3)
        )

        val rect = manager.getLayout(0).horizontalArrangement.getValue("a")
        assertEquals(2, rect.col)
        assertEquals(3, rect.row)
    }

    @Test
    fun `addItem ignores atCellInActive when that cell is occupied`() {
        manager.setOrientation(0, CanvasScrollOrientation.VERTICAL)
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        val occupied = manager.getLayout(0).verticalArrangement.getValue("a")

        manager.addItem(
            pageIndex = 0,
            item = CanvasItem.AppItem("b", packageName = "com.b"),
            atCellInActive = GridCell(occupied.col, occupied.row)
        )

        val bRect = manager.getLayout(0).verticalArrangement.getValue("b")
        assertTrue("collision must fall back to next free cell", !bRect.overlaps(occupied))
    }

    @Test
    fun `moveItem in horizontal-active leaves vertical arrangement byte-for-byte unchanged`() {
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        val verticalBefore = manager.getLayout(0).verticalArrangement
        manager.setOrientation(0, CanvasScrollOrientation.HORIZONTAL)

        manager.moveItem(0, "a", col = 4, row = 1)

        assertEquals(verticalBefore, manager.getLayout(0).verticalArrangement)
    }

    @Test
    fun `resizeItem in horizontal-active leaves vertical arrangement byte-for-byte unchanged`() {
        manager.addItem(0, CanvasItem.WidgetItem("w", widgetId = 1))
        val verticalBefore = manager.getLayout(0).verticalArrangement
        manager.setOrientation(0, CanvasScrollOrientation.HORIZONTAL)

        manager.resizeItem(0, "w", colSpan = 4, rowSpan = 2)

        assertEquals(verticalBefore, manager.getLayout(0).verticalArrangement)
    }

    @Test
    fun `compactLayout closes gaps in active arrangement and leaves the other untouched`() {
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        manager.addItem(0, CanvasItem.AppItem("b", packageName = "com.b"))
        manager.addItem(0, CanvasItem.AppItem("c", packageName = "com.c"))
        manager.removeItem(0, "b")
        manager.setOrientation(0, CanvasScrollOrientation.VERTICAL)

        val horizontalBefore = manager.getLayout(0).horizontalArrangement

        manager.compactLayout(0)

        val layout = manager.getLayout(0)
        assertEquals(
            "compact must not touch the inactive arrangement",
            horizontalBefore,
            layout.horizontalArrangement
        )
        // After compact: every item should be pulled toward origin — push-axis
        // start of each item should not exceed (item count - 1) along the
        // vertical push direction (row).
        val maxRow = layout.verticalArrangement.values.maxOf { it.row }
        assertTrue("compacted vertical layout should pull rows toward 0", maxRow <= layout.items.size - 1)
        assertTrue(layout.satisfiesInvariant())
    }

    @Test
    fun `compactLayout is a no-op when active arrangement is empty`() {
        // No items yet; should not throw or persist anything beyond what
        // touching the page already did.
        manager.compactLayout(0)
        val layout = manager.getLayout(0)
        assertTrue(layout.items.isEmpty())
        assertTrue(layout.satisfiesInvariant())
    }

    // --- Phase 3: v1 → v2 migration ----------------------------------------

    @Test
    fun `v1 vertical blob preserves user positions in vertical and auto-places horizontal`() {
        // Active VERTICAL: columns=4 cross-axis, rows is the secondary bound.
        val legacyJson = """
            {
              "orientation": "VERTICAL",
              "columns": 4,
              "rows": 6,
              "items": [
                {"type":"app","id":"a","col":3,"row":0,"colSpan":1,"rowSpan":1,"packageName":"com.legacy.a"},
                {"type":"app","id":"b","col":0,"row":2,"colSpan":1,"rowSpan":1,"packageName":"com.legacy.b"},
                {"type":"widget","id":"w","col":1,"row":1,"colSpan":2,"rowSpan":2,"widgetId":7}
              ],
              "version": 1
            }
        """.trimIndent()
        store["layout_4"] = legacyJson

        val fresh = CanvasLayoutManager(mockContext)
        val loaded = fresh.getLayout(4)

        assertEquals(3, loaded.items.size)
        assertTrue(loaded.satisfiesInvariant())
        // V1 positions preserved exactly in the v1-orientation arrangement.
        assertEquals(GridRect(3, 0, 1, 1), loaded.verticalArrangement.getValue("a"))
        assertEquals(GridRect(0, 2, 1, 1), loaded.verticalArrangement.getValue("b"))
        assertEquals(GridRect(1, 1, 2, 2), loaded.verticalArrangement.getValue("w"))
        // Horizontal arrangement auto-placed with no overlaps.
        val horizontal = loaded.horizontalArrangement.values.toList()
        for (i in horizontal.indices) {
            for (j in i + 1 until horizontal.size) {
                assertTrue(
                    "horizontal placements must not overlap",
                    !horizontal[i].overlaps(horizontal[j])
                )
            }
        }
    }

    @Test
    fun `v1 horizontal blob preserves user positions in horizontal and auto-places vertical`() {
        // Active HORIZONTAL: cross-axis = rows=4, push axis = col.
        val legacyJson = """
            {
              "orientation": "HORIZONTAL",
              "columns": 6,
              "rows": 4,
              "items": [
                {"type":"rom","id":"r1","col":0,"row":0,"colSpan":1,"rowSpan":1,"romKey":"snes/a"},
                {"type":"rom","id":"r2","col":5,"row":3,"colSpan":1,"rowSpan":1,"romKey":"snes/b"},
                {"type":"app","id":"a","col":2,"row":1,"colSpan":1,"rowSpan":1,"packageName":"com.a"}
              ],
              "version": 1
            }
        """.trimIndent()
        store["layout_2"] = legacyJson

        val fresh = CanvasLayoutManager(mockContext)
        val loaded = fresh.getLayout(2)

        assertEquals(CanvasScrollOrientation.HORIZONTAL, loaded.activeOrientation)
        assertEquals(3, loaded.items.size)
        assertTrue(loaded.satisfiesInvariant())
        assertEquals(GridRect(0, 0, 1, 1), loaded.horizontalArrangement.getValue("r1"))
        assertEquals(GridRect(5, 3, 1, 1), loaded.horizontalArrangement.getValue("r2"))
        assertEquals(GridRect(2, 1, 1, 1), loaded.horizontalArrangement.getValue("a"))
        // Vertical arrangement auto-placed without overlaps.
        val vertical = loaded.verticalArrangement.values.toList()
        for (i in vertical.indices) {
            for (j in i + 1 until vertical.size) {
                assertTrue(
                    "vertical placements must not overlap",
                    !vertical[i].overlaps(vertical[j])
                )
            }
        }
    }

    @Test
    fun `migration persists migrated blob so subsequent loads skip migration`() {
        val legacyJson = """
            {
              "orientation": "VERTICAL",
              "columns": 4,
              "rows": 6,
              "items": [
                {"type":"app","id":"a","col":0,"row":0,"colSpan":1,"rowSpan":1,"packageName":"com.a"}
              ],
              "version": 1
            }
        """.trimIndent()
        store["layout_1"] = legacyJson

        val first = CanvasLayoutManager(mockContext)
        val firstLoad = first.getLayout(1)
        assertEquals(CanvasLayout.CURRENT_VERSION, firstLoad.version)

        // Persisted blob is in v2 shape (any v2-only marker proves it isn't
        // the original v1 JSON).
        val persisted = store["layout_1"]
        assertNotNull(persisted)
        assertTrue(
            "persisted blob should be v2 shape",
            persisted!!.contains("\"verticalArrangement\"") ||
                persisted.contains("\"horizontalArrangement\"") ||
                persisted.contains("\"activeOrientation\"")
        )
        assertTrue(
            "persisted blob must no longer contain v1-only fields",
            !persisted.contains("\"orientation\"")
        )

        val second = CanvasLayoutManager(mockContext)
        val secondLoad = second.getLayout(1)
        assertEquals(firstLoad, secondLoad)
    }

    @Test
    fun `migration is idempotent on already-v2 data — content and arrangements identical`() = runTest {
        // Round-trip a v2 layout into prefs via the real manager.
        manager.addItem(0, CanvasItem.AppItem("a", packageName = "com.a"))
        manager.addItem(0, CanvasItem.WidgetItem("w", widgetId = 9))
        manager.moveItem(0, "w", col = 1, row = 1)
        val before = manager.getLayout(0)

        val reloaded = CanvasLayoutManager(mockContext).getLayout(0)

        assertEquals(before.items, reloaded.items)
        assertEquals(before.verticalArrangement, reloaded.verticalArrangement)
        assertEquals(before.horizontalArrangement, reloaded.horizontalArrangement)
        assertEquals(before.activeOrientation, reloaded.activeOrientation)
        assertEquals(CanvasLayout.CURRENT_VERSION, reloaded.version)
    }

    @Test
    fun `maintainer scenario — v1 items added in horizontal mode become visible after switching to vertical`() {
        // The bug as the maintainer experiences it: items added while horizontal
        // mode was active were "missing" after switching to vertical. Phase 3
        // migration must surface them.
        val legacyJson = """
            {
              "orientation": "HORIZONTAL",
              "columns": 4,
              "rows": 6,
              "items": [
                {"type":"app","id":"m1","col":0,"row":0,"colSpan":1,"rowSpan":1,"packageName":"com.missing.1"},
                {"type":"app","id":"m2","col":3,"row":2,"colSpan":1,"rowSpan":1,"packageName":"com.missing.2"},
                {"type":"app","id":"m3","col":6,"row":4,"colSpan":1,"rowSpan":1,"packageName":"com.missing.3"}
              ],
              "version": 1
            }
        """.trimIndent()
        store["layout_0"] = legacyJson

        val fresh = CanvasLayoutManager(mockContext)
        // Simulate the user switching to vertical after launching the app.
        fresh.setOrientation(0, CanvasScrollOrientation.VERTICAL)
        val verticalView = fresh.getLayout(0)

        // Every item the user added in horizontal is now placed in the active
        // (vertical) arrangement — no items lost, none missing.
        assertEquals(setOf("m1", "m2", "m3"), verticalView.verticalArrangement.keys)
        assertEquals(setOf("m1", "m2", "m3"), verticalView.activeArrangement.keys)
        assertEquals(setOf("m1", "m2", "m3"), verticalView.items.map { it.id }.toSet())
    }

    @Test
    fun `migration handles single-item v1 blob without losing the item`() {
        val legacyJson = """
            {
              "orientation": "VERTICAL",
              "columns": 4,
              "rows": 6,
              "items": [
                {"type":"rss_launcher","id":"rss","col":2,"row":3,"colSpan":1,"rowSpan":1}
              ],
              "version": 1
            }
        """.trimIndent()
        store["layout_5"] = legacyJson

        val loaded = CanvasLayoutManager(mockContext).getLayout(5)

        assertEquals(1, loaded.items.size)
        assertTrue(loaded.items.single() is CanvasItem.RssLauncherItem)
        assertEquals(GridRect(2, 3, 1, 1), loaded.verticalArrangement.getValue("rss"))
        assertNotNull(loaded.horizontalArrangement["rss"])
        assertTrue(loaded.satisfiesInvariant())
    }
}
