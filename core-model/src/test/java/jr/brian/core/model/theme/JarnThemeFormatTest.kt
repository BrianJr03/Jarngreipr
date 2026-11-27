package jr.brian.core.model.theme

import jr.brian.core.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnThemeFormatTest {

    private val colors = ThemeColors(
        bg = ThemeColor(0xFF000000.toInt()),
        surface = ThemeColor(0xFF111111.toInt()),
        raised = ThemeColor(0xFF222222.toInt()),
        textHigh = ThemeColor(0xFFFFFFFF.toInt()),
        textMid = ThemeColor(0xFFDDDDDD.toInt()),
        textLow = ThemeColor(0xFFBBBBBB.toInt()),
        accent = ThemeColor(0xFFFF8800.toInt()),
        danger = ThemeColor(0xFFFF4444.toInt()),
        success = ThemeColor(0xFF44FF44.toInt()),
    )

    private val full = JarnThemeSpec(
        meta = ThemeMeta(name = "Round trip", author = "Jarn"),
        tokens = ThemeTokens(
            colors = colors.copy(line = ThemeColor(0x33FFFFFF)),
            type = ThemeTypeScale(title = ThemeTextStyle(size = 30f, weight = 600)),
            radius = ThemeRadius(md = 6f),
        ),
        views = ThemeViews(
            systems = ViewSpec(
                primary = ThemePrimary.List(
                    box = ThemeRect(0.1f, 0.1f, 0.4f, 0.8f),
                    rows = 8,
                    align = ThemeAlign.End,
                    secondary = ThemeText.Game(GameTextField.Genre),
                ),
            ),
            games = ViewSpec(
                primary = ThemePrimary.Shelf(box = ThemeRect(0f, 0.6f, 1f, 0.3f), art = ThemePicture.GameMedia(MediaKind.Cover)),
                layers = listOf(
                    ThemeLayer.Picture(
                        id = "hero",
                        rect = ThemeRect(0f, 0f, 1f, 0.6f),
                        z = -2,
                        source = ThemePicture.GameMedia(MediaKind.Fanart),
                        fit = ThemeFit.Crop,
                    ),
                    ThemeLayer.Text(
                        id = "title",
                        rect = ThemeRect(0.5f, 0.5f, 0.8f, 0.1f, originX = 0.5f, originY = 0.5f),
                        content = ThemeText.Game(GameTextField.Title),
                        style = ThemeTextRole.Title,
                    ),
                    ThemeLayer.Video(id = "clip", rect = ThemeRect(0.6f, 0f, 0.4f, 0.4f), fallback = null),
                ),
            ),
        ),
    )

    private fun decoded(text: String): JarnThemeSpec {
        val result = JarnThemeFormat.decode(text)
        assertTrue("Expected Decoded, got $result", result is JarnThemeDecodeResult.Decoded)
        return (result as JarnThemeDecodeResult.Decoded).spec
    }

    @Test
    fun `full theme round trips`() {
        assertEquals(full, decoded(JarnThemeFormat.encode(full)))
    }

    @Test
    fun `minimal theme round trips and always writes its schema`() {
        val minimal = JarnThemeSpec(meta = ThemeMeta(name = "Min"), tokens = ThemeTokens(colors))
        val text = JarnThemeFormat.encode(minimal)

        assertTrue(text.startsWith("""{"schema":1,"""))
        assertEquals(minimal, decoded(text))
    }

    @Test
    fun `newer schema is refused with the versions`() {
        val newer = JarnThemeFormat.encode(full).replaceFirst(""""schema":1""", """"schema":7""")

        assertEquals(JarnThemeDecodeResult.TooNew(schema = 7, supported = 1), JarnThemeFormat.decode(newer))
    }

    @Test
    fun `newer schema is refused before its body is read`() {
        assertEquals(
            JarnThemeDecodeResult.TooNew(schema = 2, supported = 1),
            JarnThemeFormat.decode("""{"schema":2,"layout":"something else entirely"}"""),
        )
    }

    @Test
    fun `unknown keys are ignored`() {
        val extra = JarnThemeFormat.encode(full)
            .replaceFirst(""""meta":{""", """"capabilities":{"variants":["a"]},"meta":{"homepage":"x",""")
            .replaceFirst(""""views":{""", """"views":{"now":{"primary":{"type":"shelf"}},""")

        assertEquals(full, decoded(extra))
    }

    @Test
    fun `unknown layer types are dropped and known ones kept`() {
        val text = JarnThemeFormat.encode(full).replaceFirst(
            """"layers":[""",
            """"layers":[{"type":"clock","id":"c","rect":{"x":0.0,"y":0.0,"width":0.1,"height":0.1}},""",
        )

        assertEquals(full, decoded(text))
    }

    @Test
    fun `damaged files are invalid, not thrown`() {
        listOf(
            "",
            "not json",
            "[]",
            """{"meta":{"name":"No schema"}}""",
            """{"schema":"1"}""",
            """{"schema":0}""",
            """{"schema":1,"meta":{"name":"No tokens"}}""",
            JarnThemeFormat.encode(full).replaceFirst("#FF8800", "orange"),
            JarnThemeFormat.encode(full).replaceFirst(""""type":"list"""", """"type":"wheel""""),
        ).forEach { text ->
            val result = JarnThemeFormat.decode(text)
            assertTrue("Expected Invalid for '$text', got $result", result is JarnThemeDecodeResult.Invalid)
        }
    }

    @Test
    fun `colors read both hex forms and write the short one when opaque`() {
        assertEquals(ThemeColor(0xFF112233.toInt()), ThemeColor.parse("#112233"))
        assertEquals(ThemeColor(0x80112233.toInt()), ThemeColor.parse("#80112233"))
        assertEquals(ThemeColor(0xFFABCDEF.toInt()), ThemeColor.parse("#abcdef"))
        assertEquals("#112233", ThemeColor(0xFF112233.toInt()).toString())
        assertEquals("#00000000", ThemeColor(0).toString())
        listOf("112233", "#12345", "#GGGGGG", "#1122334455", "#").forEach { bad ->
            assertEquals("'$bad' should not parse", null, ThemeColor.parse(bad))
        }
    }

    @Test
    fun `views read by enum`() {
        assertEquals(full.views.systems, full.views[ThemeView.Systems])
        assertEquals(full.views.games, full.views[ThemeView.Games])
    }
}
