package jr.brian.jarnfe.launch

import jr.brian.frontend.library.art.ArtChoice
import jr.brian.frontend.library.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameActionsPageTest {
    @Test
    fun `choosing RetroArch continues to its core`() {
        assertEquals(GameActionsPage.Cores, pageAfterEmulatorChoice("com.retroarch.aarch64"))
    }

    @Test
    fun `choosing another emulator asks where it applies without launching`() {
        assertEquals(GameActionsPage.EmulatorScope, pageAfterEmulatorChoice("org.ppsspp.ppsspp"))
    }

    @Test
    fun `back leaves one layer at a time`() {
        assertEquals(GameActionsPage.Cores, pageBefore(GameActionsPage.EmulatorScope, true, true))
        assertEquals(GameActionsPage.Emulators, pageBefore(GameActionsPage.EmulatorScope, true, false))
        assertEquals(GameActionsPage.Emulators, pageBefore(GameActionsPage.Cores, true, false))
        assertEquals(GameActionsPage.Actions, pageBefore(GameActionsPage.Cores, false, false))
        assertEquals(GameActionsPage.EmulatorScope, pageBefore(GameActionsPage.EmulatorConfirm, true, false))
        assertEquals(GameActionsPage.ArtScope, pageBefore(GameActionsPage.ArtConfirm, false, false))
        assertEquals(GameActionsPage.EditArt, pageBefore(GameActionsPage.ArtScope, false, false))
        assertEquals(GameActionsPage.Edit, pageBefore(GameActionsPage.EditArt, false, false))
        assertEquals(GameActionsPage.Actions, pageBefore(GameActionsPage.Edit, false, false))
    }

    @Test
    fun `art is only available for this game when it has the file`() {
        val kinds = setOf(MediaKind.Cover)
        assertTrue(isArtAvailable(ArtChoice.Cover, kinds))
        assertTrue(isArtAvailable(ArtChoice.Default, emptySet()))
        assertFalse(isArtAvailable(ArtChoice.Marquee, kinds))
    }

    @Test
    fun `each missing choice opens its picker`() {
        assertEquals(GameActionsPage.Emulators, pageFor(JarnEmulatorChoiceKind.Emulator))
        assertEquals(GameActionsPage.Cores, pageFor(JarnEmulatorChoiceKind.RetroArchCore))
    }
}
