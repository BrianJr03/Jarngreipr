package jr.brian.jarnfe.deck

import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckMarkSlotTest {
    private val dual = JarnSessionState().withDeckDisplay(true).withDeckHost(true)

    @Test
    fun `the deck tabs carry the label slot only with the jarn label cue`() {
        assertTrue(deckShowsMark(dual, follows = true))
        assertFalse(deckShowsMark(dual, follows = false))
    }

    @Test
    fun `settings on the deck hides the label even while the deck has control`() {
        val settings = dual.openSettings()

        assertEquals(JarnScreen.Deck, settings.activeScreen)
        assertFalse(deckShowsMark(settings, follows = true))
    }

    @Test
    fun `leaving settings for the deck tabs brings the slot back`() {
        val back = dual.activate(JarnScreen.Deck).openSettings().back()

        assertEquals(JarnScreen.Deck, back.activeScreen)
        assertTrue(deckShowsMark(back, follows = true))
    }
}
