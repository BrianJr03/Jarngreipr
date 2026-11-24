package jr.brian.home.canvas.jarn

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.data.PageTypeManager
import jr.brian.home.data.PinnedRomManager
import jr.brian.home.model.PageType
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.jarnfe.canvas.JarnCanvasTileMove
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppJarnCanvasTileMoveTest {
    private val saved = mutableMapOf<Int, CanvasLayout>()
    private val layouts = mockk<CanvasLayoutManager>(relaxed = true).also { manager ->
        every { manager.getLayout(any()) } answers { saved[firstArg()] ?: CanvasLayout() }
        every { manager.replaceLayout(any(), any()) } answers { saved[firstArg()] = secondArg() }
        every { manager.removeItem(any(), any()) } answers {
            val id = secondArg<String>()
            val page = firstArg<Int>()
            val layout = saved.getValue(page)
            saved[page] = layout.copy(
                items = layout.items.filter { it.id != id },
                verticalArrangement = layout.verticalArrangement - id,
                horizontalArrangement = layout.horizontalArrangement - id,
            )
        }
    }
    private val pageTypes = mockk<PageTypeManager>(relaxed = true).also {
        every { it.pageTypes } returns MutableStateFlow(listOf(PageType.UNIFIED_CANVAS, PageType.UNIFIED_CANVAS))
    }
    private val pins = mockk<PinnedRomManager>(relaxed = true)
    private val tiles = AppJarnCanvasTiles(
        context = mockk(relaxed = true),
        layouts = layouts,
        pageTypes = pageTypes,
        folders = mockk(relaxed = true),
        pinnedRoms = pins,
        customNames = mockk(relaxed = true),
        widgetPreferences = mockk(relaxed = true),
        nowPlaying = mockk(relaxed = true),
        rss = mockk(relaxed = true),
    )

    private fun page(item: CanvasItem, rect: GridRect = GridRect(1, 0, 1, 1)) = CanvasLayout(
        items = listOf(item),
        horizontalArrangement = mapOf(item.id to rect),
        verticalArrangement = mapOf(item.id to rect),
    )

    private fun moveOf(id: String) = JarnCanvasTileMove(
        itemId = id,
        fromPage = 0,
        toPage = 1,
        orientation = CanvasScrollOrientation.HORIZONTAL,
        placements = mapOf(id to GridRect(2, 0, 1, 1)),
    )

    @Test
    fun `the destination is written before the source is cleared`() = runBlocking {
        saved[0] = page(CanvasItem.AppItem("a", "pkg"))
        saved[1] = page(CanvasItem.AppItem("c", "pkg.c"))
        assertNotNull(tiles.moveTile(moveOf("a")))
        verifyOrder {
            layouts.replaceLayout(1, any())
            layouts.removeItem(0, "a")
        }
        assertTrue(saved.getValue(0).items.isEmpty())
        assertEquals(setOf("a", "c"), saved.getValue(1).verticalArrangement.keys)
    }

    @Test
    fun `a failed source write leaves a copy instead of losing the tile`() {
        saved[0] = page(CanvasItem.AppItem("a", "pkg"))
        saved[1] = CanvasLayout()
        every { layouts.removeItem(0, "a") } throws IllegalStateException("disk")
        val failed = runCatching { runBlocking { tiles.moveTile(moveOf("a")) } }
        assertTrue(failed.isFailure)
        assertTrue(saved.getValue(0).items.any { it.id == "a" })
        assertTrue(saved.getValue(1).items.any { it.id == "a" })
    }

    @Test
    fun `a pinned game takes its pin along and undo brings it back`() = runBlocking {
        val rom = mockk<PinnedRomInfo>(relaxed = true).also { every { it.key } returns "k" }
        val pinned = mutableMapOf(0 to listOf(rom), 1 to emptyList<PinnedRomInfo>())
        every { pins.getPinnedRoms(any(), CanvasTabType.VALUE) } answers { flowOf(pinned.getValue(firstArg())) }
        every { pins.addPinnedRom(any(), any(), CanvasTabType.VALUE) } answers {
            pinned[firstArg()] = pinned.getValue(firstArg()) + secondArg<PinnedRomInfo>()
        }
        every { pins.removePinnedRom(any(), any(), CanvasTabType.VALUE) } answers {
            pinned[firstArg()] = pinned.getValue(firstArg()).filter { it.key != secondArg<String>() }
        }
        val before = page(CanvasItem.RomItem("rom-k", "k"))
        saved[0] = before
        saved[1] = CanvasLayout()
        val moved = tiles.moveTile(moveOf("rom-k"))!!
        assertTrue(moved.pinAdded && moved.pinRemoved)
        verifyOrder {
            pins.addPinnedRom(1, rom, CanvasTabType.VALUE)
            layouts.replaceLayout(1, any())
            layouts.removeItem(0, "rom-k")
            pins.removePinnedRom(0, "k", CanvasTabType.VALUE)
        }
        assertTrue(tiles.undoTileMove(moved))
        assertEquals(before, saved[0])
        assertEquals(CanvasLayout(), saved[1])
        assertEquals(listOf(rom), pinned[0])
        assertTrue(pinned.getValue(1).isEmpty())
    }

    @Test
    fun `a widget keeps its id and is never released`() = runBlocking {
        saved[0] = page(CanvasItem.WidgetItem("widget-9", 9))
        saved[1] = CanvasLayout()
        val moved = tiles.moveTile(moveOf("widget-9"))
        assertNotNull(moved)
        assertEquals(CanvasItem.WidgetItem("widget-9", 9), saved.getValue(1).items.single())
        verify(exactly = 0) { pins.removePinnedRom(any(), any(), any()) }
    }

    @Test
    fun `a folder is refused with nothing written`() = runBlocking {
        saved[0] = page(CanvasItem.FolderItem("f", "id"))
        saved[1] = CanvasLayout()
        assertNull(tiles.moveTile(moveOf("f")))
        verify(exactly = 0) { layouts.replaceLayout(any(), any()) }
        verify(exactly = 0) { layouts.removeItem(any(), any()) }
    }

    @Test
    fun `undo is refused when either page changed since the move`() = runBlocking {
        saved[0] = page(CanvasItem.AppItem("a", "pkg"))
        saved[1] = CanvasLayout()
        val moved = tiles.moveTile(moveOf("a"))!!
        saved[1] = saved.getValue(1).copy(menuButtonVisible = false)
        val changed = saved.getValue(1)
        assertFalse(tiles.undoTileMove(moved))
        assertEquals(changed, saved[1])
    }
}
