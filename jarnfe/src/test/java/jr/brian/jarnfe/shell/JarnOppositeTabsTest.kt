package jr.brian.jarnfe.shell

import android.view.KeyEvent
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.jarnBindings
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.search.JarnSearchStore
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.SettingsSection
import jr.brian.jarnfe.session.StageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JarnOppositeTabsTest {
    private val router = JarnInputRouter()
    private val session = JarnSessionStore(router::activate)
    private val library = JarnLibraryStore()
    private val search = JarnSearchStore()
    private var clock = 0L

    private val dual = JarnSessionState(deckDisplayAvailable = true, deckHostReady = true)

    @Before
    fun setUp() {
        router.setGlobalBindings(
            jarnGlobalBindings(
                session,
                onOppositeTab = { stepOppositeTabs(it, session, library, search, router) },
            ),
        )
        session.update { withDeckDisplay(true).withDeckHost(true) }
    }

    private fun JarnSessionState.strip(actions: Boolean = false, modal: Boolean = false) =
        oppositeTabStrip(actions, modal)

    private fun pull(keyCode: Int) {
        router.dispatch(JarnKeyInput(keyCode, JarnKeyPhase.Down, clock++))
        router.dispatch(JarnKeyInput(keyCode, JarnKeyPhase.Up, clock++))
    }

    @Test
    fun `from stage the deck tabs are stepped`() {
        assertEquals(JarnTabStrip.DeckTabs, dual.strip())
    }

    @Test
    fun `from deck the stage route picks the strip`() {
        val deck = dual.copy(activeScreen = JarnScreen.Deck)
        assertEquals(JarnTabStrip.LibrarySections, deck.strip())
        assertEquals(JarnTabStrip.GameListSystems, deck.openRoute(StageRoute.GameList).strip())
        assertEquals(JarnTabStrip.SearchTabs, deck.openRoute(StageRoute.Search).strip())
    }

    @Test
    fun `stage routes without tabs step nothing`() {
        val deck = dual.copy(activeScreen = JarnScreen.Deck)
        assertNull(deck.openRoute(StageRoute.Settings).strip())
        assertNull(deck.openRoute(StageRoute.Systems).strip())
    }

    @Test
    fun `settings controls on the deck are stepped from stage`() {
        val state = dual.openSettings().activate(JarnScreen.Stage)
        assertEquals(JarnTabStrip.SettingsSections, state.strip())
    }

    @Test
    fun `search keyboard on the deck has no tabs but its actions page does`() {
        val state = dual.openRoute(StageRoute.Search)
        assertNull(state.strip(actions = false))
        assertEquals(JarnTabStrip.DeckTabs, state.strip(actions = true))
    }

    @Test
    fun `swapped roles still target the screen without control`() {
        val swapped = dual.withSwapRequested(true)
        assertTrue(swapped.rolesSwapped)
        assertEquals(JarnTabStrip.DeckTabs, swapped.strip())
        assertEquals(JarnTabStrip.LibrarySections, swapped.activate(JarnScreen.Deck).strip())
    }

    @Test
    fun `single screen and a missing deck window step nothing`() {
        assertNull(JarnSessionState().strip())
        assertNull(JarnSessionState(deckPanelOpen = true, activeScreen = JarnScreen.Deck).strip())
        assertNull(dual.copy(deckHostReady = false).strip())
    }

    @Test
    fun `an open modal steps nothing`() {
        assertNull(dual.strip(modal = true))
    }

    @Test
    fun `a game action page in use on now blocks the step`() {
        val onNow = dual.copy(activeScreen = JarnScreen.Deck, deckTab = DeckTab.Now)
        assertNull(onNow.oppositeTabStrip(false, false, gameActionPageOpen = true))
        val onApps = onNow.copy(deckTab = DeckTab.Apps)
        assertEquals(
            JarnTabStrip.LibrarySections,
            onApps.oppositeTabStrip(false, false, gameActionPageOpen = true),
        )
    }

    @Test
    fun `r2 steps the other screen and leaves control alone`() {
        pull(KeyEvent.KEYCODE_BUTTON_R2)
        assertEquals(DeckTab.Apps, session.state.deckTab)
        assertEquals(LibrarySection.Systems, session.state.librarySection)
        assertEquals(JarnScreen.Stage, session.state.activeScreen)
        assertEquals(JarnScreen.Stage, router.activeScreen)
        pull(KeyEvent.KEYCODE_BUTTON_L2)
        assertEquals(DeckTab.Now, session.state.deckTab)
    }

    @Test
    fun `one held pull steps once`() {
        router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_R2, JarnKeyPhase.Down, 0))
        repeat(5) { index ->
            val repeat = JarnKeyInput(KeyEvent.KEYCODE_BUTTON_R2, JarnKeyPhase.Down, 600L + index, index + 1)
            assertTrue(router.dispatch(repeat))
        }
        assertTrue(router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_R2, JarnKeyPhase.Up, 700)))
        assertEquals(DeckTab.Apps, session.state.deckTab)
    }

    @Test
    fun `a modal on the active screen consumes the pull`() {
        val modal = router.createLayer(JarnScreen.Stage, JarnLayerMode.Modal, depth = 1)
        modal.update(jarnBindings { })
        router.attach(modal)
        assertTrue(router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_R2, JarnKeyPhase.Down, 0)))
        assertEquals(DeckTab.Now, session.state.deckTab)
    }

    @Test
    fun `a modal on the opposite screen blocks the step`() {
        val modal = router.createLayer(JarnScreen.Deck, JarnLayerMode.Modal, depth = 1)
        modal.update(jarnBindings { })
        router.attach(modal)
        assertTrue(router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_R2, JarnKeyPhase.Down, 0)))
        assertEquals(DeckTab.Now, session.state.deckTab)
    }

    @Test
    fun `r2 steps settings sections on the deck from stage`() {
        session.update { openSettings().activate(JarnScreen.Stage) }
        pull(KeyEvent.KEYCODE_BUTTON_R2)
        assertEquals(SettingsSection.Art, session.state.settingsSection)
        assertEquals(JarnScreen.Stage, session.state.activeScreen)
    }
}
