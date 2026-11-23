package jr.brian.jarnfe.session

import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnSessionStateTest {
    private val dual = JarnSessionState(deckDisplayAvailable = true)

    @Test
    fun `opening a route pushes it and reopening unwinds to it`() {
        val deep = dual.openRoute(StageRoute.GameList).openRoute(StageRoute.Settings)
        assertEquals(listOf(StageRoute.Library, StageRoute.GameList, StageRoute.Settings), deep.stageStack)
        assertEquals(listOf(StageRoute.Library), deep.openRoute(StageRoute.Library).stageStack)
    }

    @Test
    fun `back at library root does nothing`() {
        assertEquals(dual, dual.back())
        assertFalse(dual.canGoBack)
    }

    @Test
    fun `back follows the layer order`() {
        val state = dual.openRoute(StageRoute.GameList).openRoute(StageRoute.Settings)
            .copy(activeScreen = JarnScreen.Deck)
        val afterSettings = state.back()
        assertEquals(StageRoute.GameList, afterSettings.stageRoute)
        assertEquals(JarnScreen.Deck, afterSettings.activeScreen)
        val afterDeck = afterSettings.back()
        assertEquals(JarnScreen.Stage, afterDeck.activeScreen)
        assertEquals(StageRoute.GameList, afterDeck.stageRoute)
        assertEquals(StageRoute.Library, afterDeck.back().stageRoute)
    }

    @Test
    fun `switching toggles screens with two displays and the panel with one`() {
        assertEquals(JarnScreen.Deck, dual.switchActiveScreen().activeScreen)
        val single = JarnSessionState().switchActiveScreen()
        assertTrue(single.deckPanelOpen)
        assertEquals(JarnScreen.Deck, single.activeScreen)
        assertFalse(single.switchActiveScreen().deckPanelOpen)
    }

    @Test
    fun `back closes the single screen panel first`() {
        val open = JarnSessionState().openRoute(StageRoute.GameList).withDeckPanel(true)
        val closed = open.back()
        assertFalse(closed.deckPanelOpen)
        assertEquals(StageRoute.GameList, closed.stageRoute)
    }

    @Test
    fun `losing the deck display falls back without losing place`() {
        val state = dual.openRoute(StageRoute.GameList)
            .copy(activeScreen = JarnScreen.Deck, deckTab = DeckTab.Canvas, osFocusedScreen = JarnScreen.Deck)
        val lost = state.withDeckDisplay(false)
        assertEquals(JarnScreen.Stage, lost.activeScreen)
        assertEquals(StageRoute.GameList, lost.stageRoute)
        assertEquals(DeckTab.Canvas, lost.deckTab)
        assertNull(lost.osFocusedScreen)
        assertEquals(DeckTab.Canvas, lost.withDeckDisplay(true).deckTab)
    }

    @Test
    fun `restored deck focus survives the first display sync`() {
        val restored = JarnSessionState(activeScreen = JarnScreen.Deck)
        assertEquals(JarnScreen.Deck, restored.withDeckDisplay(true).activeScreen)
        assertEquals(JarnScreen.Stage, restored.withDeckDisplay(false).activeScreen)
    }

    @Test
    fun `an open panel survives repeated single screen syncs`() {
        val open = JarnSessionState().withDeckPanel(true).withDeckDisplay(false)
        assertTrue(open.deckPanelOpen)
        assertEquals(JarnScreen.Deck, open.activeScreen)
    }

    @Test
    fun `a returning display closes the panel and lights stage`() {
        val returned = JarnSessionState().withDeckPanel(true).withDeckDisplay(true)
        assertFalse(returned.deckPanelOpen)
        assertEquals(JarnScreen.Stage, returned.activeScreen)
    }

    @Test
    fun `a freshly launched deck hands focus back to stage`() {
        val launching = dual.onTopResumed(JarnScreen.Stage, true).deckLaunchStarted()
        val stageLost = launching.onTopResumed(JarnScreen.Stage, false)
        val deckTop = stageLost.onTopResumed(JarnScreen.Deck, true)
        assertEquals(JarnScreen.Stage, deckTop.activeScreen)
        assertFalse(deckTop.awaitingDeckLaunch)
        assertTrue(deckTop.needsFocusHandoff(JarnScreen.Stage))
        val settled = deckTop.onTopResumed(JarnScreen.Deck, false).onTopResumed(JarnScreen.Stage, true)
        assertFalse(settled.needsFocusHandoff(JarnScreen.Stage))
        assertFalse(settled.needsFocusHandoff(JarnScreen.Deck))
    }

    @Test
    fun `deck launches only when started with a display and no deck`() {
        assertTrue(dual.shouldLaunchDeck(isStarted = true, deckHostCreated = false))
        assertFalse(dual.shouldLaunchDeck(isStarted = false, deckHostCreated = false))
        assertFalse(dual.shouldLaunchDeck(isStarted = true, deckHostCreated = true))
        assertFalse(JarnSessionState().shouldLaunchDeck(isStarted = true, deckHostCreated = false))
    }

    @Test
    fun `a pending launch blocks duplicates and a failed one can retry`() {
        val pending = dual.deckLaunchStarted()
        assertFalse(pending.shouldLaunchDeck(isStarted = true, deckHostCreated = false))
        assertTrue(pending.deckLaunchFailed().shouldLaunchDeck(isStarted = true, deckHostCreated = false))
    }

    @Test
    fun `a timed out launch lets the active screen follow focus again`() {
        val recovered = dual.onTopResumed(JarnScreen.Stage, true)
            .deckLaunchStarted()
            .deckLaunchFailed()
            .onTopResumed(JarnScreen.Stage, false)
            .onTopResumed(JarnScreen.Deck, true)
        assertEquals(JarnScreen.Deck, recovered.activeScreen)
    }

    @Test
    fun `touching the other screen makes it active without a handoff`() {
        val touched = dual.onTopResumed(JarnScreen.Stage, true)
            .onTopResumed(JarnScreen.Stage, false)
            .onTopResumed(JarnScreen.Deck, true)
        assertEquals(JarnScreen.Deck, touched.activeScreen)
        assertFalse(touched.needsFocusHandoff(JarnScreen.Stage))
        assertFalse(touched.needsFocusHandoff(JarnScreen.Deck))
    }

    @Test
    fun `no handoff while another app holds focus`() {
        val away = dual.onTopResumed(JarnScreen.Stage, true).onTopResumed(JarnScreen.Stage, false)
        val switched = away.switchActiveScreen()
        assertFalse(switched.needsFocusHandoff(JarnScreen.Deck))
        assertFalse(switched.needsFocusHandoff(JarnScreen.Stage))
    }

    @Test
    fun `the only screen stays lit`() {
        assertTrue(JarnSessionState().isLit(JarnScreen.Stage))
        assertTrue(JarnSessionState().withDeckPanel(true).isLit(JarnScreen.Stage))
        assertFalse(dual.copy(activeScreen = JarnScreen.Deck).isLit(JarnScreen.Stage))
    }

    @Test
    fun `encoding round trips the persisted fields only`() {
        val state = dual.openRoute(StageRoute.GameList).copy(
            activeScreen = JarnScreen.Deck,
            librarySection = LibrarySection.Recent,
            deckTab = DeckTab.Apps,
            osFocusedScreen = JarnScreen.Deck,
            awaitingDeckLaunch = true,
        )
        val restored = JarnSessionState.decode(state.encode())!!
        assertEquals(state.stageStack, restored.stageStack)
        assertEquals(JarnScreen.Deck, restored.activeScreen)
        assertEquals(LibrarySection.Recent, restored.librarySection)
        assertEquals(DeckTab.Apps, restored.deckTab)
        assertNull(restored.osFocusedScreen)
        assertFalse(restored.awaitingDeckLaunch)
    }

    @Test
    fun `a session saved on the old Browse tab restores as Apps`() {
        assertEquals(DeckTab.Apps, JarnSessionState.decode("Stage|Library|Systems|Browse")?.deckTab)
    }

    @Test
    fun `corrupt saved state is rejected`() {
        listOf(
            null,
            "",
            "Stage|Library|Systems",
            "Nope|Library|Systems|Now",
            "Stage|GameList|Systems|Now",
            "Stage|Library,Library|Systems|Now",
            "Stage|Library,Nope|Systems|Now",
        ).forEach { assertNull(it, JarnSessionState.decode(it)) }
    }

    @Test
    fun `a live session is not rolled back by a recreated activity`() {
        val store = JarnSessionStore()
        store.update { copy(deckTab = DeckTab.Canvas) }
        store.restoreIfFresh(JarnSessionState().encode())
        assertEquals(DeckTab.Canvas, store.state.deckTab)
    }

    @Test
    fun `a new process restores saved state once`() {
        val store = JarnSessionStore()
        store.restoreIfFresh(JarnSessionState(deckTab = DeckTab.Apps).encode())
        assertEquals(DeckTab.Apps, store.state.deckTab)
        store.restoreIfFresh(JarnSessionState(deckTab = DeckTab.Canvas).encode())
        assertEquals(DeckTab.Apps, store.state.deckTab)
    }
}
