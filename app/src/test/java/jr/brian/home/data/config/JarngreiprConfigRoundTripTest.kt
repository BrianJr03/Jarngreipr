package jr.brian.home.data.config

import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.SystemFolderMapping
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
    fun `pre-split config carries its combined dim onto both new fields`() {
        // A v17-or-earlier export only has frontendFocusBackgroundDim; the two per-scope
        // fields don't exist in the JSON. After decode they land at their data-class
        // defaults (0.55f). ImportExportManager treats "new field at default AND legacy
        // differs" as the pre-split case and prefers the legacy value for both scopes.
        val preSplit = """
            {
              "version": 17,
              "feature": {
                "romSearch": {
                  "frontendFocusBackgroundDim": 0.8
                }
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<JarngreiprConfig>(preSplit)

        @Suppress("DEPRECATION")
        val legacyDim = decoded.feature.romSearch.frontendFocusBackgroundDim
        assertEquals(0.8f, legacyDim, 0.0001f)
        // Per-scope fields absent from the JSON: they materialize at their defaults,
        // and ImportExportManager falls back to the legacy value for both scopes.
        assertEquals(0.55f, decoded.feature.romSearch.frontendFocusBackgroundDimSystems, 0.0001f)
        assertEquals(0.55f, decoded.feature.romSearch.frontendFocusBackgroundDimGames, 0.0001f)

        val systemsDim = decoded.feature.romSearch.frontendFocusBackgroundDimSystems.let { new ->
            if (new == 0.55f && legacyDim != 0.55f) legacyDim else new
        }
        val gamesDim = decoded.feature.romSearch.frontendFocusBackgroundDimGames.let { new ->
            if (new == 0.55f && legacyDim != 0.55f) legacyDim else new
        }
        assertEquals(0.8f, systemsDim, 0.0001f)
        assertEquals(0.8f, gamesDim, 0.0001f)
    }

    @Test
    fun `post-split config round-trips independent dim values`() {
        val original = JarngreiprConfig(
            feature = FeatureConfig(
                romSearch = RomSearchConfig(
                    frontendFocusBackgroundDimSystems = 0.2f,
                    frontendFocusBackgroundDimGames = 0.9f
                )
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<JarngreiprConfig>(encoded)

        assertEquals(0.2f, decoded.feature.romSearch.frontendFocusBackgroundDimSystems, 0.0001f)
        assertEquals(0.9f, decoded.feature.romSearch.frontendFocusBackgroundDimGames, 0.0001f)
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

    @Test
    fun `system folder mappings round-trip through the config`() {
        // v22: user-declared "this folder is system X" entries have to survive an
        // export/import so a user restoring a backup keeps their SD-card systems.
        val original = JarngreiprConfig(
            system = SystemConfig(
                systemFolderMappings = listOf(
                    SystemFolderMapping(
                        systemName = "ps2",
                        treeUri = "content://com.android.externalstorage.documents/tree/1A2B-3C4D%3AMyGames",
                        displayPath = "/storage/1A2B-3C4D/MyGames",
                    ),
                    SystemFolderMapping(
                        systemName = "snes",
                        treeUri = "content://com.android.externalstorage.documents/tree/primary%3AGames",
                        displayPath = "/storage/emulated/0/Games",
                    ),
                )
            )
        )

        val decoded = json.decodeFromString<JarngreiprConfig>(json.encodeToString(original))
        assertEquals(2, decoded.system.systemFolderMappings.size)
        val ps2 = decoded.system.systemFolderMappings.first { it.systemName == "ps2" }
        assertEquals("/storage/1A2B-3C4D/MyGames", ps2.displayPath)
    }

    @Test
    fun `pre-v22 config decodes with empty system folder mappings`() {
        // Backwards-compat guard: a config exported before the field existed
        // must still import — the field materializes at its empty default.
        val preV22 = """
            {
              "version": 21,
              "system": {
                "hiddenSystems": ["ps2"]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<JarngreiprConfig>(preV22)
        assertTrue(decoded.system.systemFolderMappings.isEmpty())
        assertEquals(listOf("ps2"), decoded.system.hiddenSystems)
    }
}
