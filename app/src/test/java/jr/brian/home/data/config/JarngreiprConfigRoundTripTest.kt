package jr.brian.home.data.config

import jr.brian.home.esde.model.FrontendLayout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round-trip guarantees for the frontend prefs added across Phases 2.5 / 2.6 / 3.
 *
 * Mirrors [ImportExportManager.json]'s configuration (`ignoreUnknownKeys`,
 * `encodeDefaults`). The two must stay in sync — if you change one, change the other.
 *
 * RomSearchConfig lives at `feature.romSearch` (FeatureConfig nests it).
 */
class JarngreiprConfigRoundTripTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun `frontend prefs survive an encode-decode round trip`() {
        val original = JarngreiprConfig(
            feature = FeatureConfig(
                romSearch = RomSearchConfig(
                    hintsKbVisible = false,
                    frontendEnabled = true,
                    secondaryMediaEnabled = false,
                    systemLayout = FrontendLayout.Row.name,
                    gameLayout = FrontendLayout.Row.name
                )
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<JarngreiprConfig>(encoded)

        assertEquals(false, decoded.feature.romSearch.hintsKbVisible)
        assertEquals(true, decoded.feature.romSearch.frontendEnabled)
        assertEquals(false, decoded.feature.romSearch.secondaryMediaEnabled)
        assertEquals(FrontendLayout.Row.name, decoded.feature.romSearch.systemLayout)
        assertEquals(FrontendLayout.Row.name, decoded.feature.romSearch.gameLayout)
    }

    @Test
    fun `legacy config without the new frontend fields decodes to defaults`() {
        // Simulates a pre-frontend backup: RomSearchConfig only carries hintsKbVisible.
        // ignoreUnknownKeys + data-class defaults are what keep older exports importable
        // after a schema is extended.
        val legacy = """
            {
              "version": 1,
              "feature": {
                "romSearch": {
                  "hintsKbVisible": true
                }
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<JarngreiprConfig>(legacy)

        assertTrue(decoded.feature.romSearch.hintsKbVisible)
        // Defaults for fields the old export didn't know about.
        assertFalse(decoded.feature.romSearch.frontendEnabled)
        assertTrue(decoded.feature.romSearch.secondaryMediaEnabled)
        assertEquals(FrontendLayout.Grid.name, decoded.feature.romSearch.systemLayout)
        assertEquals(FrontendLayout.Grid.name, decoded.feature.romSearch.gameLayout)
    }

    @Test
    fun `unknown layout string resolves to null via the runCatching guard`() {
        // ImportExportManager applies the layout enum with:
        //   runCatching { FrontendLayout.valueOf(name) }.getOrNull()?.let { setter(it) }
        // Cover that this guard turns garbage into null (rather than throwing) so the
        // setter is simply skipped and the stored value stays at its default.
        val configJson = """
            {
              "version": 4,
              "feature": {
                "romSearch": {
                  "systemLayout": "Spiral",
                  "gameLayout": "Bogus"
                }
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<JarngreiprConfig>(configJson)
        // The string survives decode — defensive parse happens at apply time.
        assertEquals("Spiral", decoded.feature.romSearch.systemLayout)
        assertEquals("Bogus", decoded.feature.romSearch.gameLayout)

        val resolvedSystem = runCatching {
            FrontendLayout.valueOf(decoded.feature.romSearch.systemLayout)
        }.getOrNull()
        val resolvedGame = runCatching {
            FrontendLayout.valueOf(decoded.feature.romSearch.gameLayout)
        }.getOrNull()
        assertNull(resolvedSystem)
        assertNull(resolvedGame)
    }
}
