package jr.brian.jarnfe.wallpaper

import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnWallpaperGateTest {
    private val stage = JarnWallpaperWindow(JarnScreen.Stage, isResumed = true, isTopResumed = true)
    private val deck = JarnWallpaperWindow(JarnScreen.Deck, isResumed = true, isTopResumed = false)
    private val open = JarnWallpaperConditions(
        windows = listOf(stage, deck),
        deckDisplayAvailable = true,
        isScreenOn = true,
        isBatterySaverOn = false,
        isMotionReduced = false,
        isSplashPlaying = false,
    )

    @Test
    fun `opens when every condition holds`() = assertTrue(open.canAnimate())

    @Test
    fun `opens on a single display with only Stage`() {
        assertTrue(open.copy(windows = listOf(stage), deckDisplayAvailable = false).canAnimate())
    }

    @Test
    fun `closes when the game covers Stage and Deck stays resumed`() {
        val covered = stage.copy(isResumed = false, isTopResumed = false)
        val deckTop = deck.copy(isTopResumed = true)
        assertFalse(open.copy(windows = listOf(covered, deckTop)).canAnimate())
    }

    @Test
    fun `closes when the game covers Deck and Stage stays resumed`() {
        val covered = deck.copy(isResumed = false)
        assertFalse(open.copy(windows = listOf(stage, covered)).canAnimate())
    }

    @Test
    fun `closes when another app holds top resumed`() {
        val notTop = stage.copy(isTopResumed = false)
        assertFalse(open.copy(windows = listOf(notTop, deck)).canAnimate())
    }

    @Test
    fun `opens on a Deck display whose window is gone while Stage holds top resumed`() {
        assertTrue(open.copy(windows = listOf(stage)).canAnimate())
    }

    @Test
    fun `closes on a Deck display whose window is gone while a game runs`() {
        assertFalse(open.copy(windows = listOf(stage), isGameRunning = true).canAnimate())
    }

    @Test
    fun `closes on a Deck display whose window is gone when Stage lost top resumed`() {
        val notTop = stage.copy(isTopResumed = false)
        assertFalse(open.copy(windows = listOf(notTop)).canAnimate())
    }

    @Test
    fun `a running game alone does not close it while both windows are up`() {
        assertTrue(open.copy(isGameRunning = true).canAnimate())
    }

    @Test
    fun `reports Battery Saver before Reduce motion`() {
        assertEquals(
            JarnWallpaperPause.BatterySaver,
            open.copy(isBatterySaverOn = true, isMotionReduced = true).pause(),
        )
        assertEquals(JarnWallpaperPause.ReduceMotion, open.copy(isMotionReduced = true).pause())
        assertEquals(null, open.pause())
    }

    @Test
    fun `closes when the Stage window is gone`() {
        val deckTop = deck.copy(isTopResumed = true)
        assertFalse(open.copy(windows = listOf(deckTop)).canAnimate())
    }

    @Test
    fun `closes with no windows`() {
        assertFalse(open.copy(windows = emptyList(), deckDisplayAvailable = false).canAnimate())
    }

    @Test
    fun `closes with the screen off`() = assertFalse(open.copy(isScreenOn = false).canAnimate())

    @Test
    fun `closes on Battery Saver`() = assertFalse(open.copy(isBatterySaverOn = true).canAnimate())

    @Test
    fun `closes with reduced motion`() = assertFalse(open.copy(isMotionReduced = true).canAnimate())

    @Test
    fun `closes during the splash`() = assertFalse(open.copy(isSplashPlaying = true).canAnimate())

    @Test
    fun `time does not jump across a pause`() {
        val timeline = JarnWallpaperTimeline()
        timeline.rejoin(1_000L)
        timeline.advance(1_100L)
        timeline.rejoin(9_000_000_000L)
        timeline.advance(9_000_000_100L)
        assertEquals(200L, timeline.elapsedNanos)
    }

    @Test
    fun `both windows reporting one frame advance time once`() {
        val timeline = JarnWallpaperTimeline()
        timeline.rejoin(1_000L)
        timeline.advance(1_500L)
        timeline.advance(1_500L)
        timeline.rejoin(1_500L)
        assertEquals(500L, timeline.elapsedNanos)
    }

    @Test
    fun `windows waking on different frames keep time moving`() {
        val timeline = JarnWallpaperTimeline()
        timeline.rejoin(0L)
        var lastWake = 0L
        for (index in 1..600) {
            val frame = index * 16_666_667L
            val stageWakes = index % 5 == 0
            val deckWakes = index % 4 == 0
            if (stageWakes) timeline.advance(frame)
            if (deckWakes) timeline.advance(frame)
            if (stageWakes || deckWakes) lastWake = frame
        }
        assertEquals(lastWake, timeline.elapsedNanos)
    }

    @Test
    fun `a stalled frame cannot leap time forward`() {
        val timeline = JarnWallpaperTimeline()
        timeline.rejoin(1L)
        timeline.advance(60_000_000_001L)
        assertEquals(2_000_000_000L, timeline.elapsedNanos)
    }

    @Test
    fun `windows at one fps share a slot`() {
        assertEquals(jarnWallpaperSlot(100_000_000L, 30), jarnWallpaperSlot(100_000_001L, 30))
        assertEquals(3L, jarnWallpaperSlot(100_000_000L, 30))
    }

    @Test
    fun `fps is clamped to a safe range`() {
        assertEquals(1_000_000_000L, jarnWallpaperFrameNanos(0))
        assertEquals(1_000_000_000L / 120, jarnWallpaperFrameNanos(1_000))
    }

    @Test
    fun `wake delay stops short of the next slot`() {
        assertEquals(25L, jarnWallpaperWakeDelayMillis(0L, 30))
        assertEquals(0L, jarnWallpaperWakeDelayMillis(0L, 120))
    }
}
