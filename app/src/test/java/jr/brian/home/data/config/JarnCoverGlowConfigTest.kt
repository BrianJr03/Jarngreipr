package jr.brian.home.data.config

import jr.brian.frontend.look.JarnCoverGlow
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnCoverGlowConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `schema version covers the cover glow setting`() {
        assertEquals(78, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `the cover glow is off by default and full screen round trips`() {
        assertEquals(JarnCoverGlow.Off, JarnDisplayPrefs().coverGlow)
        val prefs = JarnDisplayPrefs(coverGlow = JarnCoverGlow.Full)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals("full", config.coverGlow)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a pre v77 display block gives off on a fresh install and keeps the current choice`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"stickMute":true}""")
        assertNull(config.coverGlow)
        assertEquals(JarnCoverGlow.Off, config.applyTo(JarnDisplayPrefs()).coverGlow)
        assertEquals(JarnCoverGlow.Full, config.applyTo(JarnDisplayPrefs(coverGlow = JarnCoverGlow.Full)).coverGlow)
    }

    @Test
    fun `an unknown glow id keeps the current choice`() {
        val restored = JarnDisplayConfig(coverGlow = "rainbow").applyTo(JarnDisplayPrefs(coverGlow = JarnCoverGlow.Full))
        assertEquals(JarnCoverGlow.Full, restored.coverGlow)
    }

    @Test
    fun `a different choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(coverGlow = "off")))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(coverGlow = "full")))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }
}
