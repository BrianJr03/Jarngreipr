package jr.brian.frontend.library.systems

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JSON schema contract for [SystemDef]. These tests pin the on-disk
 * field names and the default-tolerant decoding behaviour the shipped
 * asset files depend on.
 */
class SystemDefTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `full round trip preserves every field`() {
        val original = SystemDef(
            id = "n64",
            displayName = "Nintendo 64",
            folderAliases = listOf("nintendo64", "n64dd"),
            extensions = setOf("z64", "n64", "v64"),
            screenScraperSystemId = 14,
            retroArchSystemId = "n64",
        )
        val encoded = json.encodeToString(SystemDef.serializer(), original)
        val decoded = json.decodeFromString(SystemDef.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `minimal JSON decodes with defaults for omitted fields`() {
        val minimal = """
            {
              "id": "scummvm",
              "displayName": "ScummVM"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(SystemDef.serializer(), minimal)
        assertEquals("scummvm", decoded.id)
        assertEquals("ScummVM", decoded.displayName)
        assertTrue(decoded.folderAliases.isEmpty())
        assertTrue(decoded.extensions.isEmpty())
        assertNull(decoded.screenScraperSystemId)
        assertNull(decoded.retroArchSystemId)
    }

    @Test
    fun `unknown JSON keys do not fail parsing`() {
        // Pins the catalog's forward-compat guarantee: an older client
        // decoding a JSON authored under a newer schema skips the new
        // key and uses the fields it understands.
        val futureSchema = """
            {
              "id": "nes",
              "displayName": "Nintendo Entertainment System",
              "extensions": ["nes", "fds"],
              "emulators": [{"package": "com.example.foo"}],
              "someFutureFieldWeDoNotYetKnowAbout": 42
            }
        """.trimIndent()
        val decoded = json.decodeFromString(SystemDef.serializer(), futureSchema)
        assertEquals("nes", decoded.id)
        assertEquals(setOf("nes", "fds"), decoded.extensions)
    }
}
