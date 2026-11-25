package jr.brian.home.data.config

import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnPlayingConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `now playing is on with a 30 second dim by default`() {
        assertTrue(JarnDisplayPrefs().playingScreenVisible)
        assertEquals(JarnPlayingDim.ThirtySeconds, JarnDisplayPrefs().playingDim)
    }

    @Test
    fun `the choices round trip through the config`() {
        val prefs = JarnDisplayPrefs(playingScreenVisible = false, playingDim = JarnPlayingDim.Never)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals(false, config.playingScreenVisible)
        assertEquals("never", config.playingDim)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a pre v68 display block keeps the current choices`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"hintsVisible":true}""")
        assertNull(config.playingScreenVisible)
        assertNull(config.playingDim)
        val current = JarnDisplayPrefs(playingScreenVisible = false, playingDim = JarnPlayingDim.FiveMinutes)
        assertEquals(current, config.applyTo(current))
    }

    @Test
    fun `an unknown dim id keeps the current dim`() {
        val config = JarnDisplayConfig(playingDim = "forever")
        val current = JarnDisplayPrefs(playingDim = JarnPlayingDim.OneMinute)
        assertEquals(JarnPlayingDim.OneMinute, config.applyTo(current).playingDim)
    }

    @Test
    fun `a different now playing choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(playingScreenVisible = true)))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(playingScreenVisible = false)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }
}
