package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeTextStyle
import jr.brian.core.model.theme.ThemeTypeScale
import jr.brian.core.model.theme.ThemeViewPatch
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JarnThemeInstallerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var themes: File
    private lateinit var outside: File

    @Before
    fun setup() {
        themes = temp.newFolder("files", "themes")
        outside = File(temp.root, "files")
    }

    private fun installer(limits: ThemeArchiveLimits = ThemeArchiveLimits()) = JarnThemeInstaller(themes, limits)

    private fun zip(vararg entries: Pair<String, ByteArray>) = zipOf(temp.newFile(), *entries)

    private fun rejected(result: ThemeImportResult) = (result as? ThemeImportResult.Rejected)?.problem

    private fun assertNothingLeft() {
        assertEquals(emptyList<String>(), themes.list()!!.toList())
        assertEquals(listOf("themes"), outside.list()!!.toList())
    }

    @Test
    fun `a good theme installs under its id with its files`() {
        val result = installer().install(zip("theme.json" to themeJson(), "art/bg.png" to pngHeader(1920, 1080)))

        val installed = result as ThemeImportResult.Installed
        assertEquals("art-book", installed.id)
        assertEquals(File(themes, "art-book"), installed.folder)
        assertTrue(File(installed.folder, "art/bg.png").isFile)
        assertEquals(listOf("art-book"), themes.list()!!.toList())
    }

    @Test
    fun `a theme file's narrow layers are checked like the rest`() {
        val box = ThemeRect(0f, 0f, 1f, 1f)
        fun picture(id: String, path: String) = ThemeLayer.Picture(id, box, source = ThemePicture.File(path))
        val spec = testSpec().copy(
            views = ThemeViews(
                systems = ViewSpec(
                    primary = ThemePrimary.Grid(box),
                    narrow = ThemeViewPatch(
                        layers = listOf(picture("ok", "art/bg.png"), picture("missing", "art/nope.png"), picture("up", "../bg.png")),
                    ),
                ),
            ),
        )

        val result = installer().install(zip("theme.json" to themeJson(spec), "art/bg.png" to pngHeader(8, 8))) as ThemeImportResult.Installed

        val narrow = result.spec.views.systems!!.narrow!!
        assertEquals(listOf("ok"), narrow.layers.map { it.id })
        assertEquals(listOf("missing", "up"), narrow.hide)
        assertEquals(2, result.dropped)
    }

    @Test
    fun `a theme zipped inside one folder installs too, skipping the rest`() {
        val result = installer().install(
            zip("Art Book/theme.json" to themeJson(), "Art Book/bg.png" to pngHeader(10, 10), "__MACOSX/._x" to byteArrayOf(1)),
        ) as ThemeImportResult.Installed

        assertEquals(setOf("theme.json", "bg.png"), result.folder.list()!!.toSet())
    }

    @Test
    fun `zip slip entries are refused and nothing is written`() {
        listOf("../evil.png", "art/../../evil.png", "/abs.png", "C:/x.png", "a\\..\\b.png", "./theme.json").forEach { name ->
            val result = installer().install(zip("theme.json" to themeJson(), name to pngHeader(1, 1)))
            assertEquals(name, ThemeImportProblem.UnsafeEntry, rejected(result))
        }
        assertFalse(File(temp.root, "evil.png").exists())
        assertFalse(File(outside, "evil.png").exists())
        assertNothingLeft()
    }

    @Test
    fun `symlink entries are refused`() {
        val archive = zip("theme.json" to themeJson(), "link.png" to "/data/data/x".toByteArray())
        patchCentral(archive, "link.png", madeByHost = 3, unixMode = 0xA1FF)

        assertEquals(ThemeImportProblem.UnsafeEntry, rejected(installer().install(archive)))
        assertNothingLeft()
    }

    @Test
    fun `a plain Unix file mode is fine`() {
        val archive = zip("theme.json" to themeJson())
        patchCentral(archive, "theme.json", madeByHost = 3, unixMode = 0x81A4)

        assertTrue(installer().install(archive) is ThemeImportResult.Installed)
    }

    @Test
    fun `duplicate names are refused`() {
        val archive = zip("theme.json" to themeJson(), "a.png" to pngHeader(1, 1), "a.png/" to ByteArray(0))
        assertEquals(ThemeImportProblem.UnsafeEntry, rejected(installer().install(archive)))
    }

    @Test
    fun `too many entries are refused before anything is inflated`() {
        val entries = (1..11).map { "f$it.png" to pngHeader(1, 1) }.toTypedArray()
        val result = installer(ThemeArchiveLimits(maxEntries = 10)).install(zip("theme.json" to themeJson(), *entries))
        assertEquals(ThemeImportProblem.TooManyEntries, rejected(result))
        assertNothingLeft()
    }

    @Test
    fun `an entry over the per-entry cap is refused`() {
        val limits = ThemeArchiveLimits(maxEntryBytes = 1000, maxRatio = 1_000_000)
        val result = installer(limits).install(zip("theme.json" to themeJson(), "big.png" to ByteArray(2000)))
        assertEquals(ThemeImportProblem.TooLarge, rejected(result))
    }

    @Test
    fun `entries over the total cap are refused`() {
        val limits = ThemeArchiveLimits(maxTotalBytes = 3000, maxRatio = 1_000_000)
        val parts = (1..4).map { "p$it.png" to ByteArray(1000) }.toTypedArray()
        assertEquals(ThemeImportProblem.TooLarge, rejected(installer(limits).install(zip("theme.json" to themeJson(), *parts))))
    }

    @Test
    fun `an archive over the file cap is refused`() {
        val archive = zip("theme.json" to themeJson(), "noise.png" to kotlin.random.Random(1).nextBytes(5000))
        assertEquals(ThemeImportProblem.TooLarge, rejected(installer(ThemeArchiveLimits(maxArchiveBytes = 1000)).install(archive)))
    }

    @Test
    fun `a zip bomb is refused by its compression ratio`() {
        val result = installer().install(zip("theme.json" to themeJson(), "bomb.png" to ByteArray(4 * 1024 * 1024)))
        assertEquals(ThemeImportProblem.CompressionBomb, rejected(result))
        assertNothingLeft()
    }

    @Test
    fun `a header that understates the size is caught while inflating`() {
        val archive = zip("theme.json" to themeJson(), "liar.png" to ByteArray(4 * 1024 * 1024))
        patchCentral(archive, "liar.png", size = 10)

        val result = rejected(installer(ThemeArchiveLimits(maxEntryBytes = 1024 * 1024)).install(archive))

        assertTrue(result.toString(), result == ThemeImportProblem.TooLarge || result == ThemeImportProblem.Damaged)
        assertNothingLeft()
    }

    @Test
    fun `small files that compress well are not mistaken for bombs`() {
        val result = installer().install(zip("theme.json" to themeJson(), "flat.png" to pngHeader(4, 4) + ByteArray(30_000)))
        assertTrue(result is ThemeImportResult.Installed)
    }

    @Test
    fun `not a zip, or a zip without theme json, is not a theme`() {
        assertEquals(ThemeImportProblem.NotATheme, rejected(installer().install(temp.newFile().apply { writeText("hello there") })))
        assertEquals(ThemeImportProblem.NotATheme, rejected(installer().install(zip("bg.png" to pngHeader(1, 1)))))
        assertEquals(ThemeImportProblem.NotATheme, rejected(installer().install(zip("theme.json" to "{ nope".toByteArray()))))
        assertNothingLeft()
    }

    @Test
    fun `a theme from a newer Jarn says so`() {
        val result = installer().install(zip("theme.json" to """{ "schema": 99 }""".toByteArray()))
        assertEquals(ThemeImportResult.TooNew(99, 1), result)
        assertNothingLeft()
    }

    @Test
    fun `a clashing id keeps both by default, or replaces when asked`() {
        val first = installer().install(zip("theme.json" to themeJson(), "a.png" to pngHeader(1, 1))) as ThemeImportResult.Installed
        val second = installer().install(zip("theme.json" to themeJson())) as ThemeImportResult.Installed
        assertEquals("art-book-2", second.id)
        assertTrue(File(first.folder, "a.png").isFile)

        val replaced = installer().install(zip("theme.json" to themeJson()), ThemeIdClash.Replace) as ThemeImportResult.Installed

        assertEquals("art-book", replaced.id)
        assertFalse(File(replaced.folder, "a.png").exists())
        assertEquals(setOf("art-book", "art-book-2"), themes.list()!!.toSet())
    }

    @Test
    fun `a failed replace leaves the installed theme alone`() {
        installer().install(zip("theme.json" to themeJson(), "a.png" to pngHeader(1, 1)))

        installer().install(zip("theme.json" to themeJson(), "../x.png" to pngHeader(1, 1)), ThemeIdClash.Replace)

        assertTrue(File(themes, "art-book/a.png").isFile)
        assertEquals(listOf("art-book"), themes.list()!!.toList())
    }

    @Test
    fun `leftovers from an interrupted import are cleared`() {
        File(themes, ".incoming-old/theme.json").apply { parentFile!!.mkdirs(); writeText("{}") }
        installer().install(zip("theme.json" to themeJson()))
        assertEquals(listOf("art-book"), themes.list()!!.toList())
    }

    @Test
    fun `bad files and paths outside the theme drop alone`() {
        val spec = testSpec().copy(
            tokens = testSpec().tokens.copy(
                type = ThemeTypeScale(
                    title = ThemeTextStyle(size = 30f, font = "fonts/title.ttf"),
                    body = ThemeTextStyle(size = 16f, font = "../../system/fonts/Roboto.ttf"),
                    label = ThemeTextStyle(size = 12f, font = "fonts/broken.ttf"),
                ),
            ),
            views = ThemeViews(
                games = ViewSpec(
                    primary = ThemePrimary.Shelf(box = ThemeRect(0f, 0f, 1f, 1f), art = ThemePicture.File("missing.png")),
                    layers = listOf(
                        ThemeLayer.Picture("ok", ThemeRect(0f, 0f, 1f, 1f), source = ThemePicture.File("art/bg.png")),
                        ThemeLayer.Picture("huge", ThemeRect(0f, 0f, 1f, 1f), source = ThemePicture.File("art/huge.png")),
                        ThemeLayer.Picture("escape", ThemeRect(0f, 0f, 1f, 1f), source = ThemePicture.File("/sdcard/x.png")),
                        ThemeLayer.Picture("font", ThemeRect(0f, 0f, 1f, 1f), source = ThemePicture.File("fonts/title.ttf")),
                        ThemeLayer.Video("clip", ThemeRect(0f, 0f, 1f, 1f), fallback = ThemePicture.File("../bg.png")),
                    ),
                ),
            ),
        )
        val result = installer().install(
            zip(
                "theme.json" to themeJson(spec),
                "art/bg.png" to pngHeader(1920, 1080),
                "art/huge.png" to pngHeader(50_000, 50_000),
                "fonts/title.ttf" to ttf(),
                "fonts/broken.ttf" to ByteArray(64) { 7 },
                "notes.exe" to byteArrayOf(1, 2, 3),
            ),
        ) as ThemeImportResult.Installed

        val type = result.spec.tokens.type
        assertEquals("fonts/title.ttf", type.title?.font)
        assertNull(type.body?.font)
        assertNull(type.label?.font)
        val games = result.spec.views.games!!
        assertNull(games.primary.art)
        assertEquals(listOf("ok", "clip"), games.layers.map { it.id })
        assertNull((games.layers[1] as ThemeLayer.Video).fallback)
        assertEquals(7, result.dropped)
        assertEquals(setOf("art/bg.png", "fonts/title.ttf", "theme.json"), result.folder.walk().filter { it.isFile }
            .map { it.relativeTo(result.folder).invariantSeparatorsPath }.toSet())
        val saved = JarnThemeFormat.decode(File(result.folder, "theme.json").readText())
        assertEquals(result.spec, (saved as jr.brian.core.model.theme.JarnThemeDecodeResult.Decoded).spec)
    }

    @Test
    fun `ids come from the theme name`() {
        assertEquals("art-book-next", themeIdFor("  Art Book: NEXT!! "))
        assertEquals("theme", themeIdFor("★★★"))
        assertEquals(48, themeIdFor("x".repeat(100)).length)
    }
}
