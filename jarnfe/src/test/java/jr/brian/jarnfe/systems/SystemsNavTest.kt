package jr.brian.jarnfe.systems

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemsNavTest {
    @Test
    fun backWalksFromAppPickerToExit() {
        val apps = SystemsNav(page = SystemsPage.Emulators("ps2", allApps = true))
        val emulators = apps.back()!!
        assertEquals(SystemsPage.Emulators("ps2"), emulators.page)
        val editor = emulators.back()!!
        assertEquals(SystemsPage.Editor("ps2", SystemSection.Emulator), editor.page)
        val list = editor.back()!!
        assertEquals(SystemsPage.List, list.page)
        assertEquals("ps2", list.focusedSystemId)
        assertNull(list.back())
    }

    @Test
    fun sectionsStopAtTheEnds() {
        val first = SystemsNav.opening("snes")
        assertEquals(first, first.stepSection(-1))
        val last = SystemsNav(page = SystemsPage.Editor("snes", SystemSection.Actions))
        assertEquals(last, last.stepSection(1))
    }

    @Test
    fun filterCyclesAndMatches() {
        val hidden = JarnManagedSystem("a", "A", 1, isHidden = true)
        assertEquals(SystemFilter.Shown, SystemFilter.All.next())
        assertEquals(SystemFilter.All, SystemFilter.Hidden.next())
        assertTrue(SystemFilter.Hidden.matches(hidden))
        assertFalse(SystemFilter.Shown.matches(hidden))
    }

    @Test
    fun storageKeysKeepIdFirstWithoutDuplicates() {
        assertEquals(
            listOf("genesis", "megadrive"),
            systemStorageKeys("genesis", listOf("genesis", "megadrive", " ")),
        )
    }

    @Test
    fun colorRoundTripsStoredValues() {
        listOf(0xFFFF0000L, 0x8000FF00L, 0xFF123456L, 0x00FFFFFFL).forEach { argb ->
            assertEquals(argb, SystemColor.fromArgb(argb).toArgb())
        }
        assertEquals(SystemColor(alpha = 0f), SystemColor.fromArgb(0L))
    }
}
