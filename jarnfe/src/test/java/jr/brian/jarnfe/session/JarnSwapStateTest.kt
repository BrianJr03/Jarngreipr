package jr.brian.jarnfe.session

import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnSwapStateTest {
    private val dual = JarnSessionState().withDeckDisplay(true).withDeckHost(true)

    @Test
    fun `swap flips the windows and keeps the active role`() {
        val swapped = dual.copy(osFocusedScreen = JarnScreen.Stage).withSwapRequested(true)
        assertEquals(JarnScreen.Deck, swapped.roleOn(JarnScreen.Stage))
        assertEquals(JarnScreen.Stage, swapped.roleOn(JarnScreen.Deck))
        assertEquals(JarnScreen.Stage, swapped.activeScreen)
        assertEquals(JarnScreen.Deck, swapped.osFocusedScreen)
        assertTrue(swapped.needsFocusHandoff(JarnScreen.Stage))
    }

    @Test
    fun `a saved swap waits for a Deck window`() {
        val noHost = JarnSessionState().withDeckDisplay(true).withSwapRequested(true)
        assertFalse(noHost.rolesSwapped)
        assertEquals(JarnScreen.Stage, noHost.roleOn(JarnScreen.Stage))
        assertTrue(noHost.withDeckHost(true).rolesSwapped)
    }

    @Test
    fun `top resumed reports the role the window shows`() {
        val swapped = dual.withSwapRequested(true).onTopResumed(JarnScreen.Stage, true)
        assertEquals(JarnScreen.Deck, swapped.osFocusedScreen)
        assertEquals(JarnScreen.Deck, swapped.activeScreen)
    }

    @Test
    fun `losing the second display returns Stage to the remaining window`() {
        val swapped = dual.withSwapRequested(true).onTopResumed(JarnScreen.Stage, true)
        val single = swapped.withDeckDisplay(false)
        assertFalse(single.rolesSwapped)
        assertEquals(JarnScreen.Stage, single.osFocusedScreen)
        assertEquals(JarnScreen.Stage, single.activeScreen)
    }

    @Test
    fun `search opens on the Deck and B returns through results`() {
        val open = dual.openSearch()
        assertEquals(StageRoute.Search, open.stageRoute)
        assertEquals(JarnScreen.Deck, open.activeScreen)
        val results = open.back()
        assertEquals(StageRoute.Search, results.stageRoute)
        assertEquals(JarnScreen.Stage, results.activeScreen)
        assertEquals(StageRoute.Library, results.back().stageRoute)
    }

    @Test
    fun `search uses the panel on one screen`() {
        val open = JarnSessionState().openSearch()
        assertTrue(open.deckPanelOpen)
        val results = open.back()
        assertFalse(results.deckPanelOpen)
        assertEquals(StageRoute.Search, results.stageRoute)
    }
}
