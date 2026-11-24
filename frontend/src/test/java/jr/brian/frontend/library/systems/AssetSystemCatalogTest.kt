package jr.brian.frontend.library.systems

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the catalog loader without depending on the files shipped
 * under `frontend/src/main/assets/systems/` — a separate test
 * exercises those.
 *
 * We can't drop files directly into the Robolectric asset path, so
 * each test wraps the application context with a mocked AssetManager
 * that returns an in-memory JSON blob. That lets us pin:
 *  - a well-formed JSON becomes a SystemDef in the catalog;
 *  - a filename / id mismatch is skipped (not fatal);
 *  - findByFolderAlias looks through aliases AND the id;
 *  - case-insensitive id lookup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AssetSystemCatalogTest {

    @Test
    fun `all returns display-name sorted systems from the asset dir`() {
        val catalog = catalogOf(
            "nes.json" to nesJson(),
            "snes.json" to snesJson(),
        )
        val all = catalog.all()
        assertEquals(
            listOf("Nintendo Entertainment System", "Super Nintendo"),
            all.map { it.displayName },
        )
    }

    @Test
    fun `findById is case-insensitive`() {
        val catalog = catalogOf("nes.json" to nesJson())
        assertEquals("nes", catalog.findById("NES")?.id)
        assertEquals("nes", catalog.findById("nes")?.id)
    }

    @Test
    fun `findByFolderAlias checks the id first then the alias list`() {
        val catalog = catalogOf("nes.json" to nesJson())
        assertEquals("nes", catalog.findByFolderAlias("nes")?.id)
        assertEquals("nes", catalog.findByFolderAlias("famicom")?.id)
        assertEquals("nes", catalog.findByFolderAlias("FAMICOM")?.id)
        assertNull(catalog.findByFolderAlias("unknown-system"))
    }

    @Test
    fun `extensionsFor returns the known set or empty for unknown ids`() {
        val catalog = catalogOf("nes.json" to nesJson())
        assertEquals(setOf("nes", "fds", "unif"), catalog.extensionsFor("nes"))
        assertTrue(catalog.extensionsFor("made-up").isEmpty())
    }

    @Test
    fun `files whose id does not match the filename stem are skipped`() {
        val catalog = catalogOf(
            // Filename says nes but the file claims id = snes — should be
            // skipped so a copy-paste mistake can't shadow the real nes
            // entry authored under a correctly-named file.
            "nes.json" to """{"id":"snes","displayName":"Confused"}""",
            "snes.json" to snesJson(),
        )
        assertNull(catalog.findById("nes"))
        assertEquals("Super Nintendo", catalog.findById("snes")?.displayName)
    }

    @Test
    fun `malformed JSON is skipped and does not block other systems`() {
        val catalog = catalogOf(
            "nes.json" to nesJson(),
            "broken.json" to """not even json""",
        )
        assertEquals(listOf("nes"), catalog.all().map { it.id })
    }

    private fun catalogOf(vararg files: Pair<String, String>): SystemCatalog {
        val base = RuntimeEnvironment.getApplication()
        val fakeAssets = mockk<AssetManager>().apply {
            every { list("systems") } returns files.map { it.first }.toTypedArray()
            for ((name, content) in files) {
                every { open("systems/$name") } answers { content.byteInputStream() }
            }
        }
        val ctx = object : ContextWrapper(base) {
            override fun getAssets(): AssetManager = fakeAssets
            override fun getApplicationContext(): Context = this
        }
        return AssetSystemCatalog(ctx)
    }

    private fun nesJson() = """
        {
          "id": "nes",
          "displayName": "Nintendo Entertainment System",
          "folderAliases": ["famicom", "nintendo-nes"],
          "extensions": ["nes", "fds", "unif"],
          "screenScraperSystemId": 3
        }
    """.trimIndent()

    private fun snesJson() = """
        {
          "id": "snes",
          "displayName": "Super Nintendo",
          "folderAliases": ["sfc", "snesna"],
          "extensions": ["sfc", "smc", "fig", "swc"],
          "screenScraperSystemId": 4
        }
    """.trimIndent()
}
