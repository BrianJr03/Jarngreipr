package jr.brian.jarnfe.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class JarnScreenSwitchBorderTest {
    @Test
    fun `the border lasts 300 ms`() = assertEquals(300, JARN_SWITCH_BORDER_MILLIS)

    @Test
    fun `the border starts and ends invisible`() {
        assertEquals(0f, jarnSwitchBorderAlpha(0f), 0.001f)
        assertEquals(0f, jarnSwitchBorderAlpha(1f), 0.001f)
    }

    @Test
    fun `the border peaks halfway`() = assertEquals(1f, jarnSwitchBorderAlpha(0.5f), 0.001f)
}
