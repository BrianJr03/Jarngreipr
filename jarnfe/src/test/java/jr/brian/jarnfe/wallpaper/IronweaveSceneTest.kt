package jr.brian.jarnfe.wallpaper

import jr.brian.core.ui.theme.JarnLooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IronweaveSceneTest {
    @Test
    fun `the still frame parks the sheen where the design does`() {
        assertEquals(-340f, ironweaveSheenPosition(ironweaveSeconds(987_654L, isStill = true)), 0.01f)
    }

    @Test
    fun `the first live frame matches the still`() {
        val still = ironweaveSheenPosition(ironweaveSeconds(0L, isStill = true))
        val live = ironweaveSheenPosition(ironweaveSeconds(0L, isStill = false))
        assertEquals(still, live, 0f)
    }

    @Test
    fun `the sheen moves at the designed speed and loops`() {
        val start = ironweaveSheenPosition(10.0)
        assertEquals(start + 72f, ironweaveSheenPosition(11.0), 0.01f)
        assertEquals(start, ironweaveSheenPosition(10.0 + 4300.0 / 72.0), 0.01f)
    }

    @Test
    fun `one pass crosses both screens`() {
        assertEquals(-1800f, ironweaveSheenPosition(0.0), 0f)
        assertTrue(ironweaveSheenPosition(4299.0 / 72.0) > 2400f)
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
