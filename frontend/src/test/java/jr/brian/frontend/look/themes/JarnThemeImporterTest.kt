package jr.brian.frontend.look.themes

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnThemeImporterTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val uri = Uri.parse("content://picker/doc/1")

    private fun importer(limits: ThemeArchiveLimits = ThemeArchiveLimits(), open: (Uri) -> InputStream?): Pair<JarnThemeImporter, File> {
        val cache = temp.newFolder()
        val themes = File(temp.root, "themes")
        return JarnThemeImporter({ cache }, open, JarnThemeInstaller(themes, limits), limits) to cache
    }

    @Test
    fun `a picked theme file installs and its cache copy is removed`() = runTest {
        val archive = zipOf(temp.newFile(), "theme.json" to themeJson())
        val (importer, cache) = importer { archive.inputStream() }

        val result = importer.import(uri)

        assertTrue(result is ThemeImportResult.Installed)
        assertEquals(emptyList<String>(), cache.list()!!.toList())
    }

    @Test
    fun `a document that won't open is unreadable`() = runTest {
        assertEquals(ThemeImportResult.Rejected(ThemeImportProblem.Unreadable), importer { null }.first.import(uri))
        assertEquals(
            ThemeImportResult.Rejected(ThemeImportProblem.Unreadable),
            importer { throw SecurityException("revoked") }.first.import(uri),
        )
    }

    @Test
    fun `an oversized document stops copying at the cap`() = runTest {
        var served = 0L
        val endless = object : InputStream() {
            override fun read(): Int = 0.also { served++ }
            override fun read(b: ByteArray, off: Int, len: Int): Int = len.also { served += it }
        }
        val (importer, cache) = importer(ThemeArchiveLimits(maxArchiveBytes = 1_000_000)) { endless }

        assertEquals(ThemeImportResult.Rejected(ThemeImportProblem.TooLarge), importer.import(uri))
        assertTrue(served < 1_200_000)
        assertEquals(emptyList<String>(), cache.list()!!.toList())
    }
}
