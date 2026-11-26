package jr.brian.jarnfe.wallpaper

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TidemarkSceneTest {
    private val shapes = TidemarkScene.Shapes

    private fun rideCos() = cos(TidemarkScene.BandTiltDegrees[0] * PI / 180).toFloat()

    private fun value(i: Int, field: Int) = shapes[i * TidemarkScene.SHAPE_STRIDE + field]

    @Test
    fun `each swell advances at its own speed and wraps every period`() {
        for (band in TidemarkScene.BandLength.indices) {
            val length = TidemarkScene.BandLength[band]
            val period = TidemarkScene.BandPeriod[band]
            val start = tidemarkSwellOffset(1.0, band)
            assertEquals(start + length / period.toFloat() * 0.5f, tidemarkSwellOffset(1.5, band), 0.01f)
            assertEquals(start, tidemarkSwellOffset(1.0 + period * 3, band), 0.01f)
            for (step in 0..400) {
                val offset = tidemarkSwellOffset(step * 0.137, band)
                assertTrue(offset >= 0f && offset < length)
            }
        }
    }

    @Test
    fun `swell periods stay calm`() {
        TidemarkScene.BandPeriod.forEach { assertTrue(it in 6.0..10.0) }
    }

    @Test
    fun `the still frame is stable and matches the first live frame`() {
        val still = tidemarkSeconds(123_456_789L, isStill = true)
        assertEquals(TIDEMARK_STILL_SECONDS, still, 0.0)
        assertEquals(still, tidemarkSeconds(0L, isStill = true), 0.0)
        assertEquals(still, tidemarkSeconds(0L, isStill = false), 0.0)
        assertNotEquals(still, tidemarkSeconds(1_000_000_000L, isStill = false), 0.0)
    }

    @Test
    fun `the layout is the same every time`() {
        val kinds = IntArray(TidemarkScene.SHAPE_COUNT)
        val again = TidemarkScene.seededShapes(kinds)
        assertTrue(again.contentEquals(TidemarkScene.Shapes))
        assertTrue(kinds.contentEquals(TidemarkScene.Kinds))
        assertEquals(TidemarkScene.SHAPE_COUNT * TidemarkScene.SHAPE_STRIDE, shapes.size)
    }

    @Test
    fun `shapes stay inside the scene and cover both screens`() {
        var top = 0
        var bottom = 0
        for (i in 0 until TidemarkScene.SHAPE_COUNT) {
            val x = value(i, TidemarkScene.SHAPE_X)
            val y = value(i, TidemarkScene.SHAPE_Y)
            val reach = value(i, TidemarkScene.SHAPE_SIZE) + TidemarkScene.BOB
            assertTrue(y - reach >= 0f && y + reach <= TidemarkScene.SCENE_HEIGHT)
            if (y < JARN_WALLPAPER_SCREEN_HEIGHT) {
                top++
                assertTrue(y + reach <= JARN_WALLPAPER_SCREEN_HEIGHT)
                assertTrue(abs(x) + reach <= 960f)
            } else {
                bottom++
                assertTrue(y - reach >= JARN_WALLPAPER_SCREEN_HEIGHT)
                assertTrue(abs(x) + reach <= 560f)
            }
        }
        assertEquals(12, top)
        assertEquals(8, bottom)
    }

    @Test
    fun `every kind appears equally often`() {
        val counts = IntArray(TidemarkScene.KIND_COUNT)
        TidemarkScene.Kinds.forEach { counts[it]++ }
        assertTrue(counts.all { it == TidemarkScene.SHAPE_COUNT / TidemarkScene.KIND_COUNT })
    }

    @Test
    fun `bob, sway and squash stay within bounds`() {
        for (i in 0 until TidemarkScene.SHAPE_COUNT) {
            val x = value(i, TidemarkScene.SHAPE_X)
            val y = value(i, TidemarkScene.SHAPE_Y)
            val base = value(i, TidemarkScene.SHAPE_TURN)
            assertTrue(abs(base) <= TidemarkScene.MAX_TURN_DEGREES)
            for (step in 0..80) {
                val angle = tidemarkWaveAngle(x, y, tidemarkSwellOffset(step * 0.1, 0))
                assertTrue(abs(tidemarkLift(angle)) <= TidemarkScene.BOB + 0.001f)
                assertTrue(abs(tidemarkTurn(angle, base) - base) <= TidemarkScene.SWAY_DEGREES + 0.001f)
                val squash = tidemarkSquash(angle)
                assertTrue(squash in (1f - TidemarkScene.SQUASH - 0.001f)..1.001f)
            }
        }
    }

    @Test
    fun `a shape rises and flattens on the crest`() {
        val crest = tidemarkWaveAngle(0f, TidemarkScene.BandLength[0] / 2f / rideCos(), 0f)
        assertEquals(-TidemarkScene.BOB, tidemarkLift(crest), 0.001f)
        assertEquals(1f - TidemarkScene.SQUASH, tidemarkSquash(crest), 0.001f)
        val trough = tidemarkWaveAngle(0f, 0f, 0f)
        assertEquals(TidemarkScene.BOB, tidemarkLift(trough), 0.001f)
        assertEquals(1f, tidemarkSquash(trough), 0.001f)
    }

    @Test
    fun `the swell flows down from the top screen into the bottom`() {
        val y = 900f
        val later = tidemarkWaveAngle(0f, y, 10f)
        val earlier = tidemarkWaveAngle(0f, y - 10f / rideCos(), 0f)
        assertEquals(earlier, later, 0.01f)
    }

    @Test
    fun `the picker lists it with a readable name`() {
        val entry = JarnLiveWallpapers.fromId("tidemark")
        assertNotNull(entry)
        val label = ApplicationProvider.getApplicationContext<Application>().getString(entry!!.nameRes)
        assertEquals("Tidemark", label)
    }
}
