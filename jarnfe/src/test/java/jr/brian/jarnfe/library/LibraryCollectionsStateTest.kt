package jr.brian.jarnfe.library

import android.view.KeyEvent
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnShoulderPair
import jr.brian.jarnfe.input.jarnBindings
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.shell.JarnTabStrip
import jr.brian.jarnfe.shell.gameOptionsBinding
import jr.brian.jarnfe.shell.tabStripAt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCollectionsStateTest {
    private val racing = JarnCollection(id = 1, name = "Racing", gameCount = 2)
    private val party = JarnCollection(id = 2, name = "Party", gameCount = 0)
    private val kart = collectionGame("kart")
    private val wave = collectionGame("wave")

    private val state = JarnLibraryState(
        collections = JarnLibraryFeed.ready(listOf(racing, party)),
        gamesByCollection = mapOf(1L to JarnLibraryFeed.ready(listOf(kart, wave))),
    )

    @After
    fun tearDown() = NowVideoVisibility.report(false)

    @Test
    fun `the Collections tab selects the focused collection, the first by default`() {
        assertEquals(
            LibrarySelection.Collection(racing),
            state.selection(StageRoute.Library, LibrarySection.Collections),
        )
        assertEquals(
            LibrarySelection.Collection(party),
            state.focusCollection(2).selection(StageRoute.Library, LibrarySection.Collections),
        )
        assertEquals(LibraryConfirm.Open, state.confirmFor(StageRoute.Library, LibrarySection.Collections, canPlay = true))
    }

    @Test
    fun `an open collection selects its focused game and A plays it`() {
        val open = state.openCollection(1).focusCollectionGame("wave")
        assertEquals(LibrarySelection.Game(wave), open.selection(StageRoute.Collection, LibrarySection.Collections))
        assertEquals(LibraryConfirm.Play, open.confirmFor(StageRoute.Collection, LibrarySection.Collections, canPlay = true))
        assertEquals(racing.id, open.focusedCollectionId)
    }

    @Test
    fun `an empty open collection selects the collection itself`() {
        val open = state.openCollection(2)
        assertEquals(LibrarySelection.Collection(party), open.selection(StageRoute.Collection, LibrarySection.Collections))
    }

    @Test
    fun `a collection list loses its route once the collection is gone`() {
        assertTrue(state.gameListIsOrphaned(StageRoute.Collection))
        assertFalse(state.openCollection(1).gameListIsOrphaned(StageRoute.Collection))
        val gone = state.openCollection(1).copy(collections = JarnLibraryFeed.ready(listOf(party)))
        assertTrue(gone.gameListIsOrphaned(StageRoute.Collection))
    }

    @Test
    fun `shoulders in a collection step collections and stop at the ends`() {
        val session = JarnSessionState().openRoute(StageRoute.Collection)
        assertEquals(JarnTabStrip.CollectionLists, session.tabStripAt(JarnShoulderPair.Top, false, false))
        val open = state.openCollection(1)
        assertEquals(2L, open.stepOpenCollection(1).openCollectionId)
        assertEquals(2L, open.stepOpenCollection(1).stepOpenCollection(1).openCollectionId)
        assertEquals(1L, open.stepOpenCollection(-1).openCollectionId)
    }

    @Test
    fun `tab stepping reaches Collections after Recent and stops there`() {
        val recent = JarnSessionState(librarySection = LibrarySection.Recent)
        assertEquals(LibrarySection.Collections, recent.stepLibrarySection(1).librarySection)
        assertEquals(LibrarySection.Collections, recent.stepLibrarySection(1).stepLibrarySection(1).librarySection)
    }

    @Test
    fun `B from a collection list returns to the Collections tab`() {
        val inside = JarnSessionState(librarySection = LibrarySection.Collections).openRoute(StageRoute.Collection)
        val back = inside.back()
        assertEquals(StageRoute.Library, back.stageRoute)
        assertEquals(LibrarySection.Collections, back.librarySection)
    }

    @Test
    fun `hiding a game moves focus in its collection too`() {
        val open = state.openCollection(1).focusCollectionGame("kart")
        assertEquals("wave", open.focusAfterHiding("kart").focusedGameByCollection[1L])
    }

    @Test
    fun `a session saved on the Collections tab restores there`() {
        val saved = JarnSessionState(librarySection = LibrarySection.Collections).encode()
        assertEquals(LibrarySection.Collections, JarnSessionState.decode(saved)?.librarySection)
    }

    @Test
    fun `Select on a game in a collection still opens Options`() {
        val router = JarnInputRouter()
        val selection = state.openCollection(1).selection(StageRoute.Collection, LibrarySection.Collections)
        var options = 0
        router.attach(
            router.createLayer(JarnScreen.Stage).apply {
                update(jarnBindings { gameOptionsBinding(selection, hasActions = true) { options++ } })
            },
        )
        router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_SELECT, JarnKeyPhase.Down, 0))
        router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_SELECT, JarnKeyPhase.Up, 10))
        assertEquals(1, options)
    }

    @Test
    fun `Select on a collection tile binds nothing yet`() {
        val bindings = jarnBindings {
            gameOptionsBinding(state.selection(StageRoute.Library, LibrarySection.Collections), hasActions = true) {}
        }
        assertTrue(bindings.labels.isEmpty())
    }
}
