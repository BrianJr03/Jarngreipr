package jr.brian.frontend.look.themes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EsdeRestoreOfferTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var source: File
    private lateinit var themes: File
    private lateinit var offerFile: File
    private var access = true

    private val locator by lazy {
        EsdeThemeLocator(
            esdeFolders = { listOf(source.parentFile!!.parentFile!!) },
            hasAllFilesAccess = { access },
            readText = { runCatching { it.readText() }.getOrNull() },
        )
    }

    private val updates by lazy { EsdeThemeUpdates(themes, locator, ::readEsdeThemeStamp, offerFile) }

    @Before
    fun setup() {
        source = File(temp.newFolder("ES-DE"), "themes/linear").apply { mkdirs() }
        File(source, "capabilities.xml").writeText("<themeCapabilities/>")
        themes = temp.newFolder("themes")
        offerFile = esdeRestoreOfferFile(temp.root)
        writeEsdeRestoreOffer(offerFile, EsdeRestoreOffer("linear", source.path, variant = "textlist", colorScheme = "dark"))
    }

    @Test
    fun `a reachable folder becomes a re-import with the backed up choices`() = runTest {
        val offer = updates.restoreOfferFor("linear")!!
        assertEquals(source, offer.source)
        assertEquals("textlist", offer.record.variant)
        assertEquals("dark", offer.record.colorScheme)
        assertEquals(source.path, offer.record.sourceFolder)
    }

    @Test
    fun `an unreachable folder waits, keeping the offer for a later launch`() = runTest {
        source.deleteRecursively()
        assertNull(updates.restoreOfferFor("linear"))
        assertEquals("linear", readEsdeRestoreOffer(offerFile)?.themeId)

        access = false
        assertNull(updates.restoreOfferFor("linear"))
        assertEquals("linear", readEsdeRestoreOffer(offerFile)?.themeId)
    }

    @Test
    fun `the offer ends once the theme is back or another layout is picked`() = runTest {
        File(themes, "linear").mkdirs()
        assertNull(updates.restoreOfferFor("linear"))
        assertNull(readEsdeRestoreOffer(offerFile))

        writeEsdeRestoreOffer(offerFile, EsdeRestoreOffer("linear", source.path))
        File(themes, "linear").deleteRecursively()
        assertNull(updates.restoreOfferFor("builtin-grid"))
        assertNull(readEsdeRestoreOffer(offerFile))
    }

    @Test
    fun `saying no drops the offer, and a damaged file reads as none`() = runTest {
        updates.dropRestoreOffer()
        assertNull(readEsdeRestoreOffer(offerFile))
        offerFile.writeText("{ broken")
        assertNull(updates.restoreOfferFor("linear"))
    }
}
