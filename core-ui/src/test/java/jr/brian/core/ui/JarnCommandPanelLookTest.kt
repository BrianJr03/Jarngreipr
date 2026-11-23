package jr.brian.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import jr.brian.core.model.CommandPanelPalette
import jr.brian.core.ui.theme.JarnCardStyle
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnFocusGapColor
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.core.ui.theme.adaptCommandPanelPalette
import jr.brian.core.ui.theme.jarnBlend
import jr.brian.core.ui.theme.jarnContrastRatio
import jr.brian.core.ui.theme.resolvedColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private const val RANDOM_SEED = 361
private const val RANDOM_RUNS = 5_000
private const val OPAQUE = 0xFF shl 24

class JarnCommandPanelLookTest {

    private val looks = listOf(JarnLooks.Forge, JarnLooks.Fjord, JarnLooks.Grove, JarnLooks.Night)

    private val stockBlack = CommandPanelPalette(
        bg = argb(0xFF000000), rail = argb(0xFF0B0B0B), surface = argb(0xFF161616),
        surfaceHigh = argb(0xFF3A3A3A), border = argb(0xFF383838), accent = argb(0xCB6A0D83),
        accentSoft = argb(0x246A0D83), accentFocused = argb(0xFF8A3BA0), onAccent = argb(0xFFFFFFFF),
        textPrimary = argb(0xFFFFFFFF), textSecondary = argb(0xFFE0E0E0), textTertiary = argb(0xFFB0B0B0),
        danger = argb(0xFFF64646), success = argb(0xFF03C82E), isDark = true,
    )

    private val stockLight = CommandPanelPalette(
        bg = argb(0xFFFFFFFF), rail = argb(0xFFFAFAFA), surface = argb(0xFFF4F4F4),
        surfaceHigh = argb(0xFFD5D5D5), border = argb(0xFFD8D8D8), accent = argb(0xFF8E84FF),
        accentSoft = argb(0x248E84FF), accentFocused = argb(0xFF7F76E0), onAccent = argb(0xFF141414),
        textPrimary = argb(0xFF434343), textSecondary = argb(0xFF767676), textTertiary = argb(0xFF8A8A8A),
        danger = argb(0xFFD32F2F), success = argb(0xFF04AD29), isDark = false,
    )

    @Test
    fun `missing palette keeps the saved look`() = looks.forEach { saved ->
        val result = adaptCommandPanelPalette(null, saved)
        assertSame(saved, result.look)
        assertEquals(JarnPaletteMatch.SavedLook, result.match)
    }

    @Test
    fun `empty or single valued rows keep the saved look`() {
        val empty = uniformPalette(0)
        val flat = uniformPalette(argb(0xFF808080))
        looks.forEach { saved ->
            listOf(empty, flat, stockBlack.copy(accent = 0)).forEach { bad ->
                val result = adaptCommandPanelPalette(bad, saved)
                assertSame(saved, result.look)
                assertEquals(JarnPaletteMatch.SavedLook, result.match)
            }
        }
    }

    @Test
    fun `stock black gets the full palette`() = looks.forEach { saved ->
        val result = adaptCommandPanelPalette(stockBlack, saved)
        assertEquals(JarnPaletteMatch.FullPalette, result.match)
        assertEquals(Color(stockBlack.bg), result.look.colors.bg)
        assertEquals(Color(stockBlack.surfaceHigh), result.look.colors.raised)
        assertEquals(Color(stockBlack.textPrimary), result.look.colors.textHigh)
        assertReadable(saved.id, result.look.resolvedColors)
    }

    @Test
    fun `stock light puts only the accent on the saved look`() = looks.forEach { saved ->
        val result = adaptCommandPanelPalette(stockLight, saved)
        val colors = result.look.resolvedColors
        assertEquals(JarnPaletteMatch.AccentOnSavedLook, result.match)
        assertEquals(
            saved.resolvedColors,
            colors.copy(
                accent = saved.resolvedColors.accent,
                onAccent = saved.resolvedColors.onAccent,
                focus = saved.resolvedColors.focus,
            ),
        )
        assertReadable(saved.id, colors)
    }

    @Test
    fun `light colors flagged dark fall back to the accent`() = looks.forEach { saved ->
        val result = adaptCommandPanelPalette(stockLight.copy(isDark = true), saved)
        assertEquals(JarnPaletteMatch.AccentOnSavedLook, result.match)
        assertReadable(saved.id, result.look.resolvedColors)
    }

    @Test
    fun `unrepairable custom palette falls back to the accent`() = looks.forEach { saved ->
        val lowContrast = stockBlack.copy(
            textSecondary = argb(0xFF505050),
            textTertiary = argb(0xFF404040),
        )
        val result = adaptCommandPanelPalette(lowContrast, saved)
        assertEquals(JarnPaletteMatch.AccentOnSavedLook, result.match)
        assertReadable(saved.id, result.look.resolvedColors)
    }

    @Test
    fun `transparent values become opaque`() {
        val translucent = stockBlack.copy(
            surface = stockBlack.surface and 0x80FFFFFF.toInt(),
            textPrimary = stockBlack.textPrimary and 0x40FFFFFF,
            border = stockBlack.border and 0x20FFFFFF,
            danger = stockBlack.danger and 0x00FFFFFF,
        )
        val result = adaptCommandPanelPalette(translucent, JarnLooks.Fjord)
        assertEquals(JarnPaletteMatch.FullPalette, result.match)
        val colors = result.look.colors
        listOf(colors.surface, colors.textHigh, colors.line, colors.danger, colors.accent).forEach {
            assertEquals(1f, it.alpha)
        }
    }

    @Test
    fun `saved look identity survives and is not mutated`() {
        val saved = JarnLooks.Forge.copy(
            tileShape = RectangleShape,
            cardStyle = JarnCardStyle.Outlined,
            focusColor = Color(0xFF00FF00),
        )
        val snapshot = saved.copy()
        listOf(stockBlack, stockLight).forEach { palette ->
            val look = adaptCommandPanelPalette(palette, saved).look
            assertEquals(saved.id, look.id)
            assertEquals(saved.nameRes, look.nameRes)
            assertEquals(saved.tileShape, look.tileShape)
            assertEquals(saved.cardStyle, look.cardStyle)
            assertEquals(look.colors.focus, look.resolvedColors.focus)
            assertReadable(saved.id, look.resolvedColors)
        }
        assertEquals(snapshot, saved)
    }

    @Test
    fun `mid tone accent still gets readable content`() = looks.forEach { saved ->
        (0x60..0xA0).forEach { channel ->
            val gray = OPAQUE or (channel shl 16) or (channel shl 8) or channel
            val colors = adaptCommandPanelPalette(stockLight.copy(accent = gray), saved).look.resolvedColors
            assertReadable("${saved.id} gray $channel", colors)
        }
    }

    @Test
    fun `random accents stay readable on every look`() {
        val random = Random(RANDOM_SEED)
        repeat(RANDOM_RUNS) {
            val accent = OPAQUE or random.nextInt(0x1000000)
            looks.forEach { saved ->
                listOf(stockBlack, stockLight).forEach { base ->
                    val colors = adaptCommandPanelPalette(base.copy(accent = accent), saved).look.resolvedColors
                    assertReadable("${saved.id} accent ${accent.toUInt().toString(16)}", colors)
                }
            }
        }
    }

    @Test
    fun `random palettes always resolve to a readable look`() {
        val random = Random(RANDOM_SEED)
        val matches = mutableSetOf<JarnPaletteMatch>()
        repeat(RANDOM_RUNS) {
            val palette = randomPalette(random)
            looks.forEach { saved ->
                val result = adaptCommandPanelPalette(palette, saved)
                matches += result.match
                assertReadable("${saved.id} $palette", result.look.resolvedColors)
            }
        }
        assertTrue(JarnPaletteMatch.FullPalette in matches)
        assertTrue(JarnPaletteMatch.AccentOnSavedLook in matches)
    }

    private fun assertReadable(label: String, colors: JarnColors) {
        val tonal = listOf(colors.bg, colors.surface, colors.raised)
        val readable = listOf(
            colors.textHigh, colors.textMid, colors.textLow, colors.accent, colors.danger, colors.success,
        )
        readable.forEach { foreground ->
            assertEquals("$label opaque $foreground", 1f, foreground.alpha)
            tonal.forEach { background ->
                val ratio = jarnContrastRatio(foreground, background)
                assertTrue("$label $foreground on $background = $ratio", ratio >= 4.5)
            }
        }
        tonal.forEach { assertEquals("$label opaque $it", 1f, it.alpha) }
        assertTrue(label, jarnContrastRatio(colors.onAccent, colors.accent) >= 4.5)
        assertTrue(label, jarnContrastRatio(colors.onDanger, colors.danger) >= 4.5)
        assertEquals(label, JarnFocusGapColor, colors.focusGap)
        assertTrue(label, jarnContrastRatio(colors.focus, colors.focusGap) >= 3.0)
        tonal.forEach { assertTrue(label, jarnContrastRatio(colors.focus, it) >= 3.0) }
        val scrimOnWhite = jarnBlend(colors.artScrim, Color.White)
        assertTrue(label, jarnContrastRatio(colors.textHigh, scrimOnWhite) >= 4.5)
        assertTrue(label, jarnContrastRatio(colors.textMid, scrimOnWhite) >= 4.5)
        val selectedTint = jarnBlend(colors.textHigh.copy(alpha = 0.14f), colors.surface)
        assertTrue(label, jarnContrastRatio(colors.textHigh, selectedTint) >= 4.5)
    }

    private fun randomPalette(random: Random): CommandPanelPalette {
        fun dark() = OPAQUE or (random.nextInt(0x50) shl 16) or (random.nextInt(0x50) shl 8) or random.nextInt(0x50)
        fun light() = OPAQUE or
            ((0xA0 + random.nextInt(0x60)) shl 16) or
            ((0xA0 + random.nextInt(0x60)) shl 8) or
            (0xA0 + random.nextInt(0x60))
        fun any() = random.nextInt()
        return CommandPanelPalette(
            bg = dark(), rail = dark(), surface = dark(), surfaceHigh = dark(), border = any(),
            accent = any(), accentSoft = any(), accentFocused = any(), onAccent = any(),
            textPrimary = light(), textSecondary = light(), textTertiary = light(),
            danger = any(), success = any(), isDark = random.nextInt(4) != 0,
        )
    }

    private fun uniformPalette(value: Int) = CommandPanelPalette(
        bg = value, rail = value, surface = value, surfaceHigh = value, border = value,
        accent = value, accentSoft = value, accentFocused = value, onAccent = value,
        textPrimary = value, textSecondary = value, textTertiary = value,
        danger = value, success = value, isDark = true,
    )

    private fun argb(value: Long): Int = value.toInt()
}
