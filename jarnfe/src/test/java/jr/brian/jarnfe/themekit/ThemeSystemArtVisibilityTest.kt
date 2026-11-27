package jr.brian.jarnfe.themekit

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.ThemeVisibleWhen
import jr.brian.jarnfe.library.BigShelfSystems
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #577: layers can follow whether the focused system has art, or show only when it doesn't. */
class ThemeSystemArtVisibilityTest {
    private val withLogo = JarnLibrarySystem(id = "snes", title = "Super Nintendo", gameCount = 3, logoPath = "/art/snes-logo.png")
    private val noLogo = withLogo.copy(id = "nes", logoPath = null)
    private val media = ThemeFocusMedia()
    private val hasLogo = ThemeVisibleWhen(hasSystemArt = SystemArtKind.Logo)
    private val noLogoRule = hasLogo.copy(missing = true)

    @Test
    fun `system art shows a layer for a system that has it`() {
        assertTrue(themeLayerVisible(hasLogo, FocusedItem.System(withLogo), media))
        assertFalse(themeLayerVisible(hasLogo, FocusedItem.System(noLogo), media))
        assertFalse(themeLayerVisible(hasLogo, FocusedItem.None, media))
    }

    @Test
    fun `missing flips the art check`() {
        assertFalse(themeLayerVisible(noLogoRule, FocusedItem.System(withLogo), media))
        assertTrue(themeLayerVisible(noLogoRule, FocusedItem.System(noLogo), media))
    }

    @Test
    fun `a game reads its own system's art`() {
        val game = JarnLibraryGame(id = "g", systemId = "snes", title = "Game")
        assertTrue(themeLayerVisible(hasLogo, FocusedItem.Game(game, withLogo), media))
        assertTrue(themeLayerVisible(noLogoRule, FocusedItem.Game(game, noLogo), media))
    }

    @Test
    fun `missing with no presence check and the systems list stay as written`() {
        assertTrue(themeLayerVisible(ThemeVisibleWhen(missing = true), FocusedItem.System(withLogo), media))
        val other = noLogoRule.copy(systems = listOf("snes"))
        assertFalse(themeLayerVisible(other, FocusedItem.System(noLogo), media))
        val mediaMissing = ThemeVisibleWhen(hasMedia = MediaKind.Video, missing = true)
        assertTrue(themeLayerVisible(mediaMissing, FocusedItem.Game(JarnLibraryGame(id = "g", systemId = "snes", title = "Game")), media))
    }

    @Test
    fun `Big shelf shows the logo or the name, never both`() {
        val logo = BigShelfSystems.layers.single { it.id == "logo" }
        val title = BigShelfSystems.layers.single { it.id == "title" }
        listOf(withLogo, noLogo).forEach { system ->
            val focus = FocusedItem.System(system)
            val shown = listOf(logo, title).filter { themeLayerVisible(it.visibleWhen, focus, media) }.map { it.id }
            assertEquals(if (system.logoPath != null) listOf("logo") else listOf("title"), shown)
        }
    }
}
