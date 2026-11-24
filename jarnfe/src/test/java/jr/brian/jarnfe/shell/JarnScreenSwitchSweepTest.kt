package jr.brian.jarnfe.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnScreenSwitchSweepTest {
    @Test
    fun `leaving the top screen exits through the hinge`() =
        assertTrue(jarnSweepCenter(1f, leaving = true, isTopWindow = true) > 1f)

    @Test
    fun `leaving the bottom screen exits through the hinge`() =
        assertTrue(jarnSweepCenter(1f, leaving = true, isTopWindow = false) < 0f)

    @Test
    fun `arriving on the bottom screen enters from the hinge`() {
        assertTrue(jarnSweepCenter(0f, leaving = false, isTopWindow = false) < 0f)
        assertTrue(jarnSweepCenter(1f, leaving = false, isTopWindow = false) > 1f)
    }

    @Test
    fun `arriving on the top screen enters from the hinge`() {
        assertTrue(jarnSweepCenter(0f, leaving = false, isTopWindow = true) > 1f)
        assertTrue(jarnSweepCenter(1f, leaving = false, isTopWindow = true) < 0f)
    }

    @Test
    fun `the band is invisible when it finishes`() {
        assertEquals(0f, jarnSweepAlpha(1f, leaving = true), 0.001f)
        assertEquals(0f, jarnSweepAlpha(1f, leaving = false), 0.001f)
    }
}
