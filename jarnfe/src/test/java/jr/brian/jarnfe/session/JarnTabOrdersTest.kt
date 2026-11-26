package jr.brian.jarnfe.session

import android.view.KeyEvent
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.components.JarnTabReorder
import jr.brian.core.ui.components.jarnMovedOrder
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnInputHint
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.jarnBindings
import jr.brian.jarnfe.library.JarnLibraryFeed
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.search.JarnSearchStore
import jr.brian.jarnfe.shell.jarnGlobalBindings
import jr.brian.jarnfe.shell.stepScreenTabs
import jr.brian.jarnfe.shell.tabReorderBindings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnTabOrdersTest {
    private var clock = 0L

    @After
    fun tearDown() = JarnTabOrders.reset()

    private fun JarnInputRouter.tap(code: Int) {
        dispatch(JarnKeyInput(code, JarnKeyPhase.Down, clock++))
        dispatch(JarnKeyInput(code, JarnKeyPhase.Up, clock++))
    }

    @Test
    fun `no saved order keeps today's order`() {
        assertEquals(LibrarySection.entries, librarySectionOrder(emptyList()))
        assertEquals(DeckTab.entries, deckTabOrder(emptyList()))
    }

    @Test
    fun `a saved order drops unknown tabs and adds missing ones at the end`() {
        assertEquals(
            listOf(
                LibrarySection.Recent,
                LibrarySection.Systems,
                LibrarySection.AllGames,
                LibrarySection.Favorites,
                LibrarySection.Collections,
            ),
            librarySectionOrder(listOf("Recent", "Gone", "Recent")),
        )
        assertEquals(listOf(DeckTab.Canvas, DeckTab.Now, DeckTab.Apps), deckTabOrder(listOf("Canvas", "Now")))
    }

    @Test
    fun `stepping follows the player's order and stops at its ends`() {
        val order = listOf(LibrarySection.Recent, LibrarySection.Systems, LibrarySection.Collections)
        val state = JarnSessionState(librarySection = LibrarySection.Systems)
        assertEquals(LibrarySection.Collections, state.stepLibrarySection(1, order).librarySection)
        assertEquals(LibrarySection.Recent, state.stepLibrarySection(-1, order).librarySection)
        assertEquals(LibrarySection.Recent, state.stepLibrarySection(-1, order).stepLibrarySection(-1, order).librarySection)
        val deck = listOf(DeckTab.Canvas, DeckTab.Now, DeckTab.Apps)
        assertEquals(DeckTab.Canvas, JarnSessionState(deckTab = DeckTab.Now).stepDeckTab(-1, deck).deckTab)
    }

    @Test
    fun `L1 and R1 on the Library follow the saved order`() {
        val router = JarnInputRouter()
        val session = JarnSessionStore(router::activate)
        val library = JarnLibraryStore().apply { update { copy(systems = JarnLibraryFeed.ready(emptyList())) } }
        val search = JarnSearchStore()
        router.setGlobalBindings(
            jarnGlobalBindings(session, onScreenTab = { pair, delta -> stepScreenTabs(pair, delta, session, library, search, router) }),
        )
        router.shellTabTarget = { null }
        session.update { withDeckDisplay(true) }
        JarnTabOrders.sync(JarnDisplayPrefs(librarySectionOrder = listOf("Collections", "Systems", "Recent")))

        router.tap(KeyEvent.KEYCODE_BUTTON_R1)
        assertEquals(LibrarySection.Recent, session.state.librarySection)
        router.tap(KeyEvent.KEYCODE_BUTTON_L1)
        router.tap(KeyEvent.KEYCODE_BUTTON_L1)
        assertEquals(LibrarySection.Collections, session.state.librarySection)
    }

    @Test
    fun `while a tab is lifted the D-pad moves it, A saves, B cancels and the bar says so`() {
        val router = JarnInputRouter()
        var saved: List<Int>? = null
        val reorder = JarnTabReorder { saved = it }
        val layer = router.createLayer(JarnScreen.Stage, JarnLayerMode.Capture).apply {
            update(jarnBindings { tabReorderBindings(reorder) })
        }
        router.attach(layer)
        reorder.begin(0, 3)

        router.tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(jarnMovedOrder(listOf(0, 1, 2), 0, 1), reorder.order)
        val hints = router.hintsFor(JarnScreen.Stage)
        assertTrue(JarnInputHint(JarnKey.Dpad, R.string.jarnfe_hint_move) in hints)
        assertTrue(JarnInputHint(JarnKey.A, R.string.jarnfe_hint_done) in hints)
        assertTrue(JarnInputHint(JarnKey.B, R.string.jarnfe_hint_cancel) in hints)
        router.tap(KeyEvent.KEYCODE_BUTTON_A)
        assertEquals(listOf(1, 0, 2), saved)

        saved = null
        reorder.begin(2, 3)
        router.tap(KeyEvent.KEYCODE_DPAD_LEFT)
        router.tap(KeyEvent.KEYCODE_BUTTON_B)
        assertEquals(null, saved)
        assertEquals(false, reorder.isEditing)
    }
}
