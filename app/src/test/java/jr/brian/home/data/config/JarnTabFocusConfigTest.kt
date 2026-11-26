package jr.brian.home.data.config

import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnTabFocus
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnTabFocusConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `both is the default and every choice round trips`() {
        assertEquals(JarnTabFocus.Both, JarnDisplayPrefs().tabFocus)
        JarnTabFocus.entries.forEach { choice ->
            val prefs = JarnDisplayPrefs(tabFocus = choice)
            val config = json.decodeFromString<JarnDisplayConfig>(json.encodeToString(prefs.toConfig()))
            assertEquals(choice.id, config.tabFocus)
            assertEquals(prefs, config.applyTo(JarnDisplayPrefs()))
        }
    }

    @Test
    fun `a pre v73 display block keeps the current choice`() {
        val config = json.decodeFromString<JarnDisplayConfig>("""{"videoFit":"crop"}""")
        assertNull(config.tabFocus)
        assertEquals(JarnTabFocus.Both, config.applyTo(JarnDisplayPrefs()).tabFocus)
        assertEquals(JarnTabFocus.Shoulders, config.applyTo(JarnDisplayPrefs(tabFocus = JarnTabFocus.Shoulders)).tabFocus)
    }

    @Test
    fun `an unknown id keeps the current choice`() {
        val restored = JarnDisplayConfig(tabFocus = "touch").applyTo(JarnDisplayPrefs(tabFocus = JarnTabFocus.Neither))
        assertEquals(JarnTabFocus.Neither, restored.tabFocus)
    }

    @Test
    fun `a different choice shows as a look change`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(tabFocus = "shoulders")))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(tabFocus = "dpad")))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `the schema version covers tab focus`() {
        assertEquals(75, JarngreiprConfig.CONFIG_VERSION)
    }
}
