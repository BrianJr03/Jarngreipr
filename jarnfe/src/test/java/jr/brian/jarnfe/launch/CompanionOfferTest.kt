package jr.brian.jarnfe.launch

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompanionOfferTest {
    private val state = JarnGameActions
    private val two = listOf(JarnAppOption("a.maps", "Maps"), JarnAppOption("b.wiki", "Wiki"))

    @Before
    fun reset() {
        state.endOffer()
        state.page = GameActionsPage.Actions
        state.gameId = null
    }

    @After
    fun tearDown() = reset()

    @Test
    fun `an offer without a Library game shows the picker and leaves the game alone`() {
        state.gameId = "library-game"
        state.offer(two)
        assertTrue(companionPickerShown(state.page, state.companionChoices, hasController = true))
        assertEquals("library-game", state.gameId)
    }

    @Test
    fun `closing the offer brings back the page that was open under it`() {
        state.page = GameActionsPage.Emulators
        state.offer(two)
        assertTrue(state.endOffer())
        assertEquals(GameActionsPage.Emulators, state.page)
        assertTrue(state.companionChoices.isEmpty())
    }

    @Test
    fun `a second offer still returns to the page under the first`() {
        state.page = GameActionsPage.Edit
        state.offer(two)
        state.offer(two.take(1))
        state.endOffer()
        assertEquals(GameActionsPage.Edit, state.page)
    }

    @Test
    fun `closing with nothing offered changes nothing`() {
        state.page = GameActionsPage.Cores
        assertFalse(state.endOffer())
        assertEquals(GameActionsPage.Cores, state.page)
    }
}
