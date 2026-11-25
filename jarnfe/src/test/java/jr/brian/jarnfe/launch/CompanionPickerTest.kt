package jr.brian.jarnfe.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPickerTest {
    private val two = listOf(JarnAppOption("a.maps", "Maps"), JarnAppOption("b.wiki", "Wiki"))

    @Test
    fun `the picker shows for an offer whatever Now is showing`() {
        assertTrue(companionPickerShown(GameActionsPage.CompanionLaunch, two, hasController = true))
    }

    @Test
    fun `the picker stays hidden without an offer`() {
        assertFalse(companionPickerShown(GameActionsPage.Actions, two, hasController = true))
        assertFalse(companionPickerShown(GameActionsPage.Companions, two, hasController = true))
        assertFalse(companionPickerShown(GameActionsPage.CompanionLaunch, emptyList(), hasController = true))
    }

    @Test
    fun `the picker stays hidden when launching is not connected`() {
        assertFalse(companionPickerShown(GameActionsPage.CompanionLaunch, two, hasController = false))
    }

    @Test
    fun `a game page never draws the offer, so it is drawn once`() {
        assertFalse(GameActionsPage.CompanionLaunch.showsOnGamePage())
        assertFalse(GameActionsPage.Actions.showsOnGamePage())
        GameActionsPage.entries
            .filter { it != GameActionsPage.Actions && it != GameActionsPage.CompanionLaunch }
            .forEach { assertTrue(it.name, it.showsOnGamePage()) }
    }

    @Test
    fun `the offer rides in the Deck panel on one screen and the Deck window on two`() {
        assertEquals(CompanionPickerPlace.DeckWindow, companionPickerPlace(deckDisplayAvailable = true))
        assertEquals(CompanionPickerPlace.DeckPanel, companionPickerPlace(deckDisplayAvailable = false))
    }
}
