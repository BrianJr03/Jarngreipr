package jr.brian.jarnfe.canvas

import jr.brian.core.model.GameImageType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.EsdeContentScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasTileActionsTest {
    private val everyType = listOf(
        CanvasItem.AppItem("a", "pkg"),
        CanvasItem.FolderItem("f", "folder"),
        CanvasItem.RomItem("r", "key"),
        CanvasItem.WidgetItem("w", 7),
        CanvasItem.RssLauncherItem("l"),
        CanvasItem.RssMusicItem("m"),
        CanvasItem.EsdeArtItem("e"),
        CanvasItem.PhotoContainer("p"),
        CanvasItem.LastPlayedItem("n"),
    )

    @Test
    fun `every tile type can be moved and removed, even when its content is missing`() {
        everyType.forEach { item ->
            val actions = canvasTileActions(item, resolved = false, JarnWidgetHealth.Gone, hasRecent = false)
            assertTrue(CanvasTileAction.Move in actions)
            assertEquals(CanvasTileAction.Remove, actions.last())
        }
    }

    @Test
    fun `a widget id that no longer exists is never offered a rebind`() {
        val widget = CanvasItem.WidgetItem("w", 7)
        val gone = canvasTileActions(widget, true, JarnWidgetHealth.Gone, false)
        val unbound = canvasTileActions(widget, true, JarnWidgetHealth.Unbound, false)
        assertFalse(CanvasTileAction.ChooseWidget in gone)
        assertFalse(CanvasTileAction.UseWidget in gone)
        assertTrue(CanvasTileAction.ChooseWidget in unbound)
    }

    @Test
    fun `random is only offered with games to pick from`() {
        val tile = CanvasItem.LastPlayedItem("n")
        assertFalse(CanvasTileAction.PlayRandom in canvasTileActions(tile, true, null, false))
        assertTrue(CanvasTileAction.PlayRandom in canvasTileActions(tile, true, null, true))
    }

    @Test
    fun `flipping scale writes the opposite of the resolved default`() {
        val marquee = CanvasItem.EsdeArtItem("e", imageType = GameImageType.Marquee)
        assertEquals(EsdeContentScale.CROP, (marquee.withFlippedScale() as CanvasItem.EsdeArtItem).contentScale)
        val rom = CanvasItem.RomItem("r", "key")
        assertEquals(EsdeContentScale.FIT, (rom.withFlippedScale() as CanvasItem.RomItem).contentScale)
    }

    @Test
    fun `games shown wraps inside the stored limits`() {
        assertEquals(6, CanvasItem.LastPlayedItem("n").withNextCount().count)
        assertEquals(
            CanvasItem.LastPlayedItem.MIN_DISPLAY_COUNT,
            CanvasItem.LastPlayedItem("n", count = CanvasItem.LastPlayedItem.MAX_DISPLAY_COUNT).withNextCount().count,
        )
    }

    @Test
    fun `control navigation picks the nearest control in the pressed direction`() {
        val controls = listOf(
            CanvasControlBounds(0, 0, 10, 10),
            CanvasControlBounds(20, 0, 30, 10),
            CanvasControlBounds(90, 0, 100, 10),
            CanvasControlBounds(0, 40, 10, 50),
        )
        assertEquals(1, nextCanvasControl(controls, 0, 1, 0))
        assertEquals(3, nextCanvasControl(controls, 0, 0, 1))
        assertEquals(0, nextCanvasControl(controls, 0, -1, 0))
    }
}
