package jr.brian.jarnfe.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnDeckTabMemoryTest {
    @Test
    fun `a new process starts on the remembered tab`() {
        val store = JarnSessionStore()
        store.restoreIfFresh(null, DeckTab.Canvas)
        assertEquals(DeckTab.Canvas, store.state.deckTab)
    }

    @Test
    fun `saved window state wins over the remembered tab`() {
        val store = JarnSessionStore()
        val saved = JarnSessionState(deckTab = DeckTab.Apps).encode()
        store.restoreIfFresh(saved, DeckTab.Canvas)
        assertEquals(DeckTab.Apps, store.state.deckTab)
    }

    @Test
    fun `a live session is never moved`() {
        val store = JarnSessionStore()
        store.update { copy(deckTab = DeckTab.Apps) }
        store.restoreIfFresh(null, DeckTab.Canvas)
        assertEquals(DeckTab.Apps, store.state.deckTab)
    }

    @Test
    fun `an unknown saved name is ignored`() = assertNull(deckTabFromName("Old"))
}
