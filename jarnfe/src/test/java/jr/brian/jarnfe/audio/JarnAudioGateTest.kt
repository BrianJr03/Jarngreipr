package jr.brian.jarnfe.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnAudioGateTest {
    @Test
    fun `either window on top allows audio`() {
        val stage = JarnAudioGate().withTop(1, true)
        assertTrue(stage.allowed)
        assertTrue(stage.withTop(2, true).withTop(1, false).allowed)
        assertFalse(stage.withTop(1, false).allowed)
    }

    @Test
    fun `tapping the free window does not play over a game on the other screen`() {
        val gameOnStage = JarnAudioGate().withCovered(1, true).withTop(2, true)
        assertFalse(gameOnStage.allowed)
        assertTrue(gameOnStage.withCovered(1, false).allowed)
    }

    @Test
    fun `video and other media block audio`() {
        val top = JarnAudioGate().withTop(1, true)
        assertFalse(top.copy(videoOpen = true).allowed)
        assertFalse(top.copy(otherMediaPlaying = true).allowed)
    }

    @Test
    fun `a selection plays once however often it is reported`() {
        val trigger = JarnJingleTrigger()
        assertEquals(JarnJingleStep.Play("a"), trigger.next("a", true))
        assertEquals(JarnJingleStep.None, trigger.next("a", true))
    }

    @Test
    fun `returning from a game does not replay the jingle`() {
        val trigger = JarnJingleTrigger()
        trigger.next("a", true)
        assertEquals(JarnJingleStep.Stop, trigger.next("a", false))
        assertEquals(JarnJingleStep.None, trigger.next("a", false))
        assertEquals(JarnJingleStep.None, trigger.next("a", true))
    }

    @Test
    fun `moving away and back plays again`() {
        val trigger = JarnJingleTrigger()
        trigger.next("a", true)
        assertEquals(JarnJingleStep.Stop, trigger.next(null, true))
        assertEquals(JarnJingleStep.Play("a"), trigger.next("a", true))
    }

    @Test
    fun `a new game plays straight after the last one`() {
        val trigger = JarnJingleTrigger()
        trigger.next("a", true)
        assertEquals(JarnJingleStep.Play("b"), trigger.next("b", true))
    }

    @Test
    fun `nothing plays while audio is held`() {
        val trigger = JarnJingleTrigger()
        assertEquals(JarnJingleStep.None, trigger.next("a", false))
        assertEquals(JarnJingleStep.Play("a"), trigger.next("a", true))
    }
}
