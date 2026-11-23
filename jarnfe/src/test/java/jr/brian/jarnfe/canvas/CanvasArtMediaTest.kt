package jr.brian.jarnfe.canvas

import jr.brian.core.model.GameImageType
import jr.brian.frontend.library.media.MediaKind
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.EsdeArtType
import jr.brian.home.canvas.model.EsdeContentScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArtMediaTest {
    @Test
    fun `every offered type maps to its own still image kind`() {
        val kinds = CanvasArtMediaTypes.map { it.toMediaKind() }
        assertEquals(7, kinds.toSet().size)
        assertFalse(null in kinds)
        assertFalse(MediaKind.Video in kinds)
        CanvasArtMediaTypes.forEach { assertEquals(it.folderName, it.toMediaKind()?.folderName) }
    }

    @Test
    fun `logical modes are not offered and have no kind`() {
        listOf(GameImageType.None, GameImageType.All, GameImageType.Description).forEach {
            assertFalse(it in CanvasArtMediaTypes)
            assertNull(it.toMediaKind())
            assertNull(canvasArtMediaLabel(it))
        }
    }

    @Test
    fun `the chosen art wins when the game has it`() {
        val media = mapOf(MediaKind.Screenshot to "shot", MediaKind.Cover to "cover")
        assertEquals("shot", canvasArtPath(GameImageType.Screenshots, media, "default", null))
    }

    @Test
    fun `missing art falls back to the selection's own art`() {
        val media = mapOf(MediaKind.Cover to "cover")
        assertEquals("default", canvasArtPath(GameImageType.Fanart, media, "default", null))
    }

    @Test
    fun `with no default art any still image is used, never a video`() {
        val media = mapOf(MediaKind.Video to "video", MediaKind.PhysicalMedia to "disc")
        assertEquals("disc", canvasArtPath(GameImageType.Fanart, media, null, null))
        assertNull(canvasArtPath(GameImageType.Fanart, mapOf(MediaKind.Video to "video"), null, null))
    }

    @Test
    fun `a system prefers its logo for marquee and its background otherwise`() {
        assertEquals("logo", canvasArtPath(GameImageType.Marquee, emptyMap(), "background", "logo"))
        assertEquals("background", canvasArtPath(GameImageType.Covers, emptyMap(), "background", "logo"))
        assertEquals("logo", canvasArtPath(GameImageType.Covers, emptyMap(), null, "logo"))
    }

    @Test
    fun `blank paths count as missing`() {
        assertEquals("default", canvasArtPath(GameImageType.Fanart, mapOf(MediaKind.Fanart to ""), "default", null))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `a legacy tile keeps its type until a new one is previewed`() {
        val legacy = CanvasItem.EsdeArtItem("e", artType = EsdeArtType.LOGO)
        assertEquals(GameImageType.Marquee, legacy.resolvedImageType)
        val preview = CanvasArtPreview("e", GameImageType.Covers)
        assertEquals(GameImageType.Covers, (legacy.withArtPreview(preview) as CanvasItem.EsdeArtItem).resolvedImageType)
        assertEquals(legacy, legacy.withArtPreview(CanvasArtPreview("other", GameImageType.Covers)))
        assertEquals(legacy, legacy.withArtPreview(null))
    }

    @Test
    fun `switching to marquee saves fit with the type`() {
        val tile = CanvasItem.EsdeArtItem("e", imageType = GameImageType.Fanart, contentScale = EsdeContentScale.CROP)
        val saved = tile.withMediaType(GameImageType.Marquee)
        assertEquals(GameImageType.Marquee, saved.imageType)
        assertEquals(EsdeContentScale.FIT, saved.contentScale)
    }

    @Test
    fun `previewing marquee shows fit and clearing the preview restores type and scale`() {
        val tile = CanvasItem.EsdeArtItem("e", imageType = GameImageType.Fanart, contentScale = EsdeContentScale.CROP)
        val previewed = tile.withArtPreview(CanvasArtPreview("e", GameImageType.Marquee)) as CanvasItem.EsdeArtItem
        assertEquals(EsdeContentScale.FIT, previewed.resolvedContentScale)
        assertEquals(tile, tile.withArtPreview(null))
    }

    @Test
    fun `leaving marquee keeps fit`() {
        val marquee = CanvasItem.EsdeArtItem("e", imageType = GameImageType.Fanart, contentScale = EsdeContentScale.CROP)
            .withMediaType(GameImageType.Marquee)
        assertEquals(EsdeContentScale.FIT, marquee.withMediaType(GameImageType.Covers).contentScale)
    }

    @Test
    fun `keeping marquee respects a crop the user chose`() {
        val tile = CanvasItem.EsdeArtItem("e", imageType = GameImageType.Marquee, contentScale = EsdeContentScale.CROP)
        assertEquals(EsdeContentScale.CROP, tile.withMediaType(GameImageType.Marquee).contentScale)
    }

    @Test
    fun `a preview settles once saved or when its tile is gone`() {
        val preview = CanvasArtPreview("e", GameImageType.Covers)
        assertFalse(preview.isSettled(listOf(CanvasItem.EsdeArtItem("e", imageType = GameImageType.Fanart))))
        assertTrue(preview.isSettled(listOf(CanvasItem.EsdeArtItem("e", imageType = GameImageType.Covers))))
        assertTrue(preview.isSettled(emptyList()))
    }

    @Test
    fun `the art tile menu offers media type, scale, then transparent background`() {
        val actions = canvasTileActions(CanvasItem.EsdeArtItem("e"), true, null, false)
        assertEquals(
            listOf(
                CanvasTileAction.Move,
                CanvasTileAction.MediaType,
                CanvasTileAction.Scale,
                CanvasTileAction.TransparentBackground,
                CanvasTileAction.Remove,
            ),
            actions,
        )
    }

    @Test
    fun `only the art tile offers media type`() {
        listOf(CanvasItem.RomItem("r", "key"), CanvasItem.LastPlayedItem("n")).forEach { item ->
            assertFalse(CanvasTileAction.MediaType in canvasTileActions(item, true, null, true))
        }
    }
}
