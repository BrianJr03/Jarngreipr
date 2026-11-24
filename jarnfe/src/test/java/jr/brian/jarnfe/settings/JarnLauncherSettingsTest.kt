package jr.brian.jarnfe.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnLauncherSettingsTest {
    @Test
    fun `next dock slot reuses a gap before growing`() {
        assertEquals(1, jarnNextDockSlot(listOf("a", "", "c"), 5))
        assertEquals(2, jarnNextDockSlot(listOf("a", "b"), 5))
        assertEquals(0, jarnNextDockSlot(emptyList(), 5))
    }

    @Test
    fun `a full dock has no slot, and gaps past the limit do not count`() {
        assertNull(jarnNextDockSlot(listOf("a", "b"), 2))
        assertNull(jarnNextDockSlot(listOf("a", "b", ""), 2))
    }

    @Test
    fun `the default label size keeps the design size and others are clamped`() {
        assertEquals(1f, jarnAppLabelScale(JARN_APP_LABEL_DEFAULT_SIZE), 0f)
        assertEquals(2f, jarnAppLabelScale(99), 0f)
        assertEquals(JARN_APP_LABEL_MIN_SIZE / 12f, jarnAppLabelScale(1), 0f)
    }

    @Test
    fun `patch notes lose markdown but keep their lines`() {
        val notes = "## Title\n**Dock**\n- Added a toggle\n\n3.1.3\n"
        assertEquals("Title\nDock\n• Added a toggle\n\n3.1.3", jarnPlainPatchNotes(notes))
    }
}
