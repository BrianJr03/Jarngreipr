package jr.brian.jarnfe.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class JarnKeyboardModelTest {

    @Test
    fun `text starts empty by default`() {
        assertEquals("", JarnKeyboardState().text)
    }

    @Test
    fun `typing on prefilled text adds at the end`() {
        val typed = JarnKeyboardState("Emulators")
            .press(JarnKeyboardKey.Space)
            .press(JarnKeyboardKey.Letter('2'))
        assertEquals("Emulators 2", typed.text)
    }

    @Test
    fun `delete on prefilled text removes the last character`() {
        assertEquals("Emulator", JarnKeyboardState("Emulators").press(JarnKeyboardKey.Delete).text)
    }

    @Test
    fun `changing page keeps prefilled text`() {
        val state = JarnKeyboardState("Emulators")
            .press(JarnKeyboardKey.Shift)
            .press(JarnKeyboardKey.Symbols)
        assertEquals("Emulators", state.text)
    }
}
