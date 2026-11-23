package jr.brian.jarnfe.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HearthriseSceneTest {
    private val scene = HearthriseScene

    @Test
    fun `the seeded embers match the approved design`() {
        val embers = scene.Embers
        assertEquals(152, embers.size)
        assertEquals(139.65355f, embers[0], 0.001f)
        assertEquals(28.612764f, embers[1], 0.001f)
        assertEquals(0.53261f, embers[2], 0.001f)
        assertEquals(4.236188f, embers[3], 0.001f)
        assertEquals(-242.86496f, embers[148], 0.001f)
        assertEquals(34.884323f, embers[149], 0.001f)
        assertEquals(0.7193126f, embers[150], 0.001f)
        assertEquals(5.8595366f, embers[151], 0.001f)
    }

    @Test
    fun `the seeded seams match the approved design`() {
        val seams = scene.Seams
        assertEquals(98, seams.size)
        assertEquals(-618.6725f, seams[0], 0.001f)
        assertEquals(1953.9087f, seams[1], 0.001f)
        assertEquals(-168.26642f, seams[12], 0.001f)
        assertEquals(2019.776f, seams[13], 0.001f)
        assertEquals(492.07849f, seams[84], 0.001f)
        assertEquals(1944.4938f, seams[85], 0.001f)
        assertEquals(968.5771f, seams[96], 0.001f)
        assertEquals(1919.0396f, seams[97], 0.001f)
    }

    @Test
    fun `seams stay on the bottom screen`() {
        for (i in 1 until scene.Seams.size step 2) {
            assertTrue(scene.Seams[i] > JARN_WALLPAPER_SCREEN_HEIGHT)
        }
    }

    @Test
    fun `live time starts on the still frame`() {
        assertEquals(HEARTHRISE_STILL_SECONDS, hearthriseSeconds(0L, isStill = false), 0.0)
        assertEquals(HEARTHRISE_STILL_SECONDS, hearthriseSeconds(5_000_000_000L, isStill = true), 0.0)
        assertEquals(69.0, hearthriseSeconds(5_000_000_000L, isStill = false), 1e-9)
    }

    @Test
    fun `embers rise across the hinge and stop below the top title area`() {
        assertEquals(2150f, hearthriseEmberY(0f), 0f)
        val highest = hearthriseEmberY(scene.EMBER_RANGE)
        assertEquals(210f, highest, 0f)
        val largestAtEnd = hearthriseEmberRadius(11.2f, 1f, 1f)
        assertTrue(highest - largestAtEnd >= TOP_BAND_SOLID_END)
    }

    @Test
    fun `an ember near the hinge is drawn on both screens`() {
        assertTrue(hearthriseEmberIsOnScreen(1084f, top = 0f))
        assertTrue(hearthriseEmberIsOnScreen(1076f, top = 1080f))
        assertFalse(hearthriseEmberIsOnScreen(1200f, top = 0f))
        assertFalse(hearthriseEmberIsOnScreen(900f, top = 1080f))
    }

    @Test
    fun `travel wraps inside its range`() {
        for (step in 0 until 4000) {
            val travel = hearthriseTravel(step * 0.5, 37f, 0.9f)
            assertTrue(travel >= 0f && travel < scene.EMBER_RANGE)
        }
    }

    @Test
    fun `embers fade in and out through three levels`() {
        assertEquals(0, hearthriseEmberLevel(0f))
        assertEquals(2, hearthriseEmberLevel(0.5f))
        assertEquals(0, hearthriseEmberLevel(0.99f))
        assertEquals(setOf(0, 1, 2), (0 until 100).map { hearthriseEmberLevel(it / 100f) }.toSet())
    }

    @Test
    fun `embers shrink as they rise but never vanish`() {
        assertEquals(10f, hearthriseEmberRadius(10f, 0f, 1f), 0.001f)
        assertEquals(6f, hearthriseEmberRadius(10f, 1f, 1f), 0.001f)
        assertEquals(1.8f, hearthriseEmberRadius(1f, 1f, 2f), 0.001f)
    }

    @Test
    fun `coal stops map the design's inner radius`() {
        val stops = hearthriseCoalStops()
        assertEquals(30f / 760f, stops[0], 0.0001f)
        assertEquals((30f + 0.46f * 730f) / 760f, stops[1], 0.0001f)
        assertEquals(1f, stops[2], 0f)
    }

    @Test
    fun `coals and seams breathe inside their designed range`() {
        for (step in 0 until 1000) {
            val seconds = step * 0.25
            assertTrue(hearthriseCoalAlpha(seconds, 2.1f) in 0.459f..0.781f)
            assertTrue(hearthriseSeamAlpha(seconds) in 0.279f..0.561f)
        }
    }

    private companion object {
        const val TOP_BAND_SOLID_END = 124.2f
    }
}
