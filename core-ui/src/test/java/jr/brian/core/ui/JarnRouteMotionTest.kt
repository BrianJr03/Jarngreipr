package jr.brian.core.ui

import androidx.compose.animation.core.LinearEasing
import jr.brian.core.ui.theme.JarnEasingEmphasized
import jr.brian.core.ui.theme.JarnEasingExit
import jr.brian.core.ui.theme.JarnEasingStandard
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnRouteChange
import jr.brian.core.ui.theme.JarnRouteFollow
import jr.brian.core.ui.theme.JarnRouteLeg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JarnRouteMotionTest {
    private val full = JarnMotion()
    private val reduced = JarnMotion(reduced = true)
    private val instant = JarnMotion(reduced = true, instant = true)

    @Test
    fun `opening a system matches the approved depth push`() {
        val open = full.depthOpen
        assertEquals(JarnRouteLeg(0, 140, JarnEasingExit, toScale = 0.97f), open.exit)
        assertEquals(JarnRouteLeg(60, 220, JarnEasingEmphasized, fromScale = 1.03f), open.enter)
        assertEquals(280, open.totalMillis)
    }

    @Test
    fun `going back mirrors the push in 260ms`() {
        val back = full.depthBack
        assertEquals(JarnRouteLeg(0, 120, JarnEasingExit, toScale = 1.03f), back.exit)
        assertEquals(JarnRouteLeg(40, 220, JarnEasingEmphasized, fromScale = 0.97f), back.enter)
        assertEquals(260, back.totalMillis)
    }

    @Test
    fun `the next system is a 200ms short slide`() {
        val step = full.systemStep
        assertEquals(JarnRouteLeg(0, 80, JarnEasingExit), step.exit)
        assertEquals(JarnRouteLeg(80, 120, JarnEasingStandard), step.enter)
        assertEquals(0.014f, step.offsetFraction, 0f)
        assertEquals(200, step.totalMillis)
    }

    @Test
    fun `the Deck trails by 40ms and fades over 240ms`() {
        assertEquals(JarnRouteFollow(40, 100, 140), full.deckFollow)
        assertEquals(240, full.deckFollow.fadeMillis)
    }

    @Test
    fun `the header crossfades over 160ms after 40ms`() {
        assertEquals(JarnRouteLeg(40, 160, LinearEasing), full.headerSwap)
        assertEquals(JarnRouteLeg(0, 120, LinearEasing), reduced.headerSwap)
        assertEquals(0, instant.headerSwap.endMillis)
    }

    @Test
    fun `reduced motion turns every route change into a 120ms crossfade`() {
        val crossfade = JarnRouteLeg(0, 120, LinearEasing)
        assertEquals(JarnRouteChange(crossfade, crossfade), reduced.depthOpen)
        assertEquals(JarnRouteChange(crossfade, crossfade), reduced.depthBack)
        assertFalse(reduced.depthOpen.enter.scales || reduced.depthOpen.exit.scales)
        assertEquals(0f, reduced.systemStep.offsetFraction, 0f)
        assertEquals(120, reduced.systemStep.totalMillis)
        assertEquals(JarnRouteFollow(0, 60, 60), reduced.deckFollow)
    }

    @Test
    fun `Android animations off makes route changes instant`() {
        assertEquals(0, instant.depthOpen.totalMillis)
        assertEquals(0, instant.depthBack.totalMillis)
        assertEquals(0, instant.systemStep.totalMillis)
        assertEquals(JarnRouteFollow(0, 0, 0), instant.deckFollow)
    }
}
