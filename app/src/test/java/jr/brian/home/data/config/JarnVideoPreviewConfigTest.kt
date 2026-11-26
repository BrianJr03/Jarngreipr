package jr.brian.home.data.config

import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnVideoPreviewConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `both choices round trip through the config`() {
        val prefs = JarnDisplayPrefs(videoPreviews = true, videoPreviewSound = true)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals(true, config.videoPreviews)
        assertEquals(true, config.videoPreviewSound)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a pre v70 display block keeps the current choices`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"hintsVisible":true}""")
        assertNull(config.videoPreviews)
        assertNull(config.videoPreviewSound)
        val current = JarnDisplayPrefs(videoPreviews = true, videoPreviewSound = true)
        assertEquals(current, config.applyTo(current))
    }

    @Test
    fun `a backup can turn previews off again`() {
        val config = JarnDisplayConfig(videoPreviews = false, videoPreviewSound = false)
        val restored = config.applyTo(JarnDisplayPrefs(videoPreviews = true, videoPreviewSound = true))
        assertEquals(false, restored.videoPreviews)
        assertEquals(false, restored.videoPreviewSound)
    }

    @Test
    fun `a different preview choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(videoPreviews = false)))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(videoPreviews = true)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `a different sound choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(videoPreviewSound = false)))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(videoPreviewSound = true)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }
}
