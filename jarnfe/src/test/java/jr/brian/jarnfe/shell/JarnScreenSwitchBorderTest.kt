package jr.brian.jarnfe.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class JarnScreenSwitchBorderTest {
    @Test
    fun `the ring starts and ends invisible`() {
        assertEquals(0f, jarnSwitchRingAlpha(0f), 0.001f)
        assertEquals(0f, jarnSwitchRingAlpha(1f), 0.001f)
    }

    @Test
    fun `the ring fades in and out around a steady middle`() {
        assertEquals(0.5f, jarnSwitchRingAlpha(0.1f), 0.001f)
        assertEquals(1f, jarnSwitchRingAlpha(0.5f), 0.001f)
        assertEquals(0.5f, jarnSwitchRingAlpha(0.9f), 0.001f)
    }

    @Test
    fun `the ring makes one and a half turns`() = assertEquals(540f, jarnSwitchRingRotation(1f), 0.001f)
}
