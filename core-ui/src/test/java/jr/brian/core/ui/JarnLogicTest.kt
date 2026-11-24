package jr.brian.core.ui

import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnStepState
import jr.brian.core.ui.components.jarnShoulderTabIndex
import jr.brian.core.ui.components.jarnSliderStep
import jr.brian.core.ui.components.jarnSliderValueAt
import jr.brian.core.ui.components.jarnStepState
import jr.brian.core.ui.focus.jarnPressScale
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.jarnShouldReduceMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnLogicTest {

    @Test
    fun `press shrinks an item in place`() {
        assertEquals(0.97f, jarnPressScale(0.97f, 1f), 0.0001f)
        assertEquals(1f, jarnPressScale(0.97f, 0f), 0f)
    }

    @Test
    fun `an item never grows past its bounds`() {
        assertEquals(1f, jarnPressScale(1.06f, 1f), 0f)
    }

    @Test
    fun `motion tokens match the spec`() {
        val motion = JarnMotion()
        assertEquals(90, motion.focusIn.durationMillis)
        assertEquals(70, motion.focusOut.durationMillis)
        assertEquals(80, motion.press.durationMillis)
        assertEquals(160, motion.small.durationMillis)
        assertEquals(240, motion.panel.durationMillis)
        assertEquals(320, motion.screen.durationMillis)
        assertEquals(400, motion.art.durationMillis)
    }

    @Test
    fun `reduced motion removes movement and uses short fades`() {
        val motion = JarnMotion(reduced = true)
        assertEquals(0, motion.focusIn.durationMillis)
        assertEquals(0, motion.focusOut.durationMillis)
        assertEquals(120, motion.panel.durationMillis)
        assertEquals(120, motion.screen.durationMillis)
        assertEquals(120, motion.art.durationMillis)
        assertEquals(1f, motion.pressScale, 0f)
        assertEquals(0.dp, motion.depthOffset)
        assertEquals(0, motion.deckFollowDelayMillis)
        assertFalse(motion.animateNowPlaying)
    }

    @Test
    fun `ambient motion follows only the in-app toggle`() {
        assertFalse(JarnMotion(reduced = true, ambientReduced = false).ambientReduced)
        assertTrue(JarnMotion(reduced = true).ambientReduced)
        assertFalse(JarnMotion().ambientReduced)
    }

    @Test
    fun `reduced motion follows the toggle or a zero animator scale`() {
        assertTrue(jarnShouldReduceMotion(true, 1f))
        assertTrue(jarnShouldReduceMotion(false, 0f))
        assertFalse(jarnShouldReduceMotion(false, 0.5f))
    }

    @Test
    fun `fjord is the default and unknown ids fall back to it`() {
        assertSame(JarnLooks.Fjord, JarnLooks.Default)
        assertSame(JarnLooks.Fjord, JarnLooks.fromId(null))
        assertSame(JarnLooks.Fjord, JarnLooks.fromId("missing"))
        assertSame(JarnLooks.Night, JarnLooks.fromId("night"))
        assertEquals(4, JarnLooks.All.map { it.id }.toSet().size)
    }

    @Test
    fun `slider steps and clamps`() {
        assertEquals(0.55f, jarnSliderStep(0.5f, 0f..1f, 20, 1), 0.0001f)
        assertEquals(1f, jarnSliderStep(1f, 0f..1f, 20, 1), 0f)
        assertEquals(0f, jarnSliderStep(0f, 0f..1f, 20, -1), 0f)
        assertEquals(30f, jarnSliderStep(20f, 10f..110f, 10, 1), 0.001f)
    }

    @Test
    fun `slider fraction snaps to steps`() {
        assertEquals(0.5f, jarnSliderValueAt(0.52f, 0f..1f, 10), 0.0001f)
        assertEquals(1f, jarnSliderValueAt(4f, 0f..1f, 10), 0f)
        assertEquals(0.37f, jarnSliderValueAt(0.37f, 0f..1f, 0), 0.0001f)
    }

    @Test
    fun `shoulder tabs move one at a time and stop at the ends`() {
        assertEquals(2, jarnShoulderTabIndex(1, 4, 1))
        assertEquals(0, jarnShoulderTabIndex(0, 4, -1))
        assertEquals(3, jarnShoulderTabIndex(3, 4, 1))
    }

    @Test
    fun `stepper states`() {
        assertEquals(JarnStepState.Done, jarnStepState(0, 1))
        assertEquals(JarnStepState.Current, jarnStepState(1, 1))
        assertEquals(JarnStepState.Upcoming, jarnStepState(2, 1))
    }
}
