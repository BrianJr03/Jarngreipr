package jr.brian.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnHomeRoutingTest {
    private val stage = JarnHomeLaunch(JarnHomeWindow.Stage, PRIMARY_DISPLAY_ID)

    @Test
    fun `a running deck is raised before stage so stage keeps focus`() {
        assertEquals(
            listOf(JarnHomeLaunch(JarnHomeWindow.Deck, 4), stage),
            planJarnHomeLaunches(bottomDisplayId = 4, shouldRaiseDeck = true),
        )
    }

    @Test
    fun `a deck that does not exist is left to stage`() {
        assertEquals(listOf(stage), planJarnHomeLaunches(bottomDisplayId = 4, shouldRaiseDeck = false))
    }

    @Test
    fun `with the bottom screen gone only stage opens`() {
        assertEquals(listOf(stage), planJarnHomeLaunches(bottomDisplayId = null, shouldRaiseDeck = true))
    }

    @Test
    fun `the tour keeps control on the deck by starting it last`() {
        assertEquals(
            listOf(stage, JarnHomeLaunch(JarnHomeWindow.Deck, 4)),
            planJarnHomeLaunches(4, shouldRaiseDeck = true, focus = JarnHomeWindow.Deck),
        )
    }

    private fun action(
        press: JarnHomePress = JarnHomePress.Single,
        cover: JarnGameCover,
        hasBottom: Boolean = true,
        isRepeat: Boolean = false,
    ) = resolveJarnHomeAction(press, cover, hasBottom) { isRepeat }

    @Test
    fun `single press with no game opens jarn`() {
        assertEquals(JarnHomeAction.OpenJarn, action(cover = JarnGameCover.None))
    }

    @Test
    fun `single press keeps a top game and shows the deck`() {
        assertEquals(JarnHomeAction.ShowDeck, action(cover = JarnGameCover(top = true)))
    }

    @Test
    fun `single press keeps a bottom game and shows stage`() {
        assertEquals(JarnHomeAction.ShowStage, action(cover = JarnGameCover(bottom = true)))
    }

    @Test
    fun `a game on both screens asks first and opens on the repeat`() {
        val both = JarnGameCover(top = true, bottom = true)
        assertEquals(JarnHomeAction.AskAgain, action(cover = both))
        assertEquals(JarnHomeAction.OpenJarn, action(cover = both, isRepeat = true))
    }

    @Test
    fun `a game on the only screen asks first`() {
        assertEquals(JarnHomeAction.AskAgain, action(cover = JarnGameCover(top = true), hasBottom = false))
    }

    @Test
    fun `a stale bottom cover with no bottom screen opens jarn`() {
        assertEquals(JarnHomeAction.OpenJarn, action(cover = JarnGameCover(bottom = true), hasBottom = false))
    }

    @Test
    fun `double and long press open jarn during a game`() {
        listOf(JarnGameCover(top = true), JarnGameCover(bottom = true), JarnGameCover(true, true)).forEach {
            assertEquals(JarnHomeAction.OpenJarn, action(press = JarnHomePress.Open, cover = it))
        }
    }

    @Test
    fun `a repeat only counts inside the window and is used once`() {
        val repeat = JarnHomeRepeat(windowMs = 100L)
        assertFalse(repeat.isRepeat(0L))
        assertTrue(repeat.isRepeat(99L))
        assertFalse(repeat.isRepeat(100L))
        assertFalse(repeat.isRepeat(200L))
    }

    @Test
    fun `stops without a launched game are not a cover`() {
        val tracker = JarnGameCoverTracker()
        tracker.onWindowStopped(JarnHomeWindow.Stage, 0L)
        assertEquals(JarnGameCover.None, tracker.cover)
    }

    @Test
    fun `a launched game covers the window it stopped`() {
        val tracker = JarnGameCoverTracker()
        tracker.onGameLaunched(0L)
        tracker.onWindowStopped(JarnHomeWindow.Stage, 10L)
        assertEquals(JarnGameCover(top = true), tracker.cover)
    }

    @Test
    fun `raising the free window keeps the game cover`() {
        val tracker = JarnGameCoverTracker()
        tracker.onGameLaunched(0L)
        tracker.onWindowStopped(JarnHomeWindow.Stage, 10L)
        tracker.onWindowResumed(JarnHomeWindow.Deck)
        assertEquals(JarnGameCover(top = true), tracker.cover)
    }

    @Test
    fun `the game ends when every covered window is back`() {
        val tracker = JarnGameCoverTracker()
        tracker.onGameLaunched(0L)
        tracker.onWindowStopped(JarnHomeWindow.Stage, 10L)
        tracker.onWindowStopped(JarnHomeWindow.Deck, 20L)
        tracker.onWindowResumed(JarnHomeWindow.Stage)
        assertEquals(JarnGameCover(bottom = true), tracker.cover)
        tracker.onWindowResumed(JarnHomeWindow.Deck)
        tracker.onWindowStopped(JarnHomeWindow.Stage, 30L)
        assertEquals(JarnGameCover.None, tracker.cover)
    }

    @Test
    fun `a launch that never covered anything expires`() {
        val tracker = JarnGameCoverTracker(launchGraceMs = 100L)
        tracker.onGameLaunched(0L)
        tracker.onWindowStopped(JarnHomeWindow.Stage, 101L)
        assertEquals(JarnGameCover.None, tracker.cover)
    }

    @Test
    fun `repeated entries without a deck fall back on the fourth`() {
        val guard = JarnHomeLoopGuard()
        repeat(3) { assertTrue(guard.allowsJarn(nowMs = it * 10L, isDeckUp = false)) }
        assertFalse(guard.allowsJarn(nowMs = 30L, isDeckUp = false))
    }

    @Test
    fun `the guard allows jarn again right after a fallback`() {
        val guard = JarnHomeLoopGuard(limit = 2)
        guard.allowsJarn(nowMs = 0L, isDeckUp = false)
        assertFalse(guard.allowsJarn(nowMs = 1L, isDeckUp = false))
        assertTrue(guard.allowsJarn(nowMs = 2L, isDeckUp = false))
    }

    @Test
    fun `entries outside the window do not count`() {
        val guard = JarnHomeLoopGuard(limit = 2, windowMs = 100L)
        assertTrue(guard.allowsJarn(nowMs = 0L, isDeckUp = false))
        assertTrue(guard.allowsJarn(nowMs = 100L, isDeckUp = false))
    }

    @Test
    fun `a deck that came up clears the count`() {
        val guard = JarnHomeLoopGuard(limit = 2)
        guard.allowsJarn(nowMs = 0L, isDeckUp = false)
        assertTrue(guard.allowsJarn(nowMs = 1L, isDeckUp = true))
        assertTrue(guard.allowsJarn(nowMs = 2L, isDeckUp = false))
    }

    @Test
    fun `mashing home with the deck up never falls back`() {
        val guard = JarnHomeLoopGuard()
        repeat(20) { assertTrue(guard.allowsJarn(nowMs = it.toLong(), isDeckUp = true)) }
    }
}
