package jr.brian.core.ui

import androidx.compose.ui.graphics.Color
import jr.brian.core.ui.theme.JarnCaptionScrimColor
import jr.brian.core.ui.theme.JarnCaptionTextColor
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnFocusGapColor
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.jarnBlend
import jr.brian.core.ui.theme.jarnContrastRatio
import jr.brian.core.ui.theme.jarnOnColor
import jr.brian.core.ui.theme.resolvedColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EIGHT_BIT_ALPHA_TOLERANCE = 0.08

class JarnContrastTest {

    private data class Pair(
        val name: String,
        val target: Double,
        val measured: List<Double>,
        val ratio: (JarnColors) -> Double,
    )

    private val looks = listOf(JarnLooks.Forge, JarnLooks.Fjord, JarnLooks.Grove, JarnLooks.Night)

    private fun onWhiteArt(text: Color, colors: JarnColors) =
        jarnContrastRatio(text, jarnBlend(colors.artScrim, Color.White))

    private val specPairs = listOf(
        Pair("textHigh/bg", 4.5, listOf(16.39, 16.49, 16.53, 21.00)) { jarnContrastRatio(it.textHigh, it.bg) },
        Pair("textHigh/raised", 4.5, listOf(13.26, 12.49, 12.58, 16.67)) { jarnContrastRatio(it.textHigh, it.raised) },
        Pair("textMid/surface", 4.5, listOf(9.28, 9.51, 9.88, 11.20)) { jarnContrastRatio(it.textMid, it.surface) },
        Pair("textMid/raised", 4.5, listOf(8.20, 8.10, 8.43, 9.96)) { jarnContrastRatio(it.textMid, it.raised) },
        Pair("textLow/bg", 4.5, listOf(6.48, 7.03, 7.22, 8.03)) { jarnContrastRatio(it.textLow, it.bg) },
        Pair("textLow/surface", 4.5, listOf(5.92, 6.25, 6.43, 7.16)) { jarnContrastRatio(it.textLow, it.surface) },
        Pair("textLow/raised", 4.5, listOf(5.24, 5.33, 5.49, 6.38)) { jarnContrastRatio(it.textLow, it.raised) },
        Pair("accent/surface", 3.0, listOf(7.33, 8.66, 9.71, 15.17)) { jarnContrastRatio(it.accent, it.surface) },
        Pair("onAccent/accent", 4.5, listOf(8.13, 9.95, 11.16, 15.33)) { jarnContrastRatio(it.onAccent, it.accent) },
        Pair("focus/focusGap", 3.0, listOf(11.08, 13.74, 15.37, 19.90)) { jarnContrastRatio(it.focus, it.focusGap) },
        Pair("danger/surface", 4.5, listOf(6.71, 6.80, 6.94, 7.52)) { jarnContrastRatio(it.danger, it.surface) },
        Pair("success/surface", 4.5, listOf(10.00, 9.81, 10.08, 11.65)) { jarnContrastRatio(it.success, it.surface) },
        Pair("textHigh/scrim on white", 4.5, listOf(8.24, 8.23, 8.30, 11.73)) { onWhiteArt(it.textHigh, it) },
        Pair("textMid/scrim on white", 4.5, listOf(5.10, 5.34, 5.56, 7.01)) { onWhiteArt(it.textMid, it) },
    )

    private fun forEachLook(block: (JarnLook, JarnColors) -> Unit) =
        looks.forEach { block(it, it.resolvedColors) }

    @Test
    fun `every spec pair meets its target in every look`() = forEachLook { look, colors ->
        specPairs.forEach { pair ->
            val ratio = pair.ratio(colors)
            assertTrue("${look.id} ${pair.name} = $ratio", ratio >= pair.target)
        }
    }

    @Test
    fun `computed ratios match the published spec table`() {
        specPairs.forEach { pair ->
            looks.forEachIndexed { index, look ->
                assertEquals("${look.id} ${pair.name}", pair.measured[index], pair.ratio(look.resolvedColors), 0.03)
            }
        }
    }

    @Test
    fun `every text role passes on every tonal surface`() = forEachLook { look, colors ->
        listOf(colors.textHigh, colors.textMid, colors.textLow).forEach { text ->
            listOf(colors.bg, colors.surface, colors.raised).forEach { background ->
                val ratio = jarnContrastRatio(text, background)
                assertTrue("${look.id} $text on $background = $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun `button content passes on its container`() = forEachLook { look, colors ->
        assertTrue(look.id, jarnContrastRatio(colors.onAccent, colors.accent) >= 4.5)
        assertTrue(look.id, jarnContrastRatio(colors.onDanger, colors.danger) >= 4.5)
    }

    @Test
    fun `selected tab text passes on its tinted background`() = forEachLook { look, colors ->
        val tint = jarnBlend(colors.textHigh.copy(alpha = 0.14f), colors.surface)
        assertTrue(look.id, jarnContrastRatio(colors.textHigh, tint) >= 4.5)
    }

    @Test
    fun `tile captions pass over pure white art`() {
        val scrim = jarnBlend(JarnCaptionScrimColor, Color.White)
        val white = jarnContrastRatio(Color.White, scrim)
        val caption = jarnContrastRatio(JarnCaptionTextColor, scrim)
        assertTrue("white = $white", white >= 4.5)
        assertTrue("caption = $caption", caption >= 4.5)
        assertEquals(12.49, white, EIGHT_BIT_ALPHA_TOLERANCE)
        assertEquals(9.38, caption, EIGHT_BIT_ALPHA_TOLERANCE)
    }

    @Test
    fun `focus gap is the same in every look`() = forEachLook { _, colors ->
        assertEquals(JarnFocusGapColor, colors.focusGap)
    }

    @Test
    fun `on color picks the readable side`() {
        assertEquals(Color(0xFF14100C), jarnOnColor(Color(0xFF5CC8F5)))
        assertEquals(Color.White, jarnOnColor(Color(0xFF101010)))
    }
}
