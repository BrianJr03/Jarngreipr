package jr.brian.frontend.look.themes

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeSystemArt
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeFileRefsTest {
    private val assets = mapOf("art/snes.svg" to ThemeAssetKind.Image, "art/bg.png" to ThemeAssetKind.Image)

    @Test
    fun `system art keeps only files that passed`() {
        val spec = testSpec().copy(
            systemArt = mapOf(
                "snes" to ThemeSystemArt(logo = "art/snes.svg", background = "art/missing.png"),
                "nes" to ThemeSystemArt(logo = "../escape.png"),
                "n64" to ThemeSystemArt(background = "art/bg.png"),
            ),
        )
        val checked = checkFileRefs(spec, assets)
        assertEquals(
            mapOf("snes" to ThemeSystemArt(logo = "art/snes.svg"), "n64" to ThemeSystemArt(background = "art/bg.png")),
            checked.spec.systemArt,
        )
        assertEquals(2, checked.dropped)
    }

    @Test
    fun `picture and video fallbacks drop bad files one by one`() {
        val rect = ThemeRect(0f, 0f, 1f, 1f)
        val good = ThemePicture.File("art/bg.png")
        val bad = ThemePicture.File("art/nope.png")
        val media = ThemePicture.GameMedia(MediaKind.Fanart)
        val view = ViewSpec(
            primary = ThemePrimary.Grid(rect),
            layers = listOf(
                ThemeLayer.Picture(id = "p", rect = rect, source = media, fallbacks = listOf(bad, good)),
                ThemeLayer.Video(id = "v", rect = rect, fallbacks = listOf(good, bad)),
            ),
        )
        val checked = checkFileRefs(testSpec().copy(views = ThemeViews(games = view)), assets)
        val layers = checked.spec.views.games!!.layers
        assertEquals(listOf(good), (layers[0] as ThemeLayer.Picture).fallbacks)
        assertEquals(listOf(good), (layers[1] as ThemeLayer.Video).fallbacks)
        assertEquals(2, checked.dropped)
    }
}
