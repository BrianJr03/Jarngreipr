package jr.brian.jarnfe.wallpaper

import androidx.compose.ui.graphics.toArgb
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class JarnWallpaperPaletteTest {
    private fun paletteOf(colors: JarnColors) = jarnWallpaperPalette(
        bg = colors.bg.toArgb(),
        accent = colors.accent.toArgb(),
        focus = colors.focus.toArgb(),
        textMid = colors.textMid.toArgb(),
        textLow = colors.textLow.toArgb(),
    )

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun assertDesign(colors: JarnColors, glow: Int, line: Int, bandAlpha: Float) {
        val palette = paletteOf(colors)
        assertEquals(glow, palette.glow)
        assertEquals(line, palette.line)
        assertEquals(bandAlpha, palette.bandAlpha, 0.0001f)
    }

    @Test
    fun `each look matches the approved palette`() {
        assertDesign(JarnLooks.Fjord.colors, rgb(41, 82, 101), rgb(54, 78, 90), 0.43459774f)
        assertDesign(JarnLooks.Forge.colors, rgb(108, 63, 33), rgb(91, 66, 45), 0.5008571f)
        assertDesign(JarnLooks.Grove.colors, rgb(66, 86, 41), rgb(68, 81, 50), 0.4227073f)
        assertDesign(JarnLooks.Night.colors, rgb(83, 79, 72), rgb(76, 76, 76), 0.33224655f)
    }

    @Test
    fun `body text keeps 4_5 to 1 over the brightest mark in every look`() {
        JarnLooks.All.forEach { look ->
            val palette = paletteOf(look.colors)
            val text = look.colors.textMid.toArgb()
            assertTrue(look.id, contrast(text, palette.glow) >= 4.5)
            assertTrue(look.id, contrast(text, palette.line) >= 4.5)
            assertTrue(look.id, contrast(text, peak(palette)) >= 4.5)
        }
    }

    @Test
    fun `hints keep 4_5 to 1 over the band in every look`() {
        JarnLooks.All.forEach { look ->
            val palette = paletteOf(look.colors)
            val alpha = (palette.bandAlpha * 255).toInt() / 255.0
            val banded = over(palette.bg, peak(palette), alpha)
            assertTrue(look.id, contrast(look.colors.textLow.toArgb(), banded) >= 4.5)
        }
    }

    @Test
    fun `marks never sit darker than the background in any look`() {
        JarnLooks.All.forEach { look ->
            val palette = paletteOf(look.colors)
            for (shift in listOf(16, 8, 0)) {
                val bg = palette.bg shr shift and 0xFF
                assertTrue(look.id, (palette.glow shr shift and 0xFF) >= bg)
                assertTrue(look.id, (palette.line shr shift and 0xFF) >= bg)
            }
        }
    }

    @Test
    fun `the brightest art stays under the measured Forge ceiling`() {
        assertTrue(luminance(peak(paletteOf(JarnLooks.Forge.colors))) <= 0.077)
    }

    @Test
    fun `the mid glow sits between the background and the glow in every look`() {
        JarnLooks.All.forEach { look ->
            val palette = paletteOf(look.colors)
            for (shift in listOf(16, 8, 0)) {
                val mid = palette.midGlow shr shift and 0xFF
                assertTrue(look.id, mid >= (palette.bg shr shift and 0xFF))
                assertTrue(look.id, mid <= (palette.glow shr shift and 0xFF))
            }
        }
    }

    @Test
    fun `band stops line up with their colors`() {
        val palette = paletteOf(JarnLooks.Fjord.colors)
        for (isDeck in listOf(false, true)) {
            assertEquals(jarnWallpaperBandStops(isDeck).size, jarnWallpaperBandColors(palette, isDeck).size)
        }
    }

    private fun peak(palette: JarnWallpaperPalette): Int = rgb(
        max(palette.glow shr 16 and 0xFF, palette.line shr 16 and 0xFF),
        max(palette.glow shr 8 and 0xFF, palette.line shr 8 and 0xFF),
        max(palette.glow and 0xFF, palette.line and 0xFF),
    )

    private fun over(top: Int, under: Int, alpha: Double): Int {
        fun channel(shift: Int): Int {
            val a = top shr shift and 0xFF
            val b = under shr shift and 0xFF
            return kotlin.math.ceil(b + (a - b) * alpha).toInt()
        }
        return rgb(channel(16), channel(8), channel(0))
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(color: Int): Double {
        fun linear(shift: Int): Double {
            val value = (color shr shift and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(16) + 0.7152 * linear(8) + 0.0722 * linear(0)
    }
}
