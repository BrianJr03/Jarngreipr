package jr.brian.home.data.config

import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnBrowseViewConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `browse views round trip through the config`() {
        val prefs = JarnDisplayPrefs(systemView = JarnBrowseView.Carousel, gameView = JarnBrowseView.Grid)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals("carousel", config.systemView)
        assertEquals("grid", config.gameView)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `same as layout round trips through the config`() {
        val prefs = JarnDisplayPrefs(systemView = JarnBrowseView.SameAsLayout, gameView = JarnBrowseView.Carousel)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals("layout", config.systemView)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a v52 display block has no browse views and keeps the current ones`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"switchCue":"dim"}""")
        assertNull(config.systemView)
        assertNull(config.gameView)
        val current = JarnDisplayPrefs(systemView = JarnBrowseView.Carousel, gameView = JarnBrowseView.Carousel)
        val restored = config.applyTo(current)
        assertEquals(JarnBrowseView.Carousel, restored.systemView)
        assertEquals(JarnBrowseView.Carousel, restored.gameView)
    }

    @Test
    fun `an unknown id falls back to grid`() {
        val current = JarnDisplayPrefs(systemView = JarnBrowseView.Carousel)
        assertEquals(JarnBrowseView.Grid, JarnDisplayConfig(systemView = "wall").applyTo(current).systemView)
        assertEquals(JarnBrowseView.Grid, JarnBrowseView.fromId(null))
    }

    @Test
    fun `both views default to grid`() {
        assertEquals(JarnBrowseView.Grid, JarnDisplayPrefs().systemView)
        assertEquals(JarnBrowseView.Grid, JarnDisplayPrefs().gameView)
    }

    @Test
    fun `a different browse view shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(gameView = "grid")))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(gameView = "carousel")))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `a backup without browse views shows the current ones as staying`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true, systemView = "carousel")),
        )
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Same, look.change)
    }
}
