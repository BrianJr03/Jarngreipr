package jr.brian.jarnfe.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLaunchFocusTest {

    @Test
    fun `content leaves a user on the tab row they chose`() {
        assertFalse(libraryContentTakesFocus(forcing = false, tabsHaveFocus = true, launchEntry = false))
    }

    @Test
    fun `content takes focus from the tab row on launch`() {
        assertTrue(libraryContentTakesFocus(forcing = false, tabsHaveFocus = true, launchEntry = true))
    }

    @Test
    fun `content takes focus after a tab switch or when the tab row has none`() {
        assertTrue(libraryContentTakesFocus(forcing = true, tabsHaveFocus = true, launchEntry = false))
        assertTrue(libraryContentTakesFocus(forcing = false, tabsHaveFocus = false, launchEntry = false))
    }

    @Test
    fun `the launch entry stays open until focus lands, then ends`() {
        val before = LibraryLaunchFocus.isPending
        assertFalse(LibraryLaunchFocus.settle { false })
        assertEquals(before, LibraryLaunchFocus.isPending)

        assertTrue(LibraryLaunchFocus.settle { true })
        assertFalse(LibraryLaunchFocus.isPending)
    }

    @Test
    fun `a press or tap ends the launch entry`() {
        LibraryLaunchFocus.release()
        assertFalse(LibraryLaunchFocus.isPending)
        assertFalse(libraryContentTakesFocus(forcing = false, tabsHaveFocus = true, LibraryLaunchFocus.isPending))
    }
}
