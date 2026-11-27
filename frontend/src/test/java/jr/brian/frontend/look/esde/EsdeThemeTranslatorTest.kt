package jr.brian.frontend.look.esde

import android.app.Application
import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.SystemTextField
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeAxis
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemeDateFormat
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeSource
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeVisibleWhen
import jr.brian.frontend.look.esde.EsdeTranslationNote.ExtraPrimary
import jr.brian.frontend.look.esde.EsdeTranslationNote.Hidden
import jr.brian.frontend.look.esde.EsdeTranslationNote.UnsupportedElement
import jr.brian.frontend.look.esde.EsdeTranslationNote.UnsupportedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val HEADER_BAND = 0.2f
private const val EPSILON = 0.0001f

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeThemeTranslatorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var fixture: EsdeThemeFixture
    private val nes = EsdeSystemRef(name = "nes", fullName = "Nintendo Entertainment System")
    private val snes = EsdeSystemRef(name = "snes", fullName = "Super Nintendo")

    private val colors = ThemeColors(
        bg = ThemeColor(0xFF000000.toInt()), surface = ThemeColor(0xFF111111.toInt()),
        raised = ThemeColor(0xFF222222.toInt()), textHigh = ThemeColor(0xFFFFFFFF.toInt()),
        textMid = ThemeColor(0xFFDDDDDD.toInt()), textLow = ThemeColor(0xFFBBBBBB.toInt()),
        accent = ThemeColor(0xFFFF8800.toInt()), danger = ThemeColor(0xFFFF4444.toInt()),
        success = ThemeColor(0xFF44FF44.toInt()),
    )
    private val options = EsdeTranslateOptions(colors, fallbackName = "Folder", systemsTopClear = HEADER_BAND)

    @Before
    fun setUp() {
        fixture = EsdeThemeFixture(temp.newFolder("theme"))
        fixture.capabilities()
    }

    private fun translate(body: String, vararg systems: EsdeSystemRef = arrayOf(nes)): EsdeTranslation {
        fixture.theme(body = body)
        val model = EsdeThemeParser().parse(fixture.root, systems.toList())
        return EsdeThemeTranslator.translate(model, options)
    }

    private fun EsdeTranslation.games() = spec.views.games!!
    private fun EsdeTranslation.systems() = spec.views.systems!!
    private fun EsdeTranslation.notes(list: (EsdeTranslationReport) -> List<EsdeReportItem>) = list(report).mapNotNull { it.note }
    private inline fun <reified T : ThemeLayer> EsdeTranslation.gameLayer(id: String): T =
        games().layers.first { it.id == id } as T

    @Test
    fun `a gamelist carousel and its secondaries keep their layout and bindings`() {
        val result = translate(
            """
            <view name="gamelist">
              <carousel name="gamelistCarousel"><type>horizontal</type><pos>0 0.6</pos><size>1 0.3</size>
                <maxItemCount>5</maxItemCount><itemScale>1.5</itemScale><imageType>marquee</imageType></carousel>
              <image name="cover"><pos>0.75 0.3</pos><origin>0.5 0.5</origin><maxSize>0.2 0.4</maxSize><imageType>cover</imageType></image>
              <text name="description"><pos>0.05 0.1</pos><size>0.4 0.2</size><metadata>description</metadata>
                <fontSize>0.02</fontSize><lineSpacing>1.25</lineSpacing><horizontalAlignment>center</horizontalAlignment></text>
              <rating name="rating"><pos>0.05 0.32</pos><size>0.15 0.04</size></rating>
              <datetime name="lastplayed"><pos>0.05 0.37</pos><size>0.2 0.04</size><metadata>lastplayed</metadata></datetime>
              <video name="clip"><pos>0.5 0.05</pos><size>0.3 0.3</size><delay>1.5</delay><imageType>screenshot</imageType></video>
            </view>
            """,
        )

        val shelf = result.games().primary as ThemePrimary.Shelf
        assertEquals(ThemeAxis.Horizontal, shelf.axis)
        assertEquals(2, shelf.itemsBefore)
        assertEquals(1.5f, shelf.focusedScale, EPSILON)
        assertEquals(ThemePicture.GameMedia(MediaKind.Marquee), shelf.art)
        assertEquals(0.6f, shelf.box.y, EPSILON)

        val cover = result.gameLayer<ThemeLayer.Picture>("image-cover")
        assertEquals(ThemePicture.GameMedia(MediaKind.Cover), cover.source)
        assertEquals(ThemeFit.Fit, cover.fit)
        assertEquals(-20, cover.z)
        assertEquals(0.5f, cover.rect.originX, EPSILON)

        val description = result.gameLayer<ThemeLayer.Text>("text-description")
        assertEquals(ThemeText.Game(GameTextField.Description), description.content)
        assertEquals(ThemeAlign.Center, description.align)
        assertEquals(8, description.maxLines)

        result.gameLayer<ThemeLayer.Rating>("rating-rating")
        val played = result.gameLayer<ThemeLayer.Date>("datetime-lastplayed")
        assertEquals(ThemeDateField.LastPlayed, played.field)
        assertEquals(ThemeDateFormat.Relative, played.format)

        val clip = result.gameLayer<ThemeLayer.Video>("video-clip")
        assertEquals(1500, clip.delayMs)
        assertEquals(ThemePicture.GameMedia(MediaKind.Screenshot), clip.fallback)

        assertEquals(6, result.report.translated.size)
        assertEquals(listOf(-20, -20, -10, -10, -5).sorted(), result.games().layers.map { it.z }.sorted())
    }

    @Test
    fun `wheels become shelves and vertical carousels keep their axis`() {
        val result = translate(
            fixture.element("gamelist", "carousel", "wheel", "type" to "verticalWheel") +
                fixture.element("system", "carousel", "systems", "type" to "vertical", "pos" to "0 0.3", "size" to "0.3 0.6"),
        )

        assertEquals(ThemeAxis.Vertical, (result.games().primary as ThemePrimary.Shelf).axis)
        assertEquals(ThemeAxis.Vertical, (result.systems().primary as ThemePrimary.Shelf).axis)
        assertEquals(listOf(EsdeTranslationNote.WheelAsShelf), result.notes { it.adjusted })
    }

    @Test
    fun `grids and text lists keep their shape`() {
        val result = translate(
            fixture.element(
                "gamelist", "grid", "grid", "pos" to "0 0.1", "size" to "0.6 0.8",
                "itemSize" to "0.1 0.2", "itemSpacing" to "0.02 0.02", "imageType" to "3dbox, cover",
            ) +
                fixture.element(
                    "system", "textlist", "list", "pos" to "0.1 0.3", "size" to "0.4 0.6",
                    "fontSize" to "0.04", "lineSpacing" to "1.5", "horizontalAlignment" to "right",
                ),
        )

        val grid = result.games().primary as ThemePrimary.Grid
        assertEquals(5, grid.columns)
        assertEquals(0.1f * 16f / 9f / 0.2f, grid.itemAspect, EPSILON)
        assertEquals(ThemePicture.GameMedia(MediaKind.Cover), grid.art)

        val list = result.systems().primary as ThemePrimary.List
        assertEquals(10, list.rows)
        assertEquals(ThemeAlign.End, list.align)
    }

    @Test
    fun `per system files and names map onto Jarn's own system art and fields`() {
        val result = translate(
            """
            <view name="system">
              <carousel name="systems"><pos>0 0.4</pos><size>1 0.3</size><staticImage>./logos/${'$'}{system.theme}.svg</staticImage></carousel>
              <image name="background"><size>1 1</size><path>./bg/${'$'}{system.theme}.jpg</path></image>
              <text name="title"><pos>0.1 0.75</pos><size>0.5 0.05</size><text>${'$'}{system.fullName}</text></text>
              <text name="count"><pos>0.1 0.8</pos><size>0.5 0.05</size><systemdata>gamecount</systemdata></text>
              <text name="tagline"><pos>0.1 0.85</pos><size>0.5 0.05</size><text>Play on</text></text>
            </view>
            """,
            nes, snes,
        )

        val systems = result.systems()
        assertEquals(ThemePicture.SystemArt(SystemArtKind.Logo), systems.primary.art)
        val layers = systems.layers.associateBy { it.id }
        assertEquals(ThemePicture.SystemArt(SystemArtKind.Background), (layers.getValue("image-background") as ThemeLayer.Picture).source)
        assertEquals(ThemeText.System(SystemTextField.Title), (layers.getValue("text-title") as ThemeLayer.Text).content)
        assertEquals(ThemeText.System(SystemTextField.GameCount), (layers.getValue("text-count") as ThemeLayer.Text).content)
        assertEquals(ThemeText.Static("Play on"), (layers.getValue("text-tagline") as ThemeLayer.Text).content)
        assertTrue(result.assets.isEmpty())
    }

    @Test
    fun `elements only some systems have show only on those`() {
        fixture.theme("snes/theme.xml", body = fixture.element("system", "carousel", "c", "pos" to "0 0.4", "size" to "1 0.3") +
            fixture.element("system", "text", "only", "pos" to "0 0.8", "size" to "0.2 0.05", "text" to "Hi"))
        val result = translate(fixture.element("system", "carousel", "c", "pos" to "0 0.4", "size" to "1 0.3"), nes, snes)

        val only = result.systems().layers.single() as ThemeLayer.Text
        assertEquals(ThemeVisibleWhen(systems = listOf("snes")), only.visibleWhen)
        assertTrue(EsdeTranslationNote.ShownOnSomeSystems in result.notes { it.adjusted })
    }

    @Test
    fun `the systems view keeps the header band clear but backdrops stay`() {
        val result = translate(
            fixture.element("system", "carousel", "c", "pos" to "0 0.05", "size" to "1 0.5") +
                fixture.element("system", "image", "bg", "size" to "1 1", "path" to "./bg.jpg") +
                fixture.element("system", "text", "label", "pos" to "0.1 0.02", "size" to "0.3 0.05", "text" to "Hi"),
        )

        val systems = result.systems()
        assertEquals(HEADER_BAND, systems.primary.box.top(), EPSILON)
        assertEquals(0.55f, systems.primary.box.top() + systems.primary.box.height, EPSILON)
        assertEquals(0f, systems.layers.first { it.id == "image-bg" }.rect.y, EPSILON)
        assertEquals(HEADER_BAND, systems.layers.first { it.id == "text-label" }.rect.top(), EPSILON)
        assertEquals(listOf("bg.jpg"), result.assets)
    }

    @Test
    fun `games view keeps its top band`() {
        val result = translate(fixture.element("gamelist", "carousel", "c", "pos" to "0 0.05", "size" to "1 0.5"))

        assertEquals(0.05f, result.games().primary.box.y, EPSILON)
    }

    @Test
    fun `what Jarn can't draw is listed as not used`() {
        val result = translate(
            """
            <view name="gamelist">
              <textlist name="list"><pos>0 0</pos><size>0.5 1</size><selectedColor>FF8800</selectedColor></textlist>
              <grid name="second"><pos>0.5 0</pos><size>0.5 1</size></grid>
              <animation name="spin"><path>./a.gif</path></animation>
              <image name="box"><imageType>3dbox</imageType></image>
              <image name="gone"><visible>false</visible><imageType>cover</imageType></image>
              <gamelistinfo name="info"/>
            </view>
            """,
        )

        assertEquals(
            listOf(ExtraPrimary, UnsupportedElement, UnsupportedMedia, Hidden, UnsupportedElement, EsdeTranslationNote.TransitionsAndTriggers),
            result.notes { it.notUsed },
        )
        assertEquals(
            listOf(EsdeColorCandidate(EsdeView.Gamelist, EsdeElementKey("textlist", "list"), "selectedColor", "FF8800")),
            result.colorCandidates,
        )
        assertTrue(result.games().layers.isEmpty())
    }

    @Test
    fun `elements left empty by escaped paths or unknown values are skipped and listed`() {
        val result = translate(
            fixture.element("gamelist", "carousel", "c", "color" to "\${missing}") +
                fixture.element("gamelist", "image", "escaped", "path" to "../../outside.png") +
                fixture.element("gamelist", "rating", "blank", "color" to "\${missing}"),
        )

        assertTrue(result.games().layers.isEmpty())
        assertEquals(
            listOf(EsdeTranslationNote.NothingReadable, EsdeTranslationNote.NothingReadable),
            result.report.notUsed.filter { it.view == EsdeView.Gamelist }.map { it.note },
        )
        assertTrue(result.colorCandidates.isEmpty())
    }

    @Test
    fun `a view with no primary keeps Jarn's layout`() {
        val result = translate(fixture.element("gamelist", "image", "bg", "path" to "./bg.png"))

        assertNull(result.spec.views.games)
        assertTrue(EsdeTranslationNote.NoPrimary in result.notes { it.notUsed })
    }

    @Test
    fun `a legacy theme translates to an empty theme without throwing`() {
        val result = translate("<formatVersion>7</formatVersion>")

        assertNull(result.spec.views.systems)
        assertNull(result.spec.views.games)
        assertEquals("Fixture", result.spec.meta.name)
        assertEquals(ThemeSource.EsDe, result.spec.meta.source)
        assertEquals(listOf(EsdeTranslationNote.LegacyTheme), result.notes { it.notUsed })
    }

    @Test
    fun `translated specs survive the theme file format`() {
        val result = translate(
            fixture.element("gamelist", "carousel", "c", "imageType" to "cover") +
                fixture.element("gamelist", "image", "art", "pos" to "0.6 0.1", "size" to "0.3 0.3", "path" to "./art/x.png", "tile" to "true"),
        )

        val decoded = JarnThemeFormat.decode(JarnThemeFormat.encode(result.spec))
        assertEquals(JarnThemeDecodeResult.Decoded(result.spec), decoded)
        assertEquals(ThemeFit.Crop, result.gameLayer<ThemeLayer.Picture>("image-art").fit)
        assertEquals(listOf("art/x.png"), result.assets)
    }
}
