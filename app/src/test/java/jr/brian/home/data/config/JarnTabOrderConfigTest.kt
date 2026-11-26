package jr.brian.home.data.config

import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnTabOrderConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val library = listOf("Collections", "Systems", "AllGames", "Favorites", "Recent")
    private val deck = listOf("Canvas", "Now", "Apps")

    @Test
    fun `both tab orders round trip through the config as id lists`() {
        val prefs = JarnDisplayPrefs(librarySectionOrder = library, deckTabOrder = deck)
        val encoded = json.encodeToString(prefs.toConfig())
        val config = json.decodeFromString<JarnDisplayConfig>(encoded)
        assertEquals(library, config.librarySectionOrder)
        assertEquals(deck, config.deckTabOrder)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `the built-in order goes out as empty lists and comes back the same`() {
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(JarnDisplayPrefs().toConfig()))
        assertEquals(emptyList<String>(), config.librarySectionOrder)
        assertEquals(JarnDisplayPrefs(), config.applyTo(JarnDisplayPrefs(librarySectionOrder = library)))
    }

    @Test
    fun `a pre v75 display block keeps the device's order, the built-in one on a fresh install`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"tabFocus":"both"}""")
        assertNull(config.librarySectionOrder)
        assertNull(config.deckTabOrder)
        assertEquals(emptyList<String>(), config.applyTo(JarnDisplayPrefs()).librarySectionOrder)
        val kept = config.applyTo(JarnDisplayPrefs(librarySectionOrder = library, deckTabOrder = deck))
        assertEquals(library, kept.librarySectionOrder)
        assertEquals(deck, kept.deckTabOrder)
    }

    @Test
    fun `a different order shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(deckTabOrder = emptyList())))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(deckTabOrder = deck)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `the schema version covers tab order`() {
        assertEquals(75, JarngreiprConfig.CONFIG_VERSION)
    }
}
