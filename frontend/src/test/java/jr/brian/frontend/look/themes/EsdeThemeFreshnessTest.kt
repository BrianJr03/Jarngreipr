package jr.brian.frontend.look.themes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EsdeThemeFreshnessTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var esde: File
    private lateinit var source: File
    private var access = true

    private val locator by lazy {
        EsdeThemeLocator(esdeFolders = { listOf(esde) }, hasAllFilesAccess = { access }, readText = { runCatching { it.readText() }.getOrNull() })
    }

    @Before
    fun setup() {
        esde = temp.newFolder("ES-DE")
        source = File(esde, "themes/linear").apply { mkdirs() }
        File(source, "capabilities.xml").writeText("<themeCapabilities><themeName>Linear</themeName></themeCapabilities>")
        File(source, "theme.xml").writeText("<theme/>")
        source.setLastModified(1_000_000)
    }

    private fun record() = EsdeImportRecord(
        sourceFolder = source.path,
        variant = "textlist",
        colorScheme = "dark",
        aspectRatio = "16:9",
        stamp = readEsdeThemeStamp(source)!!,
    )

    private fun check(record: EsdeImportRecord) = EsdeThemeFreshness.check(record, locator)

    @Test
    fun `an untouched folder is unchanged`() {
        assertEquals(EsdeThemeFreshnessResult.Unchanged, check(record()))
    }

    @Test
    fun `edited files, new capabilities or a moved git HEAD are updates`() {
        val before = record()
        File(source, "theme.xml").setLastModified(5_000_000)
        assertTrue(check(before) is EsdeThemeFreshnessResult.Updated)

        val second = record()
        val capabilities = File(source, "capabilities.xml")
        val time = capabilities.lastModified()
        capabilities.writeText("<themeCapabilities><themeName>Linear 2</themeName></themeCapabilities>")
        capabilities.setLastModified(time)
        assertTrue(check(second) is EsdeThemeFreshnessResult.Updated)

        File(source, ".git/refs/heads").mkdirs()
        File(source, ".git/HEAD").writeText("ref: refs/heads/main\n")
        File(source, ".git/refs/heads/main").writeText("aaa111\n")
        source.setLastModified(1_000_000)
        val third = record()
        assertEquals("aaa111", third.stamp.gitHead)
        File(source, ".git/refs/heads/main").writeText("bbb222\n")
        val updated = check(third) as EsdeThemeFreshnessResult.Updated
        assertEquals("bbb222", updated.current.gitHead)
        assertEquals(source, updated.source)
    }

    @Test
    fun `git HEAD is read from packed refs and detached heads`() {
        val git = File(source, ".git").apply { mkdirs() }
        File(git, "HEAD").writeText("ref: refs/heads/dev\n")
        File(git, "packed-refs").writeText("# pack-refs\nccc333 refs/heads/dev\nddd444 refs/heads/main\n")
        assertEquals("ccc333", readGitHead(git))

        File(git, "HEAD").writeText("eee555\n")
        assertEquals("eee555", readGitHead(git))

        File(git, "HEAD").writeText("ref: ../../outside\n")
        assertNull(readGitHead(git))
        assertNull(readGitHead(File(source, "nope")))
    }

    @Test
    fun `a missing folder or SD card is missing, never updated, and nothing is deleted`() {
        val record = record()
        val installed = temp.newFolder("installed").also { writeEsdeImportRecord(it, record) }
        source.deleteRecursively()

        assertEquals(EsdeThemeFreshnessResult.SourceMissing, check(record))
        assertEquals(record, readEsdeImportRecord(installed))

        val sdCard = record.copy(sourceFolder = "/storage/1234-ABCD/ES-DE/themes/linear")
        assertEquals(EsdeThemeFreshnessResult.SourceMissing, check(sdCard))
    }

    @Test
    fun `a same-named copy elsewhere is not the imported theme`() {
        val record = record().copy(sourceFolder = File(temp.root, "gone/themes/linear").path)
        assertEquals(EsdeThemeFreshnessResult.SourceMissing, check(record))
    }

    @Test
    fun `a folder that lost its capabilities is missing`() {
        val record = record()
        File(source, "capabilities.xml").delete()
        assertEquals(EsdeThemeFreshnessResult.SourceMissing, check(record))
    }

    @Test
    fun `without all-files access the source is missing`() {
        val record = record()
        File(source, "theme.xml").setLastModified(9_000_000)
        access = false
        assertEquals(EsdeThemeFreshnessResult.SourceMissing, check(record))
    }

    @Test
    fun `records round trip and a damaged one reads as none`() {
        val folder = temp.newFolder("t")
        assertNull(readEsdeImportRecord(folder))
        writeEsdeImportRecord(folder, record())
        assertEquals(record(), readEsdeImportRecord(folder))
        assertEquals(listOf(ESDE_IMPORT_RECORD_FILE), folder.list()!!.toList())

        File(folder, ESDE_IMPORT_RECORD_FILE).writeText("{ broken")
        assertNull(readEsdeImportRecord(folder))
    }

    @Test
    fun `the launch check offers an update once, until a newer change`() = runTest {
        val themes = temp.newFolder("themes")
        writeEsdeImportRecord(File(themes, "linear").apply { mkdirs() }, record())
        val updates = EsdeThemeUpdates(themes, locator, ::readEsdeThemeStamp)
        assertNull(updates.updateFor("linear"))

        File(source, "theme.xml").setLastModified(5_000_000)
        val update = updates.updateFor("linear")!!
        assertEquals("textlist", update.record.variant)

        updates.dismiss(update)
        assertNull(updates.updateFor("linear"))

        File(source, "theme.xml").setLastModified(6_000_000)
        assertTrue(updates.updateFor("linear") != null)
    }

    @Test
    fun `the launch check ignores themes not from ES-DE and unsafe ids`() = runTest {
        val themes = temp.newFolder("themes")
        File(themes, "plain").mkdirs()
        val updates = EsdeThemeUpdates(themes, locator, ::readEsdeThemeStamp)
        assertNull(updates.updateFor("plain"))
        assertNull(updates.updateFor("missing"))
        assertNull(updates.updateFor("../ES-DE"))
    }
}
