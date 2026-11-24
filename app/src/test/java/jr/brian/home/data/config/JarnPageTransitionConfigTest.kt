package jr.brian.home.data.config

import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnPageTransitionConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `page transition round trips through the config`() {
        val prefs = JarnDisplayPrefs(pageTransition = JarnPageTransition.GlideUp)
        val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
        assertEquals("glide_up", config.pageTransition)
        assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
    }

    @Test
    fun `a v54 display block keeps the current page transition`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"systemView":"carousel"}""")
        assertNull(config.pageTransition)
        val current = JarnDisplayPrefs(pageTransition = JarnPageTransition.Depth)
        assertEquals(JarnPageTransition.Depth, config.applyTo(current).pageTransition)
    }

    @Test
    fun `a Classic only or unknown id falls back to the jarn slide`() {
        val current = JarnDisplayPrefs(pageTransition = JarnPageTransition.Stack)
        assertEquals(JarnPageTransition.JarnSlide, JarnDisplayConfig(pageTransition = "cube").applyTo(current).pageTransition)
    }

    @Test
    fun `the jarn slide is the default`() {
        assertEquals(JarnPageTransition.JarnSlide, JarnDisplayPrefs().pageTransition)
    }

    @Test
    fun `a different page transition shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(pageTransition = "fade")))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(pageTransition = "depth")))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `a backup without a page transition shows the current one as staying`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true, pageTransition = "stack")),
        )
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true)))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Same, look.change)
    }
}
