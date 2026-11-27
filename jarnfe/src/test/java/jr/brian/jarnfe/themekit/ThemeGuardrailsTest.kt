package jr.brian.jarnfe.themekit

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val Screen = ThemeScreen(1920, 1080, ThemeInsets(bottom = 40))
private val Full = ThemeRect(0f, 0f, 1f, 1f)
private val PrimaryBox = ThemeRect(0.1f, 0.4f, 0.8f, 0.3f)

private fun view(vararg layers: ThemeLayer, box: ThemeRect = PrimaryBox) =
    ViewSpec(primary = ThemePrimary.Shelf(box), layers = layers.toList())

private fun panel(id: String, rect: ThemeRect = ThemeRect(0f, 0f, 0.2f, 0.1f), z: Int = 0, opacity: Float = 1f) =
    ThemeLayer.Panel(id = id, rect = rect, z = z, opacity = opacity)

private fun video(id: String, fallback: ThemePicture? = ThemePicture.GameMedia(MediaKind.Screenshot)) =
    ThemeLayer.Video(id = id, rect = ThemeRect(0.6f, 0f, 0.4f, 0.3f), fallback = fallback)

private fun text(id: String, rect: ThemeRect) =
    ThemeLayer.Text(id = id, rect = rect, content = ThemeText.Static("x"))

class ThemeGuardrailsTest {

    @Test
    fun `a clean view has no issues`() {
        val placed = resolveThemeView(view(panel("a")), Screen)
        assertEquals(ThemePxRect(192, 432, 1536, 324), placed.primary)
        assertEquals(emptyList<ThemeLayoutIssue>(), placed.issues)
    }

    @Test
    fun `layers split around the primary and sort by z keeping theme order on ties`() {
        val placed = resolveThemeView(
            view(panel("top", z = 3), panel("b1", z = 0), panel("back", z = -2), panel("b2", z = 0), panel("mid", z = 1)),
            Screen,
        )
        assertEquals(listOf("back", "b1", "b2"), placed.below.map { it.layer.id })
        assertEquals(listOf("mid", "top"), placed.above.map { it.layer.id })
    }

    @Test
    fun `only the first video plays and later ones fall back to their picture`() {
        val placed = resolveThemeView(view(video("v1"), video("v2"), video("v3", fallback = null)), Screen)
        val layers = placed.below.map { it.layer }
        assertEquals(listOf("v1", "v2"), layers.map { it.id })
        assertTrue(layers[0] is ThemeLayer.Video)
        assertTrue(layers[1] is ThemeLayer.Picture)
        assertEquals(
            listOf(
                ThemeLayoutIssue.ExtraVideo("v2", "v1", fallbackShown = true),
                ThemeLayoutIssue.ExtraVideo("v3", "v1", fallbackShown = false),
            ),
            placed.issues,
        )
    }

    @Test
    fun `pictures decode at most twice their rect`() {
        val picture = ThemeLayer.Picture(
            id = "p",
            rect = ThemeRect(0f, 0f, 0.25f, 0.25f),
            source = ThemePicture.GameMedia(MediaKind.Cover),
        )
        val placed = resolveThemeView(view(picture, panel("plate")), Screen)
        assertEquals(ThemePxSize(960, 540), placed.below[0].decodeCap)
        assertEquals(null, placed.below[1].decodeCap)
    }

    @Test
    fun `decode cap never exceeds twice the screen`() {
        val picture = ThemeLayer.Picture(
            id = "huge",
            rect = ThemeRect(-1f, -1f, 3f, 3f),
            source = ThemePicture.GameMedia(MediaKind.Fanart),
        )
        val placed = resolveThemeView(view(picture), Screen)
        assertEquals(ThemePxSize(3840, 2160), placed.below.single().decodeCap)
    }

    @Test
    fun `text under the hint bar moves up out of it`() {
        val placed = resolveThemeView(view(text("t", ThemeRect(0f, 0.95f, 0.5f, 0.05f))), Screen)
        assertEquals(ThemePxRect(0, 986, 960, 54), placed.below.single().bounds)
        assertEquals(listOf(ThemeLayoutIssue.TextMoved("t")), placed.issues)
    }

    @Test
    fun `text too big to move out is flagged and kept`() {
        val placed = resolveThemeView(view(text("t", Full)), Screen)
        assertEquals(ThemePxRect(0, 0, 1920, 1080), placed.below.single().bounds)
        assertEquals(listOf(ThemeLayoutIssue.TextInSafeZone("t")), placed.issues)
    }

    @Test
    fun `full bleed decor is flagged but untouched`() {
        val placed = resolveThemeView(view(panel("bg", Full)), Screen)
        assertEquals(ThemePxRect(0, 0, 1920, 1080), placed.below.single().bounds)
        assertEquals(listOf(ThemeLayoutIssue.InSafeZone("bg")), placed.issues)
    }

    @Test
    fun `invalid and off screen layers are dropped`() {
        val placed = resolveThemeView(
            view(panel("zero", ThemeRect(0f, 0f, 0f, 0.1f)), panel("gone", ThemeRect(1.5f, 0f, 0.2f, 0.2f))),
            Screen,
        )
        assertTrue(placed.below.isEmpty())
        assertEquals(listOf(ThemeLayoutIssue.InvalidRect("zero"), ThemeLayoutIssue.OffScreen("gone")), placed.issues)
    }

    @Test
    fun `an opaque layer above the primary that covers it moves below`() {
        val placed = resolveThemeView(view(panel("cover", Full, z = 5), panel("glass", Full, z = 6, opacity = 0.3f)), Screen)
        assertEquals(listOf("cover"), placed.below.map { it.layer.id })
        assertEquals(listOf("glass"), placed.above.map { it.layer.id })
        assertTrue(ThemeLayoutIssue.CoveredPrimary("cover") in placed.issues)
    }

    @Test
    fun `text above the primary is never moved below it`() {
        val placed = resolveThemeView(view(text("t", PrimaryBox).copy(z = 2)), Screen)
        assertEquals(listOf("t"), placed.above.map { it.layer.id })
    }

    @Test
    fun `an off screen primary is clamped into the safe area`() {
        val placed = resolveThemeView(view(box = ThemeRect(0.8f, 0.9f, 0.5f, 0.3f)), Screen)
        assertEquals(ThemePxRect(960, 716, 960, 324), placed.primary)
        assertEquals(listOf(ThemeLayoutIssue.PrimaryClamped), placed.issues)
    }

    @Test
    fun `a tiny primary grows to the minimum`() {
        val placed = resolveThemeView(view(box = ThemeRect(0.5f, 0.5f, 0.01f, 0.01f, 0.5f, 0.5f)), Screen)
        assertEquals(384, placed.primary.width)
        assertEquals(208, placed.primary.height)
        assertEquals(listOf(ThemeLayoutIssue.PrimaryTooSmall), placed.issues)
    }

    @Test
    fun `an invalid primary fills the safe area`() {
        val placed = resolveThemeView(view(box = ThemeRect(0f, 0f, -1f, 0.5f)), Screen)
        assertEquals(Screen.safeArea, placed.primary)
        assertEquals(listOf(ThemeLayoutIssue.PrimaryInvalid), placed.issues)
    }

    @Test
    fun `the 4 by 3 bottom screen keeps its own aspect`() {
        val bottom = ThemeScreen(1024, 768)
        val placed = resolveThemeView(view(), bottom)
        assertEquals(ThemePxRect(102, 307, 820, 231), placed.primary)
    }
}
