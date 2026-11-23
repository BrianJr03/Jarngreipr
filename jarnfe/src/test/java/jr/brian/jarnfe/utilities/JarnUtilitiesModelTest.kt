package jr.brian.jarnfe.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnUtilitiesModelTest {

    @Test
    fun `cpu line parses total and idle`() {
        val ticks = jarnParseCpuTicks("cpu  10 20 30 40 5 6 7 8 0 0")
        assertEquals(JarnCpuTicks(total = 126, idle = 40), ticks)
    }

    @Test
    fun `malformed cpu lines are rejected`() {
        assertNull(jarnParseCpuTicks(null))
        assertNull(jarnParseCpuTicks("intr 1 2 3 4 5 6 7 8"))
        assertNull(jarnParseCpuTicks("cpu 1 2 3"))
        assertNull(jarnParseCpuTicks("cpu a b c d e f g h"))
    }

    @Test
    fun `a blocked read is unavailable rather than zero percent`() {
        assertEquals(JarnCpuReading.Unavailable, jarnCpuReading(JarnCpuTicks(10, 5), null))
    }

    @Test
    fun `the first sample is still measuring`() {
        assertEquals(JarnCpuReading.Measuring, jarnCpuReading(null, JarnCpuTicks(10, 5)))
    }

    @Test
    fun `cpu percent comes from the delta`() {
        val reading = jarnCpuReading(JarnCpuTicks(100, 50), JarnCpuTicks(200, 75))
        assertEquals(JarnCpuReading.Percent(75f), reading)
    }

    @Test
    fun `discharge estimate needs a negative current`() {
        assertNull(jarnDischargeRemainingMs(1_000_000, 0))
        assertNull(jarnDischargeRemainingMs(1_000_000, 500_000))
        assertNull(jarnDischargeRemainingMs(0, -500_000))
        assertEquals(2 * 3_600_000L, jarnDischargeRemainingMs(1_000_000, -500_000))
    }

    @Test
    fun `an unknown charge estimate is dropped`() {
        assertNull(jarnChargeRemainingMs(-1))
        assertNull(jarnChargeRemainingMs(0))
        assertEquals(120_000L, jarnChargeRemainingMs(120_000))
    }

    @Test
    fun `duration splits hours and minutes`() {
        assertEquals(JarnDuration(2, 30), jarnDuration(2 * 3_600_000L + 30 * 60_000L))
    }

    @Test
    fun `volume level rounds and stays in range`() {
        assertEquals(7, jarnVolumeLevel(6.6f, 15))
        assertEquals(15, jarnVolumeLevel(99f, 15))
        assertEquals(0, jarnVolumeLevel(-1f, 15))
        assertEquals(1, jarnVolumeSteps(0))
    }

    @Test
    fun `usage fraction handles an empty total`() {
        assertEquals(0f, JarnUsage(5, 0).fraction)
        assertEquals(0.5f, JarnUsage(5, 10).fraction)
    }

    @Test
    fun `an empty floaty tab set means every tab`() {
        assertTrue(JarnFloatySettings().isTabEnabled(3))
        val some = JarnFloatySettings(enabledTabs = setOf(1))
        assertTrue(some.isTabEnabled(1))
        assertFalse(some.isTabEnabled(0))
    }
}
