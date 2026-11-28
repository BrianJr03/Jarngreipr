package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.ui.theme.JarnLooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun color(argb: Long) = ThemeColor(argb.toInt())

private val Washed = ThemeColors(
    bg = color(0xFFB0B0B0),
    surface = color(0xFFC0C0C0),
    raised = color(0xFFD0D0D0),
    textHigh = color(0xFF909090),
    textMid = color(0xFF888888),
    textLow = color(0xFF808080),
    accent = color(0xFF3366FF),
    danger = color(0xFFFF5555),
    success = color(0xFF55CC77),
)

class ThemeEditorColorTest {
    @Test
    fun `hsl round trips within a step`() {
        listOf(0xFF3366FF, 0xFF101418, 0xFFFF8A80, 0xFF7BE0A3).map(::color).forEach { original ->
            val back = original.hsl.toThemeColor()
            listOf(16, 8, 0).forEach { shift ->
                val a = (original.argb shr shift) and 0xFF
                val b = (back.argb shr shift) and 0xFF
                assertTrue("$original vs $back", kotlin.math.abs(a - b) <= 1)
            }
        }
    }

    @Test
    fun `fix makes washed out colors readable and keeps the accent`() {
        assertFalse(Washed.isReadable)
        val fixed = Washed.fixedForReading()
        assertTrue(fixed.isReadable)
        assertEquals(Washed.accent, fixed.accent)
    }

    @Test
    fun `turning the look off starts from it, and named colors edit one at a time`() {
        val look = JarnLooks.Fjord.colors.toThemeColors()
        val own = editorWith().withFollowsLook(true, look).withFollowsLook(false, look)
        assertFalse(own.spec.meta.followsLook)
        assertEquals(look, own.spec.tokens.colors)
        assertTrue(own.entry.ownColors)
        val recolored = own.withColors(look.with(ThemeEditorColor.Accent, color(0xFF00FF00)))
        assertEquals(color(0xFF00FF00), recolored.spec.tokens.colors.get(ThemeEditorColor.Accent))
        assertEquals(look.bg, recolored.spec.tokens.colors.bg)
    }
}
