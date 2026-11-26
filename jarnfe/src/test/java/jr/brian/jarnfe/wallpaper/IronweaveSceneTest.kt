package jr.brian.jarnfe.wallpaper

import jr.brian.core.ui.theme.JarnLooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.hypot

class IronweaveSceneTest {
    @Test
    fun `the wave moves at the designed speed and wraps each wavelength`() {
        assertEquals(80f, ironweaveWavePhase(1.0), 0.001f)
        assertEquals(ironweaveWavePhase(2.5) + 80f, ironweaveWavePhase(3.5), 0.001f)
        assertEquals(ironweaveWavePhase(2.5), ironweaveWavePhase(2.5 + 480.0 / 80.0), 0.001f)
        assertEquals(0f, ironweaveWavePhase(6.0), 0.001f)
        for (step in 0..200) {
            val phase = ironweaveWavePhase(step * 0.37)
            assertTrue(phase >= 0f && phase < 480f)
        }
    }

    @Test
    fun `the still frame parks a crest in the upper middle of the top screen`() {
        val phase = ironweaveWavePhase(ironweaveSeconds(987_654L, isStill = true))
        assertEquals(1f, ironweaveWaveLevelAt(0f, 400f, phase), 0.0001f)
        assertTrue(ironweaveWaveLevelAt(0f, 400f + 498f, phase) < 0.01f)
    }

    @Test
    fun `the first live frame matches the still`() {
        val still = ironweaveWavePhase(ironweaveSeconds(0L, isStill = true))
        val live = ironweaveWavePhase(ironweaveSeconds(0L, isStill = false))
        assertEquals(still, live, 0f)
    }

    @Test
    fun `the wave runs along the old diagonal`() {
        val angle = Math.toDegrees(atan2(IronweaveScene.WaveDirectionY, IronweaveScene.WaveDirectionX).toDouble())
        assertEquals(28.81, angle, 0.01)
        assertEquals(1f, hypot(IronweaveScene.WaveDirectionX, IronweaveScene.WaveDirectionY), 0.0001f)
    }

    @Test
    fun `the wave stops are symmetric and trough at both ends`() {
        val stops = IronweaveScene.WaveStops
        val levels = IronweaveScene.WaveLevels
        assertEquals(9, stops.size)
        assertEquals(0f, stops.first(), 0f)
        assertEquals(1f, stops.last(), 0f)
        assertEquals(0f, levels.first(), 0f)
        assertEquals(0f, levels.last(), 0f)
        assertEquals(1f, levels[4], 0.0001f)
        for (i in levels.indices) {
            assertEquals(levels[i], levels[levels.lastIndex - i], 0f)
            assertEquals(1f, stops[i] + stops[stops.lastIndex - i], 0.0001f)
        }
    }

    @Test
    fun `the wave colors run from the resting rings to a capped crest in every look`() {
        JarnLooks.All.forEach { look ->
            val palette = look.colors.toJarnWallpaperPalette()
            val colors = ironweaveWaveColors(palette)
            val crest = colors[IronweaveScene.WAVE_STOP_COUNT / 2]
            assertEquals(look.id, ironweaveRingBase(palette), colors.first())
            assertEquals(look.id, colors.first(), colors.last())
            for (i in colors.indices) assertEquals(look.id, colors[i], colors[colors.lastIndex - i])
            for (shift in listOf(16, 8, 0)) {
                assertTrue(look.id, (crest shr shift and 0xFF) <= (palette.glow shr shift and 0xFF))
            }
        }
    }

    @Test
    fun `the wave flows from the top screen into the bottom one`() {
        val phase = ironweaveWavePhase(17.3)
        val topEdge = jarnWallpaperOriginY(isBottomScreen = false) + JARN_WALLPAPER_SCREEN_HEIGHT
        val bottomTop = jarnWallpaperOriginY(isBottomScreen = true)
        for (x in listOf(-900f, -250f, 0f, 610f, 900f)) {
            assertEquals(ironweaveWaveLevelAt(x, topEdge, phase), ironweaveWaveLevelAt(x, bottomTop, phase), 0f)
        }
    }

    @Test
    fun `the tile keeps its designed size and never collapses`() {
        assertEquals(58, ironweaveTilePixels(1f))
        assertEquals(29, ironweaveTilePixels(0.5f))
        assertEquals(110f, ironweaveTileSize(0.1f), 0.001f)
        assertTrue(ironweaveTilePixels(0.01f) >= 8)
    }

    @Test
    fun `resting rings sit between the background and the glow in every look`() {
        JarnLooks.All.forEach { look ->
            val palette = look.colors.toJarnWallpaperPalette()
            val ring = ironweaveRingBase(palette)
            for (shift in listOf(16, 8, 0)) {
                val channel = ring shr shift and 0xFF
                assertTrue(look.id, channel >= (palette.bg shr shift and 0xFF))
                assertTrue(look.id, channel <= (palette.glow shr shift and 0xFF))
            }
        }
    }
}
