package jr.brian.home.canvas.model

import jr.brian.core.model.GameImageType
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the stored JSON format so saved layouts and backups keep loading. */
class CanvasLayoutFormatCompatibilityTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val currentFormat = """
        {"activeOrientation":"VERTICAL","verticalColumns":5,"horizontalRows":7,"items":[
        {"type":"app","id":"a","packageName":"com.example.app"},
        {"type":"folder","id":"f","folderId":"folder-1"},
        {"type":"rom","id":"r","romKey":"snes/game.smc","contentScale":"FIT"},
        {"type":"widget","id":"w","widgetId":42},
        {"type":"rss_launcher","id":"l"},
        {"type":"rss_music","id":"m"},
        {"type":"esde_art","id":"e","imageType":"Marquee","contentScale":"CROP","backgroundColorArgb":4278190080,"backgroundCornerRadiusDp":8,"artType":"LOGO"},
        {"type":"photo","id":"p","imageUri":"file:///photo.png","contentScale":"FIT"},
        {"type":"last_played","id":"n","count":7,"showRandomButton":false,"contentScale":"CROP","backgroundColorArgb":4278190335,"backgroundCornerRadiusDp":16}
        ],"verticalArrangement":{"a":{"col":0,"row":0,"colSpan":1,"rowSpan":1}},
        "horizontalArrangement":{"a":{"col":1,"row":2,"colSpan":2,"rowSpan":3}},
        "editMode":true,"menuButtonVisible":false}
    """.trimIndent().replace("\n", "")

    @Test
    fun `current format decodes every tile type`() {
        val layout = json.decodeFromString<CanvasLayout>(currentFormat)

        assertEquals(CanvasScrollOrientation.VERTICAL, layout.activeOrientation)
        assertEquals(5, layout.verticalColumns)
        assertEquals(7, layout.horizontalRows)
        assertEquals(CanvasLayout.CURRENT_VERSION, layout.version)
        assertEquals(GridRect(1, 2, 2, 3), layout.horizontalArrangement.getValue("a"))
        assertEquals(
            listOf(
                CanvasItem.AppItem::class,
                CanvasItem.FolderItem::class,
                CanvasItem.RomItem::class,
                CanvasItem.WidgetItem::class,
                CanvasItem.RssLauncherItem::class,
                CanvasItem.RssMusicItem::class,
                CanvasItem.EsdeArtItem::class,
                CanvasItem.PhotoContainer::class,
                CanvasItem.LastPlayedItem::class
            ),
            layout.items.map { it::class }
        )
        val art = layout.items.filterIsInstance<CanvasItem.EsdeArtItem>().single()
        assertEquals(GameImageType.Marquee, art.resolvedImageType)
    }

    @Test
    fun `current format re-encodes byte for byte`() {
        val layout = json.decodeFromString<CanvasLayout>(currentFormat)
        assertEquals(currentFormat, json.encodeToString(layout))
    }

    @Test
    fun `v2 blob without optional fields uses unchanged defaults`() {
        val layout = json.decodeFromString<CanvasLayout>(
            """{"items":[{"type":"app","id":"a","packageName":"p"}]}"""
        )

        assertEquals(CanvasScrollOrientation.HORIZONTAL, layout.activeOrientation)
        assertEquals(CanvasLayout.DEFAULT_COLUMNS, layout.verticalColumns)
        assertEquals(CanvasLayout.DEFAULT_ROWS, layout.horizontalRows)
        assertEquals(false, layout.editMode)
        assertEquals(true, layout.menuButtonVisible)
        assertEquals(2, layout.version)
    }

    @Test
    fun `serial names keep their original packages`() {
        assertEquals(
            "jr.brian.home.canvas.model.CanvasLayout",
            CanvasLayout.serializer().descriptor.serialName
        )
        assertEquals(
            "jr.brian.home.canvas.model.CanvasItem",
            CanvasItem.serializer().descriptor.serialName
        )
        assertEquals(
            "jr.brian.home.canvas.model.GridRect",
            GridRect.serializer().descriptor.serialName
        )
        assertEquals(
            "jr.brian.home.model.rom.PinnedRomInfo",
            PinnedRomInfo.serializer().descriptor.serialName
        )
    }

    @Test
    fun `pinned rom keeps its stored fields`() {
        val stored = """{"key":"snes/a","name":"A","systemName":"snes","path":"a","displayMediaType":"Covers"}"""
        val rom = json.decodeFromString<PinnedRomInfo>(stored)

        assertEquals("Covers", rom.displayMediaType)
        assertTrue(rom.artworkPath == null)
        assertEquals(stored, json.encodeToString(rom))
    }
}
