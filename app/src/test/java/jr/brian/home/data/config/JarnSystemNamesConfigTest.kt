package jr.brian.home.data.config

import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnSystemNamesConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `system names are shown by default`() {
        assertTrue(JarnDisplayPrefs().systemNamesVisible)
    }

    @Test
    fun `hidden system names round trip through the config`() {
        val prefs = JarnDisplayPrefs(systemNamesVisible = false)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals(false, config.systemNamesVisible)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a v55 display block has no system names choice and keeps the current one`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"systemView":"carousel","pageTransition":"fade"}""")
        assertNull(config.systemNamesVisible)
        assertFalse(config.applyTo(JarnDisplayPrefs(systemNamesVisible = false)).systemNamesVisible)
        assertTrue(config.applyTo(JarnDisplayPrefs(systemNamesVisible = true)).systemNamesVisible)
    }

    @Test
    fun `a different system names choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(systemNamesVisible = true)))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(systemNamesVisible = false)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `a backup without the choice shows the current one as staying`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true, systemNamesVisible = false)),
        )
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Same, look.change)
    }
}
