package jr.brian.jarnfe.look

import jr.brian.core.ui.theme.JarnLooks
import jr.brian.jarnfe.settings.nextFocusedLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class JarnAppearanceTest {
    @Test
    fun `saved look applies without a preview`() {
        assertSame(JarnLooks.Grove, resolveJarnLook("grove", null))
    }

    @Test
    fun `preview wins over the saved look`() {
        assertSame(JarnLooks.Night, resolveJarnLook("grove", "night"))
    }

    @Test
    fun `missing and unknown looks fall back to fjord`() {
        assertSame(JarnLooks.Fjord, resolveJarnLook(null, null))
        assertSame(JarnLooks.Fjord, resolveJarnLook("future", null))
    }

    @Test
    fun `moving between cards keeps the newly focused look in either event order`() {
        assertEquals("night", nextFocusedLook(nextFocusedLook("forge", "forge", false), "night", true))
        assertEquals("night", nextFocusedLook(nextFocusedLook("forge", "night", true), "forge", false))
    }

    @Test
    fun `leaving the picker clears the preview`() {
        assertNull(nextFocusedLook("forge", "forge", false))
    }
}
