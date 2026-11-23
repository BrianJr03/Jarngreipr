package jr.brian.jarnfe.session

import jr.brian.frontend.look.JarnTapControl
import jr.brian.frontend.look.JarnTapControl.AlwaysBottom
import jr.brian.frontend.look.JarnTapControl.AlwaysTop
import jr.brian.frontend.look.JarnTapControl.FollowTap
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnScreen.Deck
import jr.brian.jarnfe.input.JarnScreen.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnTapControlStateTest {
    private val dual = JarnSessionState().withDeckDisplay(true).withDeckHost(true)
        .onTopResumed(Stage, true)

    private fun JarnSessionState.tap(
        host: JarnScreen,
        choice: JarnTapControl,
        visible: Boolean = true,
        modals: Set<JarnScreen> = emptySet(),
    ) = onScreenTapped(host, choice, visible) { it in modals }

    private fun JarnSessionState.osMovesTo(host: JarnScreen) =
        onTopResumed(host.opposite(), false).onTopResumed(host, true)

    private fun JarnScreen.opposite() = if (this == Stage) Deck else Stage

    private fun JarnSessionState.noHandoff() =
        assertFalse(needsFocusHandoff(Stage) || needsFocusHandoff(Deck))

    @Test
    fun `follow tap moves control with the touch and the os`() {
        val tapped = dual.tap(Deck, FollowTap)
        assertEquals(Deck, tapped.activeScreen)
        assertNull(tapped.tapHold)
        assertEquals(Deck, tapped.osMovesTo(Deck).activeScreen)
    }

    @Test
    fun `always top keeps control when the bottom is tapped, in either event order`() {
        val downFirst = dual.tap(Deck, AlwaysTop).osMovesTo(Deck)
        val focusFirst = dual.osMovesTo(Deck).tap(Deck, AlwaysTop)
        listOf(downFirst, focusFirst).forEach {
            assertEquals(Stage, it.activeScreen)
            assertEquals(Deck, it.osFocusedScreen)
            it.noHandoff()
        }
    }

    @Test
    fun `always bottom pulls control to the bottom from a top tap`() {
        val tapped = dual.tap(Stage, AlwaysBottom)
        assertEquals(Deck, tapped.activeScreen)
        tapped.noHandoff()
        assertEquals(Deck, tapped.osMovesTo(Stage).activeScreen)
    }

    @Test
    fun `top and bottom stay physical after a swap`() {
        val swapped = dual.withSwapRequested(true)
        assertEquals(swapped.roleOn(Stage), swapped.tap(Deck, AlwaysTop).activeScreen)
        assertEquals(swapped.roleOn(Deck), swapped.tap(Stage, AlwaysBottom).activeScreen)
    }

    @Test
    fun `a swap drops the hold so the normal handoff runs`() {
        val held = dual.tap(Deck, AlwaysTop).osMovesTo(Deck)
        assertNull(held.withSwapRequested(true).tapHold)
    }

    @Test
    fun `select survives the following focus event and the next tap returns control`() {
        val held = dual.tap(Deck, AlwaysTop).osMovesTo(Deck)
        val selected = held.switchActiveScreen()
        assertEquals(Deck, selected.activeScreen)
        assertEquals(Deck, selected.onTopResumed(Deck, true).activeScreen)
        assertEquals(Stage, selected.tap(Deck, AlwaysTop).activeScreen)
    }

    @Test
    fun `select back to the pinned screen uses the normal handoff`() {
        val back = dual.tap(Deck, AlwaysTop).osMovesTo(Deck).switchActiveScreen().switchActiveScreen()
        assertEquals(Stage, back.activeScreen)
        assertTrue(back.needsFocusHandoff(Stage))
    }

    @Test
    fun `a modal opened by the tap takes control and gives it back on close`() {
        val tapped = dual.tap(Deck, AlwaysTop).osMovesTo(Deck)
        val open = tapped.onModalChanged(Deck, true)
        assertEquals(Deck, open.activeScreen)
        open.noHandoff()
        val closed = open.onModalChanged(Deck, false)
        assertEquals(Stage, closed.activeScreen)
        closed.noHandoff()
    }

    @Test
    fun `a modal keeps its return when focus arrives after it opens`() {
        val open = dual.tap(Deck, AlwaysTop).onModalChanged(Deck, true).osMovesTo(Deck)
        assertEquals(Stage, open.onModalChanged(Deck, false).activeScreen)
    }

    @Test
    fun `a tap elsewhere does not pull control off an open modal`() {
        val open = dual.tap(Deck, AlwaysTop).osMovesTo(Deck).onModalChanged(Deck, true)
        val tapped = open.tap(Stage, AlwaysTop, modals = setOf(Deck))
        assertEquals(Deck, tapped.activeScreen)
        assertEquals(Stage, tapped.onModalChanged(Deck, false).activeScreen)
    }

    @Test
    fun `tapping a screen with a modal already open gives it control`() {
        val tapped = dual.tap(Deck, AlwaysTop, modals = setOf(Deck))
        assertEquals(Deck, tapped.activeScreen)
        assertEquals(Stage, tapped.onModalChanged(Deck, false).activeScreen)
    }

    @Test
    fun `a modal on the other screen that no tap opened takes nothing`() {
        val held = dual.tap(Stage, AlwaysTop)
        assertEquals(Stage, held.onModalChanged(Deck, true).activeScreen)
    }

    @Test
    fun `settings keeps its deck controls under a pinned top`() {
        val settings = dual.openSettings()
        assertEquals(Deck, settings.tap(Deck, AlwaysTop).activeScreen)
        assertEquals(Deck, settings.tap(Stage, AlwaysTop).activeScreen)
    }

    @Test
    fun `system management keeps control on stage under a pinned bottom`() {
        assertEquals(Stage, dual.openSystems().tap(Deck, AlwaysBottom).activeScreen)
    }

    @Test
    fun `a covered window makes the tap follow the touch`() {
        val tapped = dual.tap(Deck, AlwaysTop, visible = false)
        assertEquals(Deck, tapped.activeScreen)
        assertNull(tapped.tapHold)
    }

    @Test
    fun `returning from a game follows android once the hold is released`() {
        val held = dual.tap(Deck, AlwaysTop).osMovesTo(Deck)
        val returned = held.onWindowCovered().onTopResumed(Deck, false).onTopResumed(Deck, true)
        assertEquals(Deck, returned.activeScreen)
    }

    @Test
    fun `covering a window never starts a handoff over the game`() {
        val covered = dual.tap(Deck, AlwaysTop).osMovesTo(Deck).onWindowCovered()
        assertNull(covered.tapHold)
        covered.noHandoff()
    }

    @Test
    fun `covering a window without a hold changes nothing`() {
        val selected = dual.switchActiveScreen()
        assertEquals(selected, selected.onWindowCovered())
    }

    @Test
    fun `switching back to follow tap keeps control and restores the handoff`() {
        val released = dual.tap(Deck, AlwaysTop).osMovesTo(Deck).releasingTapHold()
        assertEquals(Stage, released.activeScreen)
        assertTrue(released.needsFocusHandoff(Stage))
    }

    @Test
    fun `a window launch in flight is left alone`() {
        val launching = dual.deckLaunchStarted()
        assertEquals(Deck, launching.tap(Deck, AlwaysTop).activeScreen)
    }

    @Test
    fun `single screen ignores the choice`() {
        val single = JarnSessionState()
        JarnTapControl.entries.forEach { choice ->
            val tapped = single.tap(Stage, choice)
            assertEquals(Stage, tapped.activeScreen)
            assertNull(tapped.tapHold)
        }
    }

    @Test
    fun `losing the deck clears the hold`() {
        val held = dual.tap(Deck, AlwaysTop)
        assertNull(held.withDeckHost(false).tapHold)
        assertNull(held.withDeckDisplay(false).tapHold)
    }

    @Test
    fun `an unrelated handoff still runs without a hold`() {
        assertTrue(dual.activate(Deck).needsFocusHandoff(Deck))
    }
}
