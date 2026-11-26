package jr.brian.jarnfe.transition

import jr.brian.core.ui.theme.JarnMotion
import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnMarkHandoffTimelineTest {
    private val full = JarnMotion()
    private val reduced = JarnMotion(reduced = true)
    private val instant = JarnMotion(reduced = true, instant = true)

    @Test
    fun `the label leaves in 160ms and arrives in 240ms starting 100ms in`() {
        val leave = JarnMarkHandoffTimeline.leg(from = 1f, arriving = false, motion = full)
        val arrive = JarnMarkHandoffTimeline.leg(from = 0f, arriving = true, motion = full)

        assertEquals(0, leave.delayMillis)
        assertEquals(160, leave.totalMillis)
        assertEquals(100, arrive.delayMillis)
        assertEquals(340, arrive.totalMillis)
        assertEquals(0f, leave.presenceAt(160f), 0f)
        assertEquals(0f, arrive.presenceAt(100f), 0f)
        assertEquals(1f, arrive.presenceAt(340f), 0f)
    }

    @Test
    fun `both labels show together only during the overlap`() {
        val leave = JarnMarkHandoffTimeline.leg(from = 1f, arriving = false, motion = full)
        val arrive = JarnMarkHandoffTimeline.leg(from = 0f, arriving = true, motion = full)

        assertTrue(leave.presenceAt(120f) > 0f && arrive.presenceAt(120f) > 0f)
        assertEquals(0f, arrive.presenceAt(50f), 0f)
        assertEquals(0f, leave.presenceAt(200f), 0f)
    }

    @Test
    fun `an interrupted handoff turns around from where it is`() {
        val leave = JarnMarkHandoffTimeline.leg(from = 1f, arriving = false, motion = full)
        val midway = leave.presenceAt(80f)
        val back = JarnMarkHandoffTimeline.leg(from = midway, arriving = true, motion = full)

        assertEquals(midway, back.presenceAt(0f), 0f)
        assertTrue(back.totalMillis < 340)
        assertEquals(1f, back.presenceAt(back.totalMillis.toFloat()), 0f)
    }

    @Test
    fun `a label already at rest has nothing to play`() {
        assertEquals(0, JarnMarkHandoffTimeline.leg(from = 1f, arriving = true, motion = full).totalMillis)
        assertEquals(0, JarnMarkHandoffTimeline.leg(from = 0f, arriving = false, motion = full).totalMillis)
    }

    @Test
    fun `reduced motion crossfades in place and instant swaps at once`() {
        val leave = JarnMarkHandoffTimeline.leg(from = 1f, arriving = false, motion = reduced)
        val arrive = JarnMarkHandoffTimeline.leg(from = 0f, arriving = true, motion = reduced)

        assertEquals(120, leave.totalMillis)
        assertEquals(0, arrive.delayMillis)
        assertEquals(120, arrive.totalMillis)
        assertEquals(0f, reduced.markTravel.value, 0f)
        assertEquals(0, JarnMarkHandoffTimeline.leg(from = 0f, arriving = true, motion = instant).totalMillis)
        assertEquals(1f, JarnMarkHandoffTimeline.leg(from = 0f, arriving = true, motion = instant).presenceAt(0f), 0f)
    }

    @Test
    fun `the row closes with the leaving label and opens to land with the arriving one`() {
        val label = JarnMarkHandoffTimeline.leg(from = 0f, arriving = true, motion = full)
        val closing = JarnMarkHandoffTimeline.slotLeg(from = 1f, arriving = false, motion = full)
        val opening = JarnMarkHandoffTimeline.slotLeg(from = 0f, arriving = true, motion = full)

        assertEquals(160, closing.totalMillis)
        assertEquals(0f, closing.presenceAt(160f), 0f)
        assertEquals(0, opening.delayMillis)
        assertEquals(label.totalMillis, opening.totalMillis)
        assertTrue(opening.presenceAt(100f) > label.presenceAt(100f))
        assertEquals(1f, opening.presenceAt(340f), 0f)
    }

    @Test
    fun `an interrupted row slides back from where it is`() {
        val closing = JarnMarkHandoffTimeline.slotLeg(from = 1f, arriving = false, motion = full)
        val midway = closing.presenceAt(80f)
        val back = JarnMarkHandoffTimeline.slotLeg(from = midway, arriving = true, motion = full)

        assertEquals(midway, back.presenceAt(0f), 0f)
        assertTrue(back.totalMillis < 340)
        assertEquals(1f, back.presenceAt(back.totalMillis.toFloat()), 0f)
    }

    @Test
    fun `reduced motion snaps the row once the label is gone or before it fades in`() {
        val closing = JarnMarkHandoffTimeline.slotLeg(from = 1f, arriving = false, motion = reduced)
        val opening = JarnMarkHandoffTimeline.slotLeg(from = 0f, arriving = true, motion = reduced)

        assertEquals(1f, closing.presenceAt(119f), 0f)
        assertEquals(0f, closing.presenceAt(120f), 0f)
        assertEquals(1f, opening.presenceAt(0f), 0f)
        assertEquals(0, JarnMarkHandoffTimeline.slotLeg(from = 1f, arriving = false, motion = instant).totalMillis)
    }

    @Test
    fun `the label travels toward the screen it is heading for`() {
        assertEquals(1f, jarnMarkTowardOtherScreen(JarnScreen.Stage, rolesSwapped = false), 0f)
        assertEquals(-1f, jarnMarkTowardOtherScreen(JarnScreen.Deck, rolesSwapped = false), 0f)
        assertEquals(-1f, jarnMarkTowardOtherScreen(JarnScreen.Stage, rolesSwapped = true), 0f)
        assertEquals(1f, jarnMarkTowardOtherScreen(JarnScreen.Deck, rolesSwapped = true), 0f)
        assertEquals(0f, JarnMarkHandoffTimeline.travel(presence = 1f, towardOtherScreen = -1f), 0f)
        assertEquals(-1f, JarnMarkHandoffTimeline.travel(presence = 0f, towardOtherScreen = -1f), 0f)
    }

    @Test
    fun `with two screens the label follows control, never during the splash`() {
        assertEquals(JarnScreen.Deck, jarnMarkHolder(twoScreens = true, splashIntro = false, JarnScreen.Deck))
        assertEquals(JarnScreen.Stage, jarnMarkHolder(twoScreens = true, splashIntro = false, JarnScreen.Stage))
        assertEquals(JarnScreen.Stage, jarnMarkHolder(twoScreens = true, splashIntro = true, JarnScreen.Deck))
    }

    @Test
    fun `with one screen the label stays on Stage`() {
        assertEquals(JarnScreen.Stage, jarnMarkHolder(twoScreens = false, splashIntro = false, JarnScreen.Deck))
    }
}
