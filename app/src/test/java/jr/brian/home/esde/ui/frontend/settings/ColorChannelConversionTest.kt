package jr.brian.home.esde.ui.frontend.settings

import jr.brian.home.esde.model.SystemCustomization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorChannelConversionTest {

    @Test
    fun nullArgb_isDecodedAsDefaultWhite() {
        val channels = argbToChannels(null)
        assertEquals(ColorChannels.DEFAULT_WHITE, channels)
    }

    @Test
    fun transparentArgb_decodesToZeroAlpha() {
        val channels = argbToChannels(SystemCustomization.TRANSPARENT_ARGB)
        assertEquals(0f, channels.alpha, 0f)
    }

    @Test
    fun opaqueRed_roundTripsExactly() {
        val original = 0xFFFF0000L
        val channels = argbToChannels(original)
        val encoded = channelsToArgb(channels)
        assertEquals(original, encoded)
    }

    @Test
    fun opaqueGreen_roundTripsExactly() {
        val original = 0xFF00FF00L
        val channels = argbToChannels(original)
        val encoded = channelsToArgb(channels)
        assertEquals(original, encoded)
    }

    @Test
    fun opaqueBlue_roundTripsExactly() {
        val original = 0xFF0000FFL
        val channels = argbToChannels(original)
        val encoded = channelsToArgb(channels)
        assertEquals(original, encoded)
    }

    @Test
    fun opaqueWhite_roundTripsExactly() {
        val original = 0xFFFFFFFFL
        val channels = argbToChannels(original)
        val encoded = channelsToArgb(channels)
        assertEquals(original, encoded)
    }

    @Test
    fun opaqueBlack_roundTripsExactly() {
        val original = 0xFF000000L
        val channels = argbToChannels(original)
        val encoded = channelsToArgb(channels)
        assertEquals(original, encoded)
    }

    @Test
    fun midGrayWithHalfAlpha_roundTripsWithinTolerance() {
        val original = 0x80808080L
        val channels = argbToChannels(original)
        val encoded = channelsToArgb(channels)
        assertChannelsWithin(original, encoded, tolerance = 1)
    }

    @Test
    fun arbitraryColors_roundTripWithinTolerance() {
        val samples = listOf(
            0xFF123456L,
            0xC0AABBCCL,
            0x40112233L,
            0xFFDEAD99L,
            0xFF00BFFFL
        )
        samples.forEach { original ->
            val channels = argbToChannels(original)
            val encoded = channelsToArgb(channels)
            assertChannelsWithin(original, encoded, tolerance = 1)
        }
    }

    @Test
    fun channelsToArgb_producesValidRange() {
        val channels = ColorChannels(hueDegrees = 180f, saturation = 0.5f, brightness = 0.5f, alpha = 0.5f)
        val encoded = channelsToArgb(channels)
        assertTrue(encoded in 0L..0xFFFFFFFFL)
    }

    @Test
    fun hue_at360_wrapsToZeroForRedChannel() {
        val at360 = channelsToArgb(ColorChannels(360f, 1f, 1f, 1f))
        val at0 = channelsToArgb(ColorChannels(0f, 1f, 1f, 1f))
        assertEquals(at0, at360)
    }

    @Test
    fun transparentAndNull_areDistinguishableStates() {
        assertNotEquals(argbToChannels(null).alpha, argbToChannels(SystemCustomization.TRANSPARENT_ARGB).alpha)
    }

    private fun assertChannelsWithin(expected: Long, actual: Long, tolerance: Int) {
        val ea = ((expected ushr 24) and 0xFFL).toInt()
        val er = ((expected ushr 16) and 0xFFL).toInt()
        val eg = ((expected ushr 8) and 0xFFL).toInt()
        val eb = (expected and 0xFFL).toInt()
        val aa = ((actual ushr 24) and 0xFFL).toInt()
        val ar = ((actual ushr 16) and 0xFFL).toInt()
        val ag = ((actual ushr 8) and 0xFFL).toInt()
        val ab = (actual and 0xFFL).toInt()
        assertTrue("alpha diff too large: $ea vs $aa", abs(ea - aa) <= tolerance)
        assertTrue("red diff too large: $er vs $ar", abs(er - ar) <= tolerance)
        assertTrue("green diff too large: $eg vs $ag", abs(eg - ag) <= tolerance)
        assertTrue("blue diff too large: $eb vs $ab", abs(eb - ab) <= tolerance)
    }
}
