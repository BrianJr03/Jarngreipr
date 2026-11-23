package jr.brian.jarnfe.look

import androidx.compose.ui.graphics.Color
import jr.brian.core.model.CommandPanelPalette
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.resolvedColors
import jr.brian.jarnfe.settings.nextFocusedLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

private val DarkPalette = CommandPanelPalette(
    isDark = true,
    bg = 0xFF101418.toInt(),
    rail = 0xFF14191E.toInt(),
    surface = 0xFF1A2026.toInt(),
    surfaceHigh = 0xFF232B33.toInt(),
    border = 0xFF39434D.toInt(),
    accent = 0xFF6FB6FF.toInt(),
    accentSoft = 0xFF25405A.toInt(),
    accentFocused = 0xFF9CCEFF.toInt(),
    onAccent = 0xFF08131E.toInt(),
    textPrimary = 0xFFF2F5F8.toInt(),
    textSecondary = 0xFFC9D2DB.toInt(),
    textTertiary = 0xFFAAB5C0.toInt(),
    danger = 0xFFFF8A80.toInt(),
    success = 0xFF7BE0A3.toInt(),
)

class JarnAppearanceTest {
    @Test
    fun `saved look applies without a preview`() {
        assertSame(JarnLooks.Grove, resolveJarnLook("grove", null))
    }

    @Test
    fun `preview wins over the saved look`() {
        assertSame(JarnLooks.Night, resolveJarnLook("grove", "night"))
    }

    @Test
    fun `missing and unknown looks fall back to fjord`() {
        assertSame(JarnLooks.Fjord, resolveJarnLook(null, null))
        assertSame(JarnLooks.Fjord, resolveJarnLook("future", null))
    }

    @Test
    fun `a matched palette recolors the saved look and keeps its id`() {
        val look = resolveJarnLook("grove", null, DarkPalette)
        assertEquals("grove", look.id)
        assertEquals(Color(DarkPalette.bg), look.resolvedColors.bg)
    }

    @Test
    fun `live preview wins over the matched palette`() {
        assertSame(JarnLooks.Night, resolveJarnLook("grove", "night", DarkPalette))
    }

    @Test
    fun `no palette returns the saved look exactly`() {
        assertSame(JarnLooks.Grove, resolveJarnLook("grove", null, null))
    }

    @Test
    fun `moving between cards keeps the newly focused look in either event order`() {
        assertEquals("night", nextFocusedLook(nextFocusedLook("forge", "forge", false), "night", true))
        assertEquals("night", nextFocusedLook(nextFocusedLook("forge", "night", true), "forge", false))
    }

    @Test
    fun `leaving the picker clears the preview`() {
        assertNull(nextFocusedLook("forge", "forge", false))
    }
}
