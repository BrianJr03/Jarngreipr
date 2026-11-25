package jr.brian.jarnfe.shell

import jr.brian.core.ui.theme.JarnSwitchCue
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnScreenDimTest {
    @Test
    fun `only an inactive screen with a second display dims`() {
        assertEquals(1f, jarnDimTarget(JarnSwitchCue.Dim, isLit = false, twoScreens = true))
        assertEquals(0f, jarnDimTarget(JarnSwitchCue.Dim, isLit = true, twoScreens = true))
        assertEquals(0f, jarnDimTarget(JarnSwitchCue.Dim, isLit = false, twoScreens = false))
    }

    @Test
    fun `other cues never dim`() {
        assertEquals(0f, jarnDimTarget(JarnSwitchCue.Pulse, isLit = false, twoScreens = true))
        assertEquals(0f, jarnDimTarget(JarnSwitchCue.TabMark, isLit = false, twoScreens = true))
    }

    @Test
    fun `a tab mark only shows with two screens`() {
        assertEquals(JarnSwitchCue.TabMark, jarnShownCue(JarnSwitchCue.TabMark, twoScreens = true))
        assertEquals(JarnSwitchCue.Pulse, jarnShownCue(JarnSwitchCue.TabMark, twoScreens = false))
        assertEquals(JarnSwitchCue.Dim, jarnShownCue(JarnSwitchCue.Dim, twoScreens = false))
    }

    @Test
    fun `the jarn label only moves with two screens`() {
        assertEquals(JarnSwitchCue.JarnLabel, jarnShownCue(JarnSwitchCue.JarnLabel, twoScreens = true))
        assertEquals(JarnSwitchCue.Pulse, jarnShownCue(JarnSwitchCue.JarnLabel, twoScreens = false))
        assertEquals(0f, jarnDimTarget(JarnSwitchCue.JarnLabel, isLit = false, twoScreens = true))
    }

    @Test
    fun `the first change is immediate and the next waits out the gap`() {
        val gate = JarnDimGate(shown = 0f)
        assertEquals(0L, gate.waitMillis(1f, 1_000L))
        gate.commit(1f, 1_000L)
        assertEquals(120L, gate.waitMillis(0f, 1_050L))
        assertEquals(0L, gate.waitMillis(0f, 1_170L))
    }

    @Test
    fun `returning to the shown direction never waits`() {
        val gate = JarnDimGate(shown = 0f)
        gate.commit(1f, 1_000L)
        assertEquals(0L, gate.waitMillis(1f, 1_010L))
    }
}
