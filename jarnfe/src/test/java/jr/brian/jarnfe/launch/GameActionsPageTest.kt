package jr.brian.jarnfe.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class GameActionsPageTest {
    @Test
    fun `choosing RetroArch continues to its core`() {
        assertEquals(GameActionsPage.Cores, pageAfterEmulatorChoice("com.retroarch.aarch64"))
    }

    @Test
    fun `choosing another emulator returns to actions without launching`() {
        assertEquals(GameActionsPage.Actions, pageAfterEmulatorChoice("org.ppsspp.ppsspp"))
    }

    @Test
    fun `each missing choice opens its picker`() {
        assertEquals(GameActionsPage.Emulators, pageFor(JarnEmulatorChoiceKind.Emulator))
        assertEquals(GameActionsPage.Cores, pageFor(JarnEmulatorChoiceKind.RetroArchCore))
    }
}
