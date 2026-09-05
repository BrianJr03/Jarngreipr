package jr.brian.home.esde.util

import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName
import org.junit.Assert.assertEquals
import org.junit.Test

class ESDEMediaConstantsTest {

    @Test
    fun `recent maps to auto-lastplayed asset name`() {
        assertEquals("auto-lastplayed", getMediaSystemName("recent"))
    }

    @Test
    fun `favorites maps to auto-favorites asset name`() {
        assertEquals("auto-favorites", getMediaSystemName("favorites"))
    }

    @Test
    fun `all maps to auto-allgames asset name`() {
        assertEquals("auto-allgames", getMediaSystemName("all"))
    }

    @Test
    fun `existing hardware variant alias still resolves to parent system`() {
        assertEquals("nes", getMediaSystemName("famicom"))
    }

    @Test
    fun `unmapped system name is returned unchanged`() {
        assertEquals("snes", getMediaSystemName("snes"))
    }
}
