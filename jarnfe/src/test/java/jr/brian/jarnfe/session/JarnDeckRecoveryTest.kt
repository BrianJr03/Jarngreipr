package jr.brian.jarnfe.session

import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnDeckRecoveryTest {
    private val free = JarnDeckSurroundings(
        hasDeckDisplay = true,
        isStageResumed = true,
        isStageFinishing = false,
        isDeckHosted = false,
        isLaunchPending = false,
    )
    private val up = JarnDeckRecovery().onDeckUp(0L)
    private val lost = up.onDeckClosed(JarnDeckClose.Unexpected, 1_000L)
    private val held = up.onDeckClosed(JarnDeckClose.Covered, 1_000L)

    @Test
    fun `close kinds separate deliberate, recreated, covered and unexpected`() {
        assertEquals(JarnDeckClose.Deliberate, jarnDeckCloseKind(true, false, true))
        assertNull(jarnDeckCloseKind(false, true, false))
        assertEquals(JarnDeckClose.Covered, jarnDeckCloseKind(false, false, true))
        assertEquals(JarnDeckClose.Unexpected, jarnDeckCloseKind(false, false, false))
    }

    @Test
    fun `unexpected close schedules a relaunch`() {
        assertTrue(lost.isMissing)
        assertEquals(500L, lost.nextRetryDelayMs(free))
    }

    @Test
    fun `deliberate finish never relaunches`() {
        val closed = up.onDeckClosed(JarnDeckClose.Deliberate, 1_000L)
        assertFalse(closed.isMissing)
        assertNull(closed.nextRetryDelayMs(free))
    }

    @Test
    fun `retries back off and then stop`() {
        val delays = generateSequence(lost) { it.onRetryLaunched() }
            .take(DECK_RETRY_LIMIT + 1)
            .map { it.nextRetryDelayMs(free) }
            .toList()
        assertEquals(listOf(500L, 2_000L, 5_000L, null), delays)
    }

    @Test
    fun `a deck that dies right after coming back keeps its spent retries`() {
        val again = lost.onRetryLaunched().onDeckUp(2_000L).onDeckClosed(JarnDeckClose.Unexpected, 3_000L)
        assertEquals(1, again.attempts)
    }

    @Test
    fun `a deck that stayed up earns its retries back`() {
        val spent = lost.onRetryLaunched().onRetryLaunched().onDeckUp(2_000L)
        val again = spent.onDeckClosed(JarnDeckClose.Unexpected, 2_000L + DECK_STABLE_MS)
        assertEquals(0, again.attempts)
    }

    @Test
    fun `a deck closed while covered waits because a game may own the display`() {
        assertTrue(held.isDisplayHeld)
        assertNull(held.nextRetryDelayMs(free))
    }

    @Test
    fun `an app opened while the deck is missing holds the display`() {
        assertNull(lost.onOtherAppOpened().nextRetryDelayMs(free))
    }

    @Test
    fun `focus leaving a resumed stage holds the display once settled`() {
        val unclear = lost.onStageLostFocus()
        assertNull(unclear.nextRetryDelayMs(free))
        assertTrue(unclear.onFocusSettled().isDisplayHeld)
    }

    @Test
    fun `a stage covered on its own display does not hold the deck display`() {
        val covered = lost.onStageLostFocus().onStagePaused()
        assertFalse(covered.onFocusSettled().isDisplayHeld)
        assertEquals(500L, covered.nextRetryDelayMs(free))
    }

    @Test
    fun `jarn in the background waits and restores on return`() {
        assertNull(lost.nextRetryDelayMs(free.copy(isStageResumed = false)))
        assertEquals(500L, lost.nextRetryDelayMs(free))
    }

    @Test
    fun `no relaunch without a display, while finishing, hosted or already launching`() {
        assertNull(lost.nextRetryDelayMs(free.copy(hasDeckDisplay = false)))
        assertNull(lost.nextRetryDelayMs(free.copy(isStageFinishing = true)))
        assertNull(lost.nextRetryDelayMs(free.copy(isDeckHosted = true)))
        assertNull(lost.nextRetryDelayMs(free.copy(isLaunchPending = true)))
    }

    @Test
    fun `switching to classic counts as deliberate and clears a held display`() {
        val switched = held.onDeckClosed(JarnDeckClose.Deliberate, 2_000L)
        assertEquals(JarnDeckRecovery(), switched)
    }

    @Test
    fun `select relaunches at once when the display is free`() {
        val spent = generateSequence(lost) { it.onRetryLaunched() }.elementAt(DECK_RETRY_LIMIT)
        assertTrue(spent.needsSelect)
        assertEquals(JarnDeckSelectAction.Relaunch, spent.onSelect(free, 2_000L).action)
    }

    @Test
    fun `select over a held display asks first and relaunches on the second press`() {
        val first = held.onSelect(free, 2_000L)
        assertEquals(JarnDeckSelectAction.AskConfirm, first.action)
        val second = first.next.onSelect(free, 3_000L)
        assertEquals(JarnDeckSelectAction.Relaunch, second.action)
        assertFalse(second.next.isDisplayHeld)
    }

    @Test
    fun `an expired confirmation asks again`() {
        val first = held.onSelect(free, 2_000L)
        val late = first.next.onSelect(free, 2_001L + DECK_SELECT_CONFIRM_MS)
        assertEquals(JarnDeckSelectAction.AskConfirm, late.action)
    }

    @Test
    fun `a failed manual relaunch does not start automatic retries`() {
        val pressed = lost.onSelect(free, 2_000L).next
        assertNull(pressed.nextRetryDelayMs(free))
    }

    @Test
    fun `select does nothing extra when the deck is present or on its way`() {
        assertEquals(JarnDeckSelectAction.NotMissing, up.onSelect(free, 2_000L).action)
        val pending = lost.onSelect(free.copy(isLaunchPending = true), 2_000L)
        assertEquals(JarnDeckSelectAction.Waiting, pending.action)
    }

    @Test
    fun `session falls back to stage while the deck window is lost`() {
        val state = JarnSessionState(deckDisplayAvailable = true, activeScreen = JarnScreen.Deck)
            .withDeckHost(true)
            .onDeckWindowLost()
        assertTrue(state.deckWindowLost)
        assertFalse(state.deckDisplayAvailable)
        assertEquals(JarnScreen.Stage, state.activeScreen)
        assertEquals(JarnScreen.Stage, state.inputScreen)
    }

    @Test
    fun `swapped screens unswap while lost and swap again on return`() {
        val swapped = JarnSessionState(deckDisplayAvailable = true, swapRequested = true).withDeckHost(true)
        assertTrue(swapped.rolesSwapped)
        val missing = swapped.onDeckWindowLost()
        assertFalse(missing.rolesSwapped)
        assertEquals(JarnScreen.Stage, missing.roleOn(JarnScreen.Stage))
        assertTrue(missing.onDeckWindowBack().withDeckHost(true).rolesSwapped)
    }

    @Test
    fun `settings controls return to the deck when it comes back`() {
        val settings = JarnSessionState(deckDisplayAvailable = true).withDeckHost(true).openSettings()
        val missing = settings.onDeckWindowLost()
        assertEquals(JarnScreen.Stage, missing.activeScreen)
        val back = missing.onDeckWindowBack().withDeckHost(true)
        assertEquals(JarnScreen.Deck, back.activeScreen)
        assertNotNull(back.settingsReturnScreen)
    }

    @Test
    fun `a removed display clears the lost window`() {
        val missing = JarnSessionState(deckDisplayAvailable = true).withDeckHost(true).onDeckWindowLost()
        assertFalse(missing.withDeckDisplay(false).deckWindowLost)
    }

    @Test
    fun `a panel opened while lost closes when the deck returns`() {
        val missing = JarnSessionState(deckDisplayAvailable = true).withDeckHost(true).onDeckWindowLost()
        val panel = missing.switchActiveScreen()
        assertTrue(panel.deckPanelOpen)
        val back = panel.onDeckWindowBack().withDeckHost(true)
        assertFalse(back.deckPanelOpen)
        assertEquals(JarnScreen.Stage, back.activeScreen)
    }
}
