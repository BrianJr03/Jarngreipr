package jr.brian.jarnfe.backdrop

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.jarnContrastRatio
import jr.brian.core.ui.theme.jarnRelativeLuminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnAdaptiveToneTest {
    private val colors = JarnLooks.Fjord.colors

    private fun luminanceOf(argb: Long) = jarnPixelLuminance(argb.toInt())

    private fun toneOver(argb: Long, current: JarnTextTone = JarnTextTone.Theme) =
        jarnTextToneOver(colors, listOf(luminanceOf(argb)), current)

    @Test
    fun `pixel luminance follows WCAG`() {
        assertEquals(1.0, luminanceOf(0xFFFFFFFF), 1e-9)
        assertEquals(0.0, luminanceOf(0xFF000000), 1e-9)
        assertEquals(0.2126, luminanceOf(0xFFFF0000), 1e-4)
        assertEquals(0.0722, luminanceOf(0xFF0000FF), 1e-4)
        for (argb in listOf(0xFF777777, 0xFF3A7BD5, 0xFFE8C547)) {
            assertEquals(jarnRelativeLuminance(Color(argb)), luminanceOf(argb), 1e-6)
        }
    }

    @Test
    fun `see-through pixels read as lying over black`() {
        assertEquals(0.0, luminanceOf(0x00FFFFFF), 1e-9)
        assertEquals(luminanceOf(0xFF808080), luminanceOf(0x80FFFFFF), 1e-9)
    }

    @Test
    fun `contrast runs from 1 to 21 either way round`() {
        assertEquals(21.0, jarnContrastRatio(1.0, 0.0), 1e-9)
        assertEquals(21.0, jarnContrastRatio(0.0, 1.0), 1e-9)
        assertEquals(1.0, jarnContrastRatio(0.3, 0.3), 1e-9)
        assertEquals(jarnContrastRatio(Color.White, Color.Red), jarnContrastRatio(1.0, 0.2126), 1e-3)
    }

    @Test
    fun `white backgrounds get dark text and black ones keep the look's`() {
        assertEquals(JarnTextTone.Inverse, toneOver(0xFFFFFFFF))
        assertEquals(JarnTextTone.Theme, toneOver(0xFF000000))
        assertEquals(JarnTextTone.Theme, toneOver(0xFF000000, current = JarnTextTone.Inverse))
    }

    @Test
    fun `saturated red takes dark text and saturated blue keeps light`() {
        assertEquals(JarnTextTone.Inverse, toneOver(0xFFFF0000))
        assertEquals(JarnTextTone.Theme, toneOver(0xFF0000FF))
        assertEquals(JarnTextTone.Theme, toneOver(0xFF0000FF, current = JarnTextTone.Inverse))
    }

    @Test
    fun `mid grey holds whichever tone is showing`() {
        assertEquals(JarnTextTone.Theme, toneOver(0xFF777777, current = JarnTextTone.Theme))
        assertEquals(JarnTextTone.Inverse, toneOver(0xFF777777, current = JarnTextTone.Inverse))
    }

    @Test
    fun `the other tone must win by the margin before it shows`() {
        val margin = JARN_TONE_SWITCH_MARGIN
        assertEquals(JarnTextTone.Theme, jarnPickTextTone(JarnTextTone.Theme, 4.0, 4.0 * margin - 0.01))
        assertEquals(JarnTextTone.Inverse, jarnPickTextTone(JarnTextTone.Theme, 4.0, 4.0 * margin + 0.01))
        assertEquals(JarnTextTone.Inverse, jarnPickTextTone(JarnTextTone.Inverse, 4.0 * margin - 0.01, 4.0))
        assertEquals(JarnTextTone.Theme, jarnPickTextTone(JarnTextTone.Inverse, 4.0 * margin + 0.01, 4.0))
    }

    @Test
    fun `the worst label decides and no labels keep the tone`() {
        val text = colors.textHigh
        val worst = jarnWorstContrast(text, listOf(0.0, 1.0, 0.5))
        assertEquals(jarnContrastRatio(jarnRelativeLuminance(text), 1.0), worst, 1e-9)
        assertEquals(JarnTextTone.Inverse, jarnTextToneOver(colors, emptyList(), JarnTextTone.Inverse))
    }

    @Test
    fun `inverse text and key caps stay readable in every look`() {
        assertSame(colors, jarnTextColorsFor(colors, JarnTextTone.Theme))
        JarnLooks.All.forEach { look ->
            val inverse = jarnTextColorsFor(look.colors, JarnTextTone.Inverse)
            assertTrue(look.id, jarnContrastRatio(inverse.textHigh, Color.White) >= 4.5)
            assertTrue(look.id, jarnContrastRatio(inverse.textHigh, inverse.raised) >= 4.5)
            assertEquals(look.id, look.colors.bg, inverse.bg)
        }
    }

    @Test
    fun `label rects average the pixels under them`() {
        val white = Color.White.toArgb()
        val black = Color.Black.toArgb()
        val pixels = JarnBackdropPixels(4, 2, 2f, intArrayOf(white, white, black, black, white, white, black, black))
        assertEquals(1.0, pixels.meanLuminance(Rect(0f, 0f, 4f, 4f))!!, 1e-9)
        assertEquals(0.0, pixels.meanLuminance(Rect(4f, 0f, 8f, 4f))!!, 1e-9)
        assertEquals(0.5, pixels.meanLuminance(Rect(-10f, -10f, 100f, 100f))!!, 1e-9)
        assertNull(pixels.meanLuminance(Rect(20f, 20f, 30f, 30f)))
    }

    @Test
    fun `samples stay at least one interval apart`() {
        val throttle = JarnSampleThrottle(JARN_ADAPTIVE_SAMPLE_MS)
        assertEquals(0L, throttle.waitMillis(1_000L))
        throttle.mark(1_000L)
        assertEquals(JARN_ADAPTIVE_SAMPLE_MS, throttle.waitMillis(1_000L))
        assertEquals(JARN_ADAPTIVE_SAMPLE_MS - 100, throttle.waitMillis(1_100L))
        assertEquals(0L, throttle.waitMillis(1_000L + JARN_ADAPTIVE_SAMPLE_MS))
        assertEquals(0L, throttle.waitMillis(5_000L))
        assertTrue(JARN_ADAPTIVE_SAMPLE_MS >= 200L)
    }
}
