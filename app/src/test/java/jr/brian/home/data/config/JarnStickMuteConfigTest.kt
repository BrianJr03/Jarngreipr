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

class JarnStickMuteConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `schema version covers the L3 and R3 mute setting`() {
        assertEquals(77, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `muting with L3 and R3 is off by default and on round trips`() {
        assertFalse(JarnDisplayPrefs().stickMute)
        val prefs = JarnDisplayPrefs(stickMute = true)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals(true, config.stickMute)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a pre v76 display block keeps the current value, off for a fresh install`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"deckTabOrder":["recent"]}""")
        assertNull(config.stickMute)
        assertFalse(config.applyTo(JarnDisplayPrefs()).stickMute)
        assertTrue(config.applyTo(JarnDisplayPrefs(stickMute = true)).stickMute)
    }

    @Test
    fun `a different choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(stickMute = false)))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(stickMute = true)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }
}
