package jr.brian.jarnfe.deck

import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnScreen.Deck
import jr.brian.jarnfe.input.JarnScreen.Stage
import jr.brian.jarnfe.settings.playingDimApplies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingBlackoutTest {
    private val game = NowPlayingGame("gc", "/roms/gc/Melee.rvz", "Melee")
    private val onTop = NowPlayingSession(game, launchedAtUptimeMs = 0L, startedAtWallMs = 0L, gameHost = Stage)

    private fun role(
        session: NowPlayingSession? = onTop,
        screenOff: Boolean = true,
        twoScreens: Boolean = true,
        swapped: Boolean = false,
        osFocus: JarnScreen? = null,
        offer: Boolean = false,
    ) = nowPlayingBlackoutRole(session, screenOff, twoScreens, swapped, osFocus, offer)

    @Test
    fun `unswapped, the bottom screen in the Deck role goes black`() {
        assertEquals(Deck, role())
        assertEquals(Deck, role(osFocus = Deck))
    }

    @Test
    fun `swapped, the bottom screen in the Stage role goes black`() {
        assertEquals(Stage, role(swapped = true))
    }

    @Test
    fun `it never goes black on one screen, with the setting off, or with no game in front`() {
        assertNull(role(twoScreens = false))
        assertNull(role(screenOff = false))
        assertNull(role(session = null))
        assertNull(role(session = onTop.copy(gameHost = null)))
    }

    @Test
    fun `it never covers the companion offer`() {
        assertNull(role(offer = true))
        assertNull(role(swapped = true, offer = true))
    }

    @Test
    fun `it clears once Jarn is back on the covered screen`() {
        assertNull(role(osFocus = Stage))
        assertNull(role(swapped = true, osFocus = Deck))
    }

    @Test
    fun `a dismissed Now Playing still goes black, since the setting is about the screen`() {
        assertEquals(Deck, role(session = onTop.copy(dismissed = true)))
    }

    @Test
    fun `black at first, awake after a wake, black again once the delay passes`() {
        assertTrue(nowPlayingBlackoutDue(JarnPlayingDim.ThirtySeconds, lastWakeMs = null, nowMs = 0L))
        assertFalse(nowPlayingBlackoutDue(JarnPlayingDim.ThirtySeconds, lastWakeMs = 1_000L, nowMs = 30_999L))
        assertTrue(nowPlayingBlackoutDue(JarnPlayingDim.ThirtySeconds, lastWakeMs = 1_000L, nowMs = 31_000L))
    }

    @Test
    fun `with Never, a woken screen stays on`() {
        assertTrue(nowPlayingBlackoutDue(JarnPlayingDim.Never, lastWakeMs = null, nowMs = 0L))
        assertFalse(nowPlayingBlackoutDue(JarnPlayingDim.Never, lastWakeMs = 1_000L, nowMs = Long.MAX_VALUE / 2))
    }

    @Test
    fun `the state wakes, then goes black again on the recheck after the delay`() {
        val state = NowPlayingBlackoutState()
        assertTrue(state.isBlack(JarnPlayingDim.TenSeconds))
        state.wake(nowMs = 5_000L)
        assertFalse(state.isBlack(JarnPlayingDim.TenSeconds))
        state.recheck(nowMs = 14_999L)
        assertFalse(state.isBlack(JarnPlayingDim.TenSeconds))
        state.recheck(nowMs = 15_000L)
        assertTrue(state.isBlack(JarnPlayingDim.TenSeconds))
    }

    @Test
    fun `the dim delay setting applies when either Now Playing or screen off is on`() {
        assertTrue(playingDimApplies(JarnDisplayPrefs(playingScreenVisible = true, playingScreenOff = false)))
        assertTrue(playingDimApplies(JarnDisplayPrefs(playingScreenVisible = false, playingScreenOff = true)))
        assertFalse(playingDimApplies(JarnDisplayPrefs(playingScreenVisible = false, playingScreenOff = false)))
    }
}
