package jr.brian.jarnfe.shell

import android.view.KeyEvent
import jr.brian.core.ui.components.JarnKey
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.jarnBindings
import jr.brian.jarnfe.library.JarnLibraryFeed
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JarnShellInputTest {
    private val router = JarnInputRouter()
    private val session = JarnSessionStore(router::activate)
    private val library = JarnLibraryStore().apply {
        update { copy(systems = JarnLibraryFeed.ready(emptyList())) }
    }
    private val stageLayer = router.createLayer(JarnScreen.Stage)
    private val deckLayer = router.createLayer(JarnScreen.Deck)
    private val panelLayer = router.createLayer(JarnScreen.Stage, JarnLayerMode.Modal, depth = 1)
    private var clock = 0L

    @Before
    fun setUp() {
        router.setGlobalBindings(jarnGlobalBindings(session))
        router.attach(stageLayer)
        router.attach(deckLayer)
        session.update { withDeckDisplay(true) }
        refreshLayers()
    }

    private fun refreshLayers() {
        val state = session.state
        stageLayer.update(jarnBindings { stageBindings(state, session, library) })
        deckLayer.update(jarnBindings { deckBindings(state, session) })
        panelLayer.update(jarnBindings { deckPanelBindings(session) })
    }

    private fun press(keyCode: Int) {
        router.dispatch(JarnKeyInput(keyCode, JarnKeyPhase.Down, clock++))
        router.dispatch(JarnKeyInput(keyCode, JarnKeyPhase.Up, clock++))
        refreshLayers()
    }

    private fun hintKeys(screen: JarnScreen) = router.hintsFor(screen).map { it.key }

    @Test
    fun `b at the root does nothing and is consumed`() {
        val before: JarnSessionState = session.state
        assertTrue(router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_B, JarnKeyPhase.Down, 0)))
        assertTrue(router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_B, JarnKeyPhase.Up, 1)))
        assertEquals(before, session.state)
    }

    @Test
    fun `one back press pops exactly one layer`() {
        session.update { openRoute(StageRoute.GameList).openRoute(StageRoute.Settings) }
        refreshLayers()
        press(KeyEvent.KEYCODE_BACK)
        assertEquals(StageRoute.GameList, session.state.stageRoute)
    }

    @Test
    fun `system back during a key press does not double pop`() {
        session.update { openRoute(StageRoute.GameList).openRoute(StageRoute.Settings) }
        refreshLayers()
        router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BACK, JarnKeyPhase.Down, 0))
        router.dispatchSystemBack()
        router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BACK, JarnKeyPhase.Up, 1))
        assertEquals(StageRoute.GameList, session.state.stageRoute)
    }

    @Test
    fun `select switches the router and the session together`() {
        press(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals(JarnScreen.Deck, session.state.activeScreen)
        assertEquals(JarnScreen.Deck, router.activeScreen)
        press(KeyEvent.KEYCODE_BACK)
        assertEquals(JarnScreen.Stage, session.state.activeScreen)
        assertEquals(JarnScreen.Stage, router.activeScreen)
    }

    @Test
    fun `shoulders change tabs only on the active screen`() {
        press(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(LibrarySection.Favorites, session.state.librarySection)
        assertEquals(DeckTab.Now, session.state.deckTab)
        press(KeyEvent.KEYCODE_BUTTON_SELECT)
        press(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(DeckTab.Browse, session.state.deckTab)
        assertEquals(LibrarySection.Favorites, session.state.librarySection)
    }

    @Test
    fun `shoulders stop at the first and last tab`() {
        press(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals(LibrarySection.Systems, session.state.librarySection)
        repeat(5) { press(KeyEvent.KEYCODE_BUTTON_R1) }
        assertEquals(LibrarySection.Recent, session.state.librarySection)
    }

    @Test
    fun `shoulders are inert on a game list without data`() {
        session.update { openRoute(StageRoute.GameList) }
        refreshLayers()
        press(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(LibrarySection.Systems, session.state.librarySection)
        assertEquals(StageRoute.GameList, session.state.stageRoute)
    }

    @Test
    fun `shoulders step systems in a game list and stop at the ends`() {
        library.update {
            copy(systems = JarnLibraryFeed.ready(listOf(system("a"), system("b")))).openSystem("a")
        }
        session.update { openRoute(StageRoute.GameList) }
        refreshLayers()
        press(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals("b", library.state.openSystemId)
        press(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals("b", library.state.openSystemId)
        press(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals("a", library.state.openSystemId)
        assertEquals(LibrarySection.Systems, session.state.librarySection)
    }

    @Test
    fun `a game shows a play hint only once launching is connected`() {
        val game = JarnLibraryGame("g", "a", "Game")
        library.update {
            copy(
                systems = JarnLibraryFeed.ready(listOf(system("a"))),
                gamesBySystem = mapOf("a" to JarnLibraryFeed.ready(listOf(game))),
            ).openSystem("a")
        }
        session.update { openRoute(StageRoute.GameList) }
        refreshLayers()
        assertFalse(JarnKey.A in hintKeys(JarnScreen.Stage))
        library.onPlay = {}
        refreshLayers()
        assertTrue(JarnKey.A in hintKeys(JarnScreen.Stage))
    }

    private fun system(id: String) = JarnLibrarySystem(id, id, gameCount = 1)

    @Test
    fun `hints only declare working actions`() {
        assertEquals(listOf(JarnKey.A, JarnKey.Start, JarnKey.Select), hintKeys(JarnScreen.Stage))
        assertEquals(listOf(JarnKey.Select), hintKeys(JarnScreen.Deck))
        session.update { openRoute(StageRoute.Settings) }
        refreshLayers()
        assertEquals(listOf(JarnKey.A, JarnKey.B, JarnKey.Select), hintKeys(JarnScreen.Stage))
    }

    @Test
    fun `start opens settings from the library and back returns`() {
        press(KeyEvent.KEYCODE_BUTTON_START)
        assertEquals(StageRoute.Settings, session.state.stageRoute)
        press(KeyEvent.KEYCODE_BACK)
        assertEquals(StageRoute.Library, session.state.stageRoute)
    }

    @Test
    fun `single screen panel owns input and closes one layer at a time`() {
        session.update { withDeckDisplay(false).openRoute(StageRoute.GameList) }
        press(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertTrue(session.state.deckPanelOpen)
        assertEquals(JarnScreen.Stage, router.activeScreen)
        router.attach(panelLayer)
        press(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(DeckTab.Browse, session.state.deckTab)
        press(KeyEvent.KEYCODE_BACK)
        assertFalse(session.state.deckPanelOpen)
        assertEquals(StageRoute.GameList, session.state.stageRoute)
    }
}
