package jr.brian.jarnfe.editor

import jr.brian.core.model.MediaKind
import jr.brian.jarnfe.library.JarnGameMetadata
import jr.brian.jarnfe.library.JarnLibraryFeed
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibraryState
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.themekit.ThemeFocusMedia
import jr.brian.jarnfe.themekit.without
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** #576: the editor's preview cases change only what the canvas draws. */
class ThemeEditorPreviewCasesTest {
    private val snes = JarnLibrarySystem(id = "snes", title = "Super Nintendo", gameCount = 3, artPath = "/a.png", logoPath = "/l.png")
    private val nes = JarnLibrarySystem(id = "nes", title = "NES", gameCount = 2)
    private val ct = JarnLibraryGame(
        id = "ct",
        systemId = "snes",
        title = "Chrono Trigger",
        artPath = "/c.png",
        metadata = JarnGameMetadata(genre = "RPG", description = "Time travel"),
    )
    private val smw = JarnLibraryGame(id = "smw", systemId = "snes", title = "Super Mario World", artPath = "/m.png")
    private val real = JarnLibraryState(
        systems = JarnLibraryFeed.ready(listOf(nes, snes)),
        allGames = JarnLibraryFeed.ready(listOf(smw, ct)),
    )
    private val text = ThemeEditorPreviewText(system = "Sample system", game = "Sample game", longTitle = "A very long title")

    private fun preview(case: ThemeEditorPreviewCase, state: JarnLibraryState = real) =
        previewLibraryState(state, ThemeEditorPreviewSettings(case, systemId = "snes", gameId = "ct"), text)

    @After
    fun reset() = ThemeEditorPreview.reset()

    @Test
    fun `the library case focuses the picks and changes nothing else`() {
        val shown = preview(ThemeEditorPreviewCase.Library)
        assertEquals("snes", shown.focusedSystemId)
        assertEquals("ct", shown.focusedAllGameId)
        assertEquals(real.systems.items, shown.systems.items)
        assertEquals(real.allGames.items, shown.allGames.items)
    }

    @Test
    fun `missing data cases strip only the focused game`() {
        val noCover = preview(ThemeEditorPreviewCase.NoCover).allGames.items.single { it.id == "ct" }
        assertNull(noCover.artPath)
        assertNull(noCover.defaultArtPath)
        val noInfo = preview(ThemeEditorPreviewCase.NoInfo).allGames.items.single { it.id == "ct" }
        assertEquals(JarnGameMetadata(), noInfo.metadata)
        assertEquals("/m.png", preview(ThemeEditorPreviewCase.NoCover).allGames.items.single { it.id == "smw" }.artPath)
        assertEquals(setOf(MediaKind.Video), ThemeEditorPreviewCase.NoVideo.hiddenMedia)
        assertEquals(setOf(MediaKind.Cover), ThemeEditorPreviewCase.NoCover.hiddenMedia)
    }

    @Test
    fun `long title renames the focused system and game`() {
        val shown = preview(ThemeEditorPreviewCase.LongTitle)
        assertEquals("A very long title", shown.focusedSystem?.title)
        assertEquals("A very long title", shown.focusedAllGame?.title)
    }

    @Test
    fun `game count cases`() {
        val empty = preview(ThemeEditorPreviewCase.EmptySystem)
        assertEquals(0, empty.focusedSystem?.gameCount)
        assertNull(empty.focusedSystem?.artPath)
        assertTrue(empty.allGames.items.isEmpty())
        assertNull(empty.focusedAllGameId)

        val one = preview(ThemeEditorPreviewCase.OneGame)
        assertEquals(listOf("ct"), one.allGames.items.map { it.id })
        assertEquals(1, one.focusedSystem?.gameCount)

        val many = preview(ThemeEditorPreviewCase.ManyGames)
        assertEquals(PREVIEW_MANY_GAMES, many.allGames.items.size)
        assertEquals(PREVIEW_MANY_GAMES, many.allGames.items.map { it.id }.toSet().size)
        assertEquals("ct", many.allGames.items.first().id)
        assertEquals(PREVIEW_MANY_GAMES, many.focusedSystem?.gameCount)
    }

    @Test
    fun `an empty library still previews with sample items`() {
        val shown = preview(ThemeEditorPreviewCase.Library, JarnLibraryState())
        assertEquals(listOf("Sample system"), shown.systems.items.map { it.title })
        assertEquals(listOf("Sample game"), shown.allGames.items.map { it.title })
    }

    @Test
    fun `the real library is never changed`() {
        val before = real
        ThemeEditorPreviewCase.entries.forEach { preview(it) }
        assertEquals(before, real)
    }

    @Test
    fun `picks step and wrap, and shuffle draws from the library`() {
        assertEquals("snes", ThemeEditorPreview.next(real.systems.items, "nes", JarnLibrarySystem::id))
        assertEquals("nes", ThemeEditorPreview.next(real.systems.items, "snes", JarnLibrarySystem::id))
        assertEquals("nes", ThemeEditorPreview.next(real.systems.items, null, JarnLibrarySystem::id))
        assertNull(ThemeEditorPreview.next(emptyList<JarnLibrarySystem>(), null, JarnLibrarySystem::id))
        ThemeEditorPreview.shuffle(real, Random(7))
        assertTrue(ThemeEditorPreview.settings.systemId in setOf("nes", "snes"))
        assertTrue(ThemeEditorPreview.settings.gameId in setOf("ct", "smw"))
    }

    @Test
    fun `hidden media drops only those kinds`() {
        val media = ThemeFocusMedia("ct", mapOf(MediaKind.Video to "/v.mp4", MediaKind.Fanart to "/f.png"))
        assertEquals(mapOf(MediaKind.Fanart to "/f.png"), media.without(setOf(MediaKind.Video)).paths)
        assertEquals(media, media.without(emptySet()))
    }
}
