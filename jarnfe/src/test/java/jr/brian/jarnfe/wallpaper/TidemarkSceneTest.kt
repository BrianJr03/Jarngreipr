package jr.brian.jarnfe.wallpaper

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnLooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TidemarkSceneTest {
    private val period = TidemarkScene.SWELL_PERIOD_SECONDS
    private val wavelength = TidemarkScene.WAVELENGTH

    @Test
    fun `a crest passes a point every swell period and the phase wraps`() {
        assertEquals(8.0, period, 0.0)
        val start = tidemarkPhase(1.0)
        assertEquals(start + wavelength / 8f, tidemarkPhase(2.0), 0.01f)
        assertEquals(start, tidemarkPhase(1.0 + period * 5), 0.01f)
        for (step in 0..500) {
            val phase = tidemarkPhase(step * 0.113)
            assertTrue(phase >= 0f && phase < wavelength)
        }
        val point = 1.7
        assertEquals(
            tidemarkLevel(200f, 300f, tidemarkPhase(point)),
            tidemarkLevel(200f, 300f, tidemarkPhase(point + period)),
            0.0001f,
        )
    }

    @Test
    fun `the level is 1 on a crest, 0 in a trough and symmetric`() {
        val dx = TidemarkScene.DIR_X
        val dy = TidemarkScene.DIR_Y
        assertEquals(1f, tidemarkLevel(0f, 0f, 0f), 0.0001f)
        val half = wavelength / 2f
        assertEquals(0f, tidemarkLevel(half * dx, half * dy, 0f), 0.0001f)
        for (d in listOf(20f, 60f, 140f)) {
            assertEquals(
                tidemarkLevel(d * dx, d * dy, 0f),
                tidemarkLevel(-d * dx, -d * dy, 0f),
                0.0001f,
            )
        }
    }

    @Test
    fun `size and colour stay between rest and crest`() {
        assertEquals(0f, tidemarkCrestAmount(0f), 0f)
        assertEquals(0f, tidemarkCrestAmount(TidemarkScene.CREST_START), 0f)
        assertEquals(1f, tidemarkCrestAmount(1f), 0f)
        assertEquals(TidemarkScene.BASE_HALF, tidemarkHalfSize(0f), 0f)
        assertEquals(TidemarkScene.CREST_HALF, tidemarkHalfSize(1f), 0f)
        assertTrue(TidemarkScene.CREST_HALF + TidemarkScene.STROKE < TidemarkScene.CELL / 2f)
        var previous = 0f
        for (step in 0..100) {
            val amount = tidemarkCrestAmount(step / 100f)
            assertTrue(amount in 0f..1f && amount >= previous)
            previous = amount
        }
    }

    @Test
    fun `the crest colour never gets brighter than the glow in any look`() {
        JarnLooks.All.forEach { look ->
            val palette = look.colors.toJarnWallpaperPalette()
            val base = tidemarkBaseColor(palette)
            assertEquals(palette.glow, tidemarkMix(base, palette.glow, 1f))
            assertEquals(base, tidemarkMix(base, palette.glow, 0f))
            for (amount in listOf(0.25f, 0.5f, 0.75f)) {
                val mixed = tidemarkMix(base, palette.glow, amount)
                for (shift in listOf(16, 8, 0)) {
                    val low = minOf(base shr shift and 0xFF, palette.glow shr shift and 0xFF)
                    val high = maxOf(base shr shift and 0xFF, palette.glow shr shift and 0xFF)
                    assertTrue(look.id, (mixed shr shift and 0xFF) in low..high)
                }
            }
        }
    }

    @Test
    fun `few enough cells lift per frame on either screen`() {
        val top = crestCounts(TOP_HALF_WIDTH, 0f)
        val bottom = crestCounts(BOTTOM_HALF_WIDTH, JARN_WALLPAPER_SCREEN_HEIGHT)
        assertTrue("top max ${top.max()}", top.max() <= MAX_CREST_CELLS)
        assertTrue("bottom max ${bottom.max()}", bottom.max() <= MAX_CREST_CELLS)
        assertTrue(top.min() > 0 && bottom.min() > 0)
    }

    @Test
    fun `every cell keeps its shape and the mix is even`() {
        assertEquals(tidemarkKind(3, 7), tidemarkKind(3, 7))
        assertEquals(tidemarkKind(-1, -1), tidemarkKind(3, 3))
        assertEquals(tidemarkKind(2, 5), tidemarkKind(2 + 4 * 9, 5 + 4 * 30))
        for (i in 0 until TidemarkScene.PATTERN_SIZE) {
            val row = (0 until TidemarkScene.PATTERN_SIZE).map { tidemarkKind(it, i) }.toSet()
            val col = (0 until TidemarkScene.PATTERN_SIZE).map { tidemarkKind(i, it) }.toSet()
            assertEquals(TidemarkScene.KIND_COUNT, row.size)
            assertEquals(TidemarkScene.KIND_COUNT, col.size)
        }
    }

    @Test
    fun `the still frame parks a crest in the upper middle and the first live frame matches`() {
        val still = tidemarkSeconds(987_654_321L, isStill = true)
        assertEquals(still, tidemarkSeconds(0L, isStill = false), 0.0)
        assertNotEquals(still, tidemarkSeconds(1_000_000_000L, isStill = false), 0.0)
        val phase = tidemarkPhase(still)
        assertEquals(1f, tidemarkLevel(TidemarkScene.STILL_CREST_X, TidemarkScene.STILL_CREST_Y, phase), 0.001f)
    }

    @Test
    fun `the bottom screen continues the top screen's grid and wave`() {
        val topLast = tidemarkLastRow(0f)
        val bottomFirst = tidemarkFirstRow(JARN_WALLPAPER_SCREEN_HEIGHT)
        assertEquals(topLast + 1, bottomFirst)
        assertEquals(JARN_WALLPAPER_SCREEN_HEIGHT, tidemarkCellCenter(bottomFirst) - TidemarkScene.CELL / 2f, 0f)
        assertEquals(0f, JARN_WALLPAPER_SCREEN_HEIGHT % TidemarkScene.TILE_SIZE % TidemarkScene.CELL, 0f)

        val phase = tidemarkPhase(3.3)
        val out = FloatArray(tidemarkCrestCapacity(BOTTOM_HALF_WIDTH, JARN_WALLPAPER_SCREEN_HEIGHT))
        val count = tidemarkFillCrest(phase, BOTTOM_HALF_WIDTH, JARN_WALLPAPER_SCREEN_HEIGHT, out)
        for (i in 0 until count) {
            val at = i * TidemarkScene.CREST_STRIDE
            val x = out[at + TidemarkScene.CREST_X]
            val y = out[at + TidemarkScene.CREST_Y]
            assertTrue(y > JARN_WALLPAPER_SCREEN_HEIGHT)
            val expected = tidemarkCrestAmount(tidemarkLevel(x, y, phase))
            assertEquals(expected, out[at + TidemarkScene.CREST_AMOUNT], 0f)
        }
    }

    @Test
    fun `the picker lists it with a readable name`() {
        val entry = JarnLiveWallpapers.fromId("tidemark")
        assertNotNull(entry)
        val label = ApplicationProvider.getApplicationContext<Application>().getString(entry!!.nameRes)
        assertEquals("Tidemark", label)
    }

    private fun crestCounts(halfWidth: Float, top: Float): List<Int> {
        val out = FloatArray(tidemarkCrestCapacity(halfWidth, top))
        return (0 until 160).map { step ->
            tidemarkFillCrest(tidemarkPhase(step * period / 160), halfWidth, top, out)
        }
    }

    private companion object {
        const val TOP_HALF_WIDTH = 960f
        const val BOTTOM_HALF_WIDTH = 574f
        const val MAX_CREST_CELLS = 260
    }
}
