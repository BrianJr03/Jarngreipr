package jr.brian.jarnfe.shell

import android.view.KeyEvent
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnShoulderPair
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

class JarnScreenTabsTest {
    private val router = JarnInputRouter()
    private val session = JarnSessionStore(router::activate)
    private val library = JarnLibraryStore()
    private val search = JarnSearchStore()
    private var clock = 0L

    private val dual = JarnSessionState(deckDisplayAvailable = true, deckHostReady = true)
    private val top = JarnShoulderPair.Top
    private val bottom = JarnShoulderPair.Bottom

    @Before
    fun setUp() {
        router.setGlobalBindings(
            jarnGlobalBindings(
                session,
                onScreenTab = { pair, delta -> stepScreenTabs(pair, delta, session, library, search, router) },
            ),
        )
        session.update { withDeckDisplay(true).withDeckHost(true) }
    }

    private fun JarnSessionState.strip(
        pair: JarnShoulderPair,
        actions: Boolean = false,
        modal: Boolean = false,
    ) = tabStripAt(pair, actions, modal)

    private fun pull(keyCode: Int) {
        router.dispatch(JarnKeyInput(keyCode, JarnKeyPhase.Down, clock++))
        router.dispatch(JarnKeyInput(keyCode, JarnKeyPhase.Up, clock++))
    }

    private fun assertControlOn(screen: JarnScreen) {
        assertEquals(screen, session.state.activeScreen)
        assertEquals(screen, router.activeScreen)
    }

    @Test
    fun `l1 steps the top screen and l2 the bottom one from either screen`() {
        assertEquals(JarnTabStrip.LibrarySections, dual.strip(top))
        assertEquals(JarnTabStrip.DeckTabs, dual.strip(bottom))
        val deck = dual.activate(JarnScreen.Deck)
        assertEquals(JarnTabStrip.LibrarySections, deck.strip(top))
        assertEquals(JarnTabStrip.DeckTabs, deck.strip(bottom))
    }

    @Test
    fun `the stage route picks the top strip`() {
        val deck = dual.activate(JarnScreen.Deck)
        assertEquals(JarnTabStrip.GameListSystems, deck.openRoute(StageRoute.GameList).strip(top))
        assertEquals(JarnTabStrip.SearchTabs, deck.openRoute(StageRoute.Search).strip(top))
    }

    @Test
    fun `stage routes without tabs step nothing`() {
        assertNull(dual.openRoute(StageRoute.Systems).strip(top))
        assertNull(dual.openSettings().activate(JarnScreen.Stage).strip(top))
    }

    @Test
    fun `settings sections sit on the bottom screen with the controls`() {
        val fromStage = dual.openSettings().activate(JarnScreen.Stage)
        assertEquals(JarnTabStrip.SettingsSections, fromStage.strip(bottom))
        assertEquals(JarnTabStrip.SettingsSections, dual.openSettings().strip(bottom))
    }

    @Test
    fun `search keyboard on the deck has no tabs but its actions page does`() {
        val state = dual.openRoute(StageRoute.Search)
        assertNull(state.strip(bottom, actions = false))
        assertEquals(JarnTabStrip.DeckTabs, state.strip(bottom, actions = true))
    }

    @Test
    fun `after a swap l1 steps the deck on top and l2 the stage below`() {
        val swapped = dual.withSwapRequested(true)
        assertTrue(swapped.rolesSwapped)
        assertEquals(JarnTabStrip.DeckTabs, swapped.strip(top))
        assertEquals(JarnTabStrip.LibrarySections, swapped.strip(bottom))
        val onDeck = swapped.activate(JarnScreen.Deck)
        assertEquals(JarnTabStrip.DeckTabs, onDeck.strip(top))
        assertEquals(JarnTabStrip.LibrarySections, onDeck.strip(bottom))
    }

    @Test
    fun `a saved swap without a deck window keeps stage on top`() {
        val waiting = dual.copy(deckHostReady = false).withSwapRequested(true)
        assertEquals(JarnScreen.Stage, waiting.roleAt(top))
        assertEquals(JarnTabStrip.LibrarySections, waiting.strip(top))
        assertNull(waiting.strip(bottom))
    }

    @Test
    fun `a single screen gives both pairs to its tabs`() {
        val single = JarnSessionState()
        assertEquals(JarnTabStrip.LibrarySections, single.strip(top))
        assertEquals(JarnTabStrip.LibrarySections, single.strip(bottom))
        assertEquals(JarnTabStrip.SettingsSections, single.openSettings().strip(bottom))
        assertEquals(JarnShoulderPair.entries, single.shoulderPairsFor(JarnScreen.Stage))
    }

    @Test
    fun `each role reaches its own pair on two screens`() {
        assertEquals(listOf(top), dual.shoulderPairsFor(JarnScreen.Stage))
        assertEquals(listOf(bottom), dual.shoulderPairsFor(JarnScreen.Deck))
        val swapped = dual.withSwapRequested(true)
        assertEquals(listOf(bottom), swapped.shoulderPairsFor(JarnScreen.Stage))
        assertEquals(listOf(top), swapped.shoulderPairsFor(JarnScreen.Deck))
    }

    @Test
    fun `an open modal only blocks reaching the other screen`() {
        assertNull(dual.strip(bottom, modal = true))
        assertEquals(JarnTabStrip.LibrarySections, dual.strip(top, modal = true))
    }

    @Test
    fun `a game action page in use on now blocks reaching stage`() {
        val onNow = dual.copy(activeScreen = JarnScreen.Deck, deckTab = DeckTab.Now)
        assertNull(onNow.tabStripAt(top, false, false, gameActionPageOpen = true))
        assertEquals(JarnTabStrip.DeckTabs, onNow.tabStripAt(bottom, false, false, gameActionPageOpen = true))
        val onApps = onNow.copy(deckTab = DeckTab.Apps)
        assertEquals(JarnTabStrip.LibrarySections, onApps.tabStripAt(top, false, false, gameActionPageOpen = true))
    }

    @Test
    fun `r2 from stage steps the deck and leaves control alone`() {
        pull(KeyEvent.KEYCODE_BUTTON_R2)
        assertEquals(DeckTab.Apps, session.state.deckTab)
        assertEquals(LibrarySection.Systems, session.state.librarySection)
        assertControlOn(JarnScreen.Stage)
        pull(KeyEvent.KEYCODE_BUTTON_L2)
        assertEquals(DeckTab.Now, session.state.deckTab)
    }

    @Test
    fun `r1 from deck steps stage and r2 steps deck`() {
        session.update { activate(JarnScreen.Deck) }
        pull(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(LibrarySection.Favorites, session.state.librarySection)
        assertEquals(DeckTab.Now, session.state.deckTab)
        pull(KeyEvent.KEYCODE_BUTTON_R2)
        assertEquals(DeckTab.Apps, session.state.deckTab)
        assertControlOn(JarnScreen.Deck)
    }

    @Test
    fun `after a swap the shoulders follow the physical screens`() {
        session.update { withSwapRequested(true) }
        pull(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(DeckTab.Apps, session.state.deckTab)
        assertEquals(LibrarySection.Systems, session.state.librarySection)
        pull(KeyEvent.KEYCODE_BUTTON_R2)
        assertEquals(LibrarySection.Favorites, session.state.librarySection)
        assertControlOn(JarnScreen.Stage)
        session.update { activate(JarnScreen.Deck) }
        pull(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(DeckTab.Canvas, session.state.deckTab)
        pull(KeyEvent.KEYCODE_BUTTON_R2)
        assertEquals(LibrarySection.Recent, session.state.librarySection)
        assertControlOn(JarnScreen.Deck)
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
        assertTrue(router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_R1, JarnKeyPhase.Down, 1)))
        assertEquals(DeckTab.Now, session.state.deckTab)
        assertEquals(LibrarySection.Systems, session.state.librarySection)
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
    fun `a layer that claims l1 keeps it`() {
        var shrunk = 0
        val edit = router.createLayer(JarnScreen.Deck, depth = 1)
        edit.update(jarnBindings { on(JarnAction.TabPrevious, R.string.jarnfe_canvas_hint_shrink) { shrunk++ } })
        router.attach(edit)
        session.update { stepLibrarySection(1).activate(JarnScreen.Deck) }
        pull(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals(1, shrunk)
        assertEquals(LibrarySection.Favorites, session.state.librarySection)
    }

    @Test
    fun `r2 steps settings sections on the deck and r1 does nothing`() {
        session.update { openSettings().activate(JarnScreen.Stage) }
        pull(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(SettingsSection.Library, session.state.settingsSection)
        pull(KeyEvent.KEYCODE_BUTTON_R2)
        assertEquals(SettingsSection.Art, session.state.settingsSection)
        assertControlOn(JarnScreen.Stage)
    }
}
