package jr.brian.jarnfe.themekit

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ViewSpec
import jr.brian.jarnfe.library.JarnLibraryGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #569: pictures try their fallbacks in order, and stacked layers close up around empty ones. */
class ThemeFallbackStackTest {
    private val game = JarnLibraryGame(id = "g1", systemId = "snes", title = "Chrono Trigger", artPath = "/covers/ct.png")
    private val focus = FocusedItem.Game(game)
    private val media = ThemeFocusMedia("g1", mapOf(MediaKind.Screenshot to "/shots/ct.png", MediaKind.Fanart to "/fan/ct.png"))

    private fun media(kind: MediaKind) = ThemePicture.GameMedia(kind)

    @Test
    fun `fallbacks resolve in order and skip what the game lacks`() {
        val sources = listOf(media(MediaKind.Miximage), media(MediaKind.Screenshot), media(MediaKind.Fanart))
        assertEquals(listOf("/shots/ct.png", "/fan/ct.png"), resolveThemePictures(sources, focus, media, null))
    }

    @Test
    fun `fallbacks drop repeats and come back empty when nothing exists`() {
        val repeated = listOf(media(MediaKind.Cover), media(MediaKind.Cover), media(MediaKind.Screenshot))
        assertEquals(listOf("/covers/ct.png", "/shots/ct.png"), resolveThemePictures(repeated, focus, media, null))
        assertTrue(resolveThemePictures(listOf(media(MediaKind.Marquee)), focus, media, null).isEmpty())
    }

    @Test
    fun `an extra video keeps its whole fallback chain as a picture`() {
        val video = { id: String ->
            ThemeLayer.Video(id = id, rect = ThemeRect(0f, 0f, 0.2f, 0.2f), fallback = null, fallbacks = listOf(media(MediaKind.Fanart), media(MediaKind.Cover)))
        }
        val view = ViewSpec(ThemePrimary.Grid(ThemeRect(0.3f, 0.3f, 0.5f, 0.5f)), listOf(video("a"), video("b")))
        val extra = resolveThemeView(view, ThemeScreen(1000, 1000)).below.last().layer as ThemeLayer.Picture
        assertEquals(media(MediaKind.Fanart), extra.source)
        assertEquals(listOf(media(MediaKind.Cover)), extra.fallbacks)
    }

    @Test
    fun `stack members gather into one slot where the first draws`() {
        fun text(id: String, stack: String?) = PlacedThemeLayer(
            ThemeLayer.Text(id = id, rect = ThemeRect(0f, 0f, 0.1f, 0.1f), content = ThemeText.Static(id), stack = stack),
            ThemePxRect(0, id.length * 10, 100, 10),
        )
        val items = listOf(text("a", "info"), text("solo", null), text("bb", "info")).slotItems()
        assertEquals(listOf("jarn-theme-stack:info", "solo"), items.map { it.key })
        assertEquals(ThemePxRect(0, 10, 100, 20), items.first().bounds)
    }

    @Test
    fun `empty rows close up and short rows pull the rest up`() {
        val rows = listOf(
            StackRow(top = 0, extent = 100, measured = 40),
            StackRow(top = 110, extent = 30, measured = 0),
            StackRow(top = 150, extent = 30, measured = 30),
            StackRow(top = 190, extent = 60, measured = 60),
        )
        assertEquals(listOf(0, null, 50, 90), stackRowTops(rows))
    }

    @Test
    fun `a stack whose first rows are empty starts at the top`() {
        val rows = listOf(StackRow(0, 50, 0), StackRow(60, 20, 20))
        assertEquals(listOf(null, 0), stackRowTops(rows))
    }
}
