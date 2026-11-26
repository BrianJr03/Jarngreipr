package jr.brian.home.data.config

import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnBatterySaverConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `schema version covers the battery saver setting`() {
        assertEquals(75, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `pausing is on by default and off round trips`() {
        assertTrue(JarnDisplayPrefs().pauseOnBatterySaver)
        val prefs = JarnDisplayPrefs(pauseOnBatterySaver = false)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals(false, config.pauseOnBatterySaver)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a pre v74 display block keeps the current value, on for a fresh install`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"videoFit":"crop"}""")
        assertNull(config.pauseOnBatterySaver)
        assertTrue(config.applyTo(JarnDisplayPrefs()).pauseOnBatterySaver)
        assertEquals(false, config.applyTo(JarnDisplayPrefs(pauseOnBatterySaver = false)).pauseOnBatterySaver)
    }

    @Test
    fun `a different choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(pauseOnBatterySaver = true)))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(pauseOnBatterySaver = false)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }
}
