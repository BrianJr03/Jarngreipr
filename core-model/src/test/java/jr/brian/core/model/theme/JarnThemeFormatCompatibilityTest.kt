package jr.brian.core.model.theme

import jr.brian.core.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the schema 1 file format so themes players made or shared keep loading. */
class JarnThemeFormatCompatibilityTest {

    private val schema1 = """
        {"schema":1,"meta":{"name":"Golden","author":"Jarn","version":"1.0","license":"CC-BY-4.0","source":"es-de"},
        "tokens":{"colors":{"bg":"#141211","surface":"#1F1B19","raised":"#2B2522","textHigh":"#F6EFE8",
        "textMid":"#C9BDB2","textLow":"#A39689","accent":"#FF8B3D","danger":"#FF7A6B","success":"#7FD99A",
        "line":"#4A403A","focus":"#80FFB070"},
        "type":{"display":{"size":60.0,"lineHeight":62.0,"weight":700,"font":"fonts/display.ttf"},"caption":{"size":13.0}},
        "radius":{"sm":4.0,"md":8.0,"lg":12.0,"xl":20.0}},
        "views":{"systems":{"primary":{"type":"shelf","box":{"x":0.0,"y":0.3,"width":1.0,"height":0.4},
        "art":{"type":"system","kind":"logo"},"axis":"vertical","itemsBefore":3,"itemsAfter":1,"focusedScale":1.4,"showTitle":false},
        "layers":[
        {"type":"picture","id":"bg","rect":{"x":0.0,"y":0.0,"width":1.0,"height":1.0},"z":-1,"opacity":0.5,
        "source":{"type":"system","kind":"background"},"fit":"crop","corner":"lg"},
        {"type":"text","id":"count","rect":{"x":0.5,"y":0.9,"width":0.4,"height":0.05,"originX":0.5,"originY":0.5},"z":2,
        "content":{"type":"system","field":"gameCount"},"style":"caption","color":"textMid","align":"center","maxLines":2},
        {"type":"panel","id":"plate","rect":{"x":0.1,"y":0.8,"width":0.8,"height":0.15},"z":1,"color":"raised","corner":"xl","outlined":true}
        ]},
        "games":{"primary":{"type":"grid","box":{"x":0.05,"y":0.1,"width":0.6,"height":0.85},
        "art":{"type":"media","kind":"Miximage"},"columns":4,"itemAspect":1.0,"spacing":0.02,"showTitle":true},
        "layers":[
        {"type":"video","id":"clip","rect":{"x":0.7,"y":0.1,"width":0.25,"height":0.3},"delayMs":800,"audio":true,
        "fallback":{"type":"file","path":"art/fallback.png"},"corner":"md"},
        {"type":"rating","id":"stars","rect":{"x":0.7,"y":0.45,"width":0.2,"height":0.04},"color":"success"},
        {"type":"date","id":"played","rect":{"x":0.7,"y":0.5,"width":0.25,"height":0.04},"field":"lastPlayed",
        "format":"relative","style":"body","color":"textLow","align":"end"},
        {"type":"text","id":"maker","rect":{"x":0.7,"y":0.55,"width":0.25,"height":0.04},
        "visibleWhen":{"hasMedia":"Marquee","hasText":"developer","systems":["snes","n64"]},
        "content":{"type":"static","text":"Made by"}}
        ]}}}
    """.trimIndent().replace("\n", "")

    private fun decodeGolden(): JarnThemeSpec {
        val result = JarnThemeFormat.decode(schema1)
        assertTrue("Expected Decoded, got $result", result is JarnThemeDecodeResult.Decoded)
        return (result as JarnThemeDecodeResult.Decoded).spec
    }

    @Test
    fun `schema 1 decodes every primary field and layer type`() {
        val spec = decodeGolden()

        assertEquals(1, spec.schema)
        assertEquals(ThemeSource.EsDe, spec.meta.source)
        assertEquals(ThemeColor(0x80FFB070.toInt()), spec.tokens.colors.focus)
        assertEquals("fonts/display.ttf", spec.tokens.type.display?.font)
        assertEquals(null, spec.tokens.type.title)
        assertEquals(ThemeRadius(sm = 4f, md = 8f, lg = 12f, xl = 20f), spec.tokens.radius)

        val systems = spec.views[ThemeView.Systems]!!
        val shelf = systems.primary as ThemePrimary.Shelf
        assertEquals(ThemeAxis.Vertical, shelf.axis)
        assertEquals(ThemePicture.SystemArt(SystemArtKind.Logo), shelf.art)
        assertEquals(
            listOf(ThemeLayer.Picture::class, ThemeLayer.Text::class, ThemeLayer.Panel::class),
            systems.layers.map { it::class },
        )

        val games = spec.views[ThemeView.Games]!!
        assertEquals(ThemePicture.GameMedia(MediaKind.Miximage), (games.primary as ThemePrimary.Grid).art)
        assertEquals(
            listOf(ThemeLayer.Video::class, ThemeLayer.Rating::class, ThemeLayer.Date::class, ThemeLayer.Text::class),
            games.layers.map { it::class },
        )
        val maker = games.layers.last() as ThemeLayer.Text
        assertEquals(ThemeVisibleWhen(MediaKind.Marquee, GameTextField.Developer, listOf("snes", "n64")), maker.visibleWhen)
    }

    @Test
    fun `schema 1 re-encodes byte for byte`() {
        assertEquals(schema1, JarnThemeFormat.encode(decodeGolden()))
    }

    /** Added with #568: a schema 1 theme with a narrow screen patch. The golden above stays as it was. */
    private val schema1Narrow = """
        {"schema":1,"meta":{"name":"Narrow"},"tokens":{"colors":{"bg":"#000000","surface":"#111111",
        "raised":"#222222","textHigh":"#FFFFFF","textMid":"#DDDDDD","textLow":"#BBBBBB",
        "accent":"#FF8800","danger":"#FF4444","success":"#44FF44"}},
        "views":{"games":{"primary":{"type":"grid","box":{"x":0.05,"y":0.1,"width":0.6,"height":0.8},"columns":4},
        "layers":[{"type":"panel","id":"rail","rect":{"x":0.7,"y":0.1,"width":0.25,"height":0.8}},
        {"type":"panel","id":"strip","rect":{"x":0.0,"y":0.9,"width":1.0,"height":0.1}}],
        "narrow":{"primary":{"box":{"x":0.05,"y":0.1,"width":0.5,"height":0.8},"columns":3},
        "layers":[{"type":"panel","id":"rail","rect":{"x":0.6,"y":0.1,"width":0.35,"height":0.8},"corner":"lg"}],
        "hide":["strip"]}}}}
    """.trimIndent().replace("\n", "")

    @Test
    fun `schema 1 narrow patch decodes and re-encodes byte for byte`() {
        val spec = (JarnThemeFormat.decode(schema1Narrow) as JarnThemeDecodeResult.Decoded).spec
        val games = spec.views[ThemeView.Games]!!

        assertEquals(1, spec.schema)
        assertEquals(3, games.narrow?.primary?.columns)
        assertEquals(listOf("strip"), games.narrow?.hide)
        assertEquals(ThemeCorner.Lg, (games.narrow?.layers?.single() as ThemeLayer.Panel).corner)
        assertEquals(schema1Narrow, JarnThemeFormat.encode(spec))
    }

    /** Added with #569: picture fallbacks, text vertical alignment and stacks, all in schema 1. */
    private val schema1Stacks = """
        {"schema":1,"meta":{"name":"Stacks"},"tokens":{"colors":{"bg":"#000000","surface":"#111111",
        "raised":"#222222","textHigh":"#FFFFFF","textMid":"#DDDDDD","textLow":"#BBBBBB",
        "accent":"#FF8800","danger":"#FF4444","success":"#44FF44"}},
        "views":{"games":{"primary":{"type":"list","box":{"x":0.05,"y":0.1,"width":0.4,"height":0.8}},
        "layers":[{"type":"picture","id":"art","rect":{"x":0.5,"y":0.1,"width":0.45,"height":0.4},
        "source":{"type":"media","kind":"Miximage"},"fallbacks":[{"type":"media","kind":"Screenshot"},{"type":"file","path":"art/none.png"}]},
        {"type":"video","id":"clip","rect":{"x":0.5,"y":0.1,"width":0.45,"height":0.4},"fallbacks":[{"type":"media","kind":"Fanart"}]},
        {"type":"text","id":"title","rect":{"x":0.5,"y":0.55,"width":0.45,"height":0.1},
        "content":{"type":"game","field":"title"},"verticalAlign":"start","stack":"info"},
        {"type":"rating","id":"stars","rect":{"x":0.5,"y":0.66,"width":0.2,"height":0.05},"stack":"info"},
        {"type":"date","id":"year","rect":{"x":0.75,"y":0.66,"width":0.2,"height":0.05},"field":"releaseDate",
        "verticalAlign":"end","stack":"info"}]}}}
    """.trimIndent().replace("\n", "")

    @Test
    fun `schema 1 fallbacks and stacks decode and re-encode byte for byte`() {
        val spec = (JarnThemeFormat.decode(schema1Stacks) as JarnThemeDecodeResult.Decoded).spec
        val layers = spec.views[ThemeView.Games]!!.layers

        assertEquals(ThemePicture.GameMedia(MediaKind.Screenshot), (layers[0] as ThemeLayer.Picture).fallbacks.first())
        assertEquals(listOf(ThemePicture.GameMedia(MediaKind.Fanart)), (layers[1] as ThemeLayer.Video).fallbacks)
        assertEquals(ThemeAlign.Start, (layers[2] as ThemeLayer.Text).verticalAlign)
        assertEquals(listOf("info", "info", "info"), layers.drop(2).map { it.stack })
        assertEquals(schema1Stacks, JarnThemeFormat.encode(spec))
    }

    @Test
    fun `the golden schema 1 theme keeps its defaults for fallbacks and stacks`() {
        val games = decodeGolden().views[ThemeView.Games]!!
        assertEquals(emptyList<ThemePicture>(), (games.layers.first() as ThemeLayer.Video).fallbacks)
        assertEquals(ThemeAlign.Center, (games.layers.last() as ThemeLayer.Text).verticalAlign)
        assertEquals(null, games.layers.last().stack)
    }

    /** Added with #577: a layer shown only when the system has no logo. */
    private val schema1SystemArt = """
        {"schema":1,"meta":{"name":"Logo or name"},"tokens":{"colors":{"bg":"#000000","surface":"#111111",
        "raised":"#222222","textHigh":"#FFFFFF","textMid":"#DDDDDD","textLow":"#BBBBBB",
        "accent":"#FF8800","danger":"#FF4444","success":"#44FF44"}},
        "views":{"systems":{"primary":{"type":"shelf","box":{"x":0.0,"y":0.5,"width":1.0,"height":0.4}},
        "layers":[{"type":"picture","id":"logo","rect":{"x":0.05,"y":0.1,"width":0.4,"height":0.2},
        "visibleWhen":{"hasSystemArt":"logo"},"source":{"type":"system","kind":"logo"}},
        {"type":"text","id":"name","rect":{"x":0.05,"y":0.1,"width":0.4,"height":0.2},
        "visibleWhen":{"hasSystemArt":"logo","missing":true},"content":{"type":"system","field":"title"}}]}}}
    """.trimIndent().replace("\n", "")

    @Test
    fun `schema 1 system art visibility decodes and re-encodes byte for byte`() {
        val spec = (JarnThemeFormat.decode(schema1SystemArt) as JarnThemeDecodeResult.Decoded).spec
        val name = spec.views[ThemeView.Systems]!!.layers.last()

        assertEquals(ThemeVisibleWhen(hasSystemArt = SystemArtKind.Logo, missing = true), name.visibleWhen)
        assertEquals(schema1SystemArt, JarnThemeFormat.encode(spec))
    }

    /** Added with #578: panels fading at an edge. */
    private val schema1Fade = """
        {"schema":1,"meta":{"name":"Fades"},"tokens":{"colors":{"bg":"#000000","surface":"#111111",
        "raised":"#222222","textHigh":"#FFFFFF","textMid":"#DDDDDD","textLow":"#BBBBBB",
        "accent":"#FF8800","danger":"#FF4444","success":"#44FF44"}},
        "views":{"games":{"primary":{"type":"shelf","box":{"x":0.0,"y":0.5,"width":1.0,"height":0.4}},
        "layers":[{"type":"panel","id":"plate","rect":{"x":0.0,"y":0.4,"width":1.0,"height":0.6},"fade":{"edge":"top","length":0.3}},
        {"type":"panel","id":"side","rect":{"x":0.0,"y":0.0,"width":0.3,"height":1.0},"outlined":true,"fade":{"edge":"end"}}]}}}
    """.trimIndent().replace("\n", "")

    @Test
    fun `schema 1 panel fades decode and re-encode byte for byte`() {
        val spec = (JarnThemeFormat.decode(schema1Fade) as JarnThemeDecodeResult.Decoded).spec
        val panels = spec.views[ThemeView.Games]!!.layers.map { it as ThemeLayer.Panel }

        assertEquals(ThemeFade(ThemeEdge.Top, 0.3f), panels[0].fade)
        assertEquals(ThemeFade(ThemeEdge.End, ThemeFade.DEFAULT_LENGTH), panels[1].fade)
        assertEquals(null, (decodeGolden().views[ThemeView.Systems]!!.layers.last() as ThemeLayer.Panel).fade)
        assertEquals(schema1Fade, JarnThemeFormat.encode(spec))
    }

    @Test
    fun `the golden schema 1 theme has no narrow patch`() {
        assertEquals(null, decodeGolden().views[ThemeView.Systems]?.narrow)
        assertEquals(null, decodeGolden().views[ThemeView.Games]?.narrow)
    }

    @Test
    fun `minimal schema 1 theme keeps unchanged defaults`() {
        val minimal = """
            {"schema":1,"meta":{"name":"Min"},"tokens":{"colors":{"bg":"#000000","surface":"#111111",
            "raised":"#222222","textHigh":"#FFFFFF","textMid":"#DDDDDD","textLow":"#BBBBBB",
            "accent":"#FF8800","danger":"#FF4444","success":"#44FF44"}}}
        """.trimIndent().replace("\n", "")

        val spec = (JarnThemeFormat.decode(minimal) as JarnThemeDecodeResult.Decoded).spec

        assertEquals(ThemeSource.Jarn, spec.meta.source)
        assertEquals(null, spec.tokens.colors.line)
        assertEquals(null, spec.tokens.colors.focus)
        assertEquals(ThemeTypeScale(), spec.tokens.type)
        assertEquals(ThemeRadius(sm = 8f, md = 12f, lg = 16f, xl = 24f), spec.tokens.radius)
        assertEquals(ThemeViews(), spec.views)
        assertEquals(minimal, JarnThemeFormat.encode(spec))
    }

    @Test
    fun `layer defaults stay unchanged`() {
        val layer = ThemeLayer.Video(id = "v", rect = ThemeRect(0f, 0f, 1f, 1f))

        assertEquals(0, layer.z)
        assertEquals(1f, layer.opacity)
        assertEquals(null, layer.visibleWhen)
        assertEquals(ThemePicture.GameMedia(MediaKind.Screenshot), layer.fallback)
        assertEquals(ThemeCorner.None, layer.corner)
    }

    @Test
    fun `media kinds keep their stored names`() {
        assertEquals(
            listOf("Cover", "Marquee", "Fanart", "Screenshot", "Titlescreen", "Miximage", "PhysicalMedia", "Video"),
            MediaKind.entries.map { kind ->
                JarnThemeFormat.json.encodeToString(MediaKind.serializer(), kind).trim('"')
            },
        )
    }
}
