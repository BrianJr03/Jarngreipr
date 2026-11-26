package jr.brian.jarnfe.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `a preview with sound holds music but may still play itself`() {
        val claimed = JarnAudioGate().withTop(1, true).copy(previewGameId = "a")
        assertFalse(claimed.allowed)
        assertTrue(claimed.previewMayPlay)
    }

    @Test
    fun `other media, an RSS video or a covered screen keep a preview quiet`() {
        val claimed = JarnAudioGate().withTop(1, true).copy(previewGameId = "a")
        assertFalse(claimed.copy(otherMediaPlaying = true).previewMayPlay)
        assertFalse(claimed.copy(videoOpen = true).previewMayPlay)
        assertFalse(claimed.withCovered(1, true).previewMayPlay)
        assertFalse(claimed.withTop(1, false).previewMayPlay)
    }

    @Test
    fun `a game whose preview has sound never plays its jingle`() {
        val trigger = JarnJingleTrigger()
        assertEquals(JarnJingleStep.None, trigger.next("a", false, previewGameId = "a"))
        assertEquals(JarnJingleStep.None, trigger.next("a", true))
    }

    @Test
    fun `a preview claiming its game stops a jingle already playing`() {
        val trigger = JarnJingleTrigger()
        trigger.next("a", true)
        assertEquals(JarnJingleStep.Stop, trigger.next("a", false, previewGameId = "a"))
    }

    @Test
    fun `the next game plays its jingle once the last preview lets go`() {
        val trigger = JarnJingleTrigger()
        trigger.next("a", false, previewGameId = "a")
        assertEquals(JarnJingleStep.None, trigger.next("b", false, previewGameId = "a"))
        assertEquals(JarnJingleStep.Play("b"), trigger.next("b", true))
    }

    @Test
    fun `only the claiming game can release a preview claim`() {
        JarnAudio.resetForTest(JarnAudioGate(topWindows = setOf(1)))
        try {
            JarnAudio.claimPreview("a")
            JarnAudio.claimPreview("b")
            JarnAudio.releasePreview("a")
            assertEquals("b", JarnAudio.gateState.value.previewGameId)
            JarnAudio.releasePreview("b")
            assertNull(JarnAudio.gateState.value.previewGameId)
            assertTrue(JarnAudio.gateState.value.allowed)
        } finally {
            JarnAudio.resetForTest()
        }
    }
}
