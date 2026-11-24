package jr.brian.jarnfe.apps

import jr.brian.jarnfe.canvas.JarnAppDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnAppOptionsTest {
    private fun options(ask: Boolean, second: Boolean) =
        JarnAppOptions(JarnAppDisplay.Current, ask, null, second)

    @Test
    fun `asks only with the setting on and a second display`() {
        assertTrue(options(ask = true, second = true).shouldAskDisplay())
        assertFalse(options(ask = true, second = false).shouldAskDisplay())
        assertFalse(options(ask = false, second = true).shouldAskDisplay())
    }

    @Test
    fun `icon search ignores case and separators`() {
        val names = listOf("google_chrome", "ic-maps", "Retro Arch")
        assertEquals(listOf("google_chrome"), jarnFilterIconNames(names, "GoogleChrome"))
        assertEquals(listOf("ic-maps"), jarnFilterIconNames(names, "ic maps"))
        assertEquals(listOf("Retro Arch"), jarnFilterIconNames(names, "retro_arch"))
    }

    @Test
    fun `a blank icon search keeps every icon`() {
        val names = listOf("a", "b")
        assertEquals(names, jarnFilterIconNames(names, "  "))
    }
}
