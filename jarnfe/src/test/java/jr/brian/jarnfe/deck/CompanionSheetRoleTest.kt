package jr.brian.jarnfe.deck

import jr.brian.jarnfe.input.JarnScreen.Deck
import jr.brian.jarnfe.input.JarnScreen.Stage
import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionSheetRoleTest {
    private val game = NowPlayingGame("gc", "/roms/gc/Melee.rvz", "Melee")
    private val onTop = NowPlayingSession(game, launchedAtUptimeMs = 0L, startedAtWallMs = 0L, gameHost = Stage)

    @Test
    fun `swapped, the sheet shows in the Stage role, on the bottom screen beside the game`() {
        assertEquals(Stage, companionSheetRole(onTop, deckDisplayAvailable = true, rolesSwapped = true))
    }

    @Test
    fun `unswapped, the sheet stays on the Deck`() {
        assertEquals(Deck, companionSheetRole(onTop, deckDisplayAvailable = true, rolesSwapped = false))
    }

    @Test
    fun `with no game covering a window, or on one screen, the sheet stays on the Deck`() {
        assertEquals(Deck, companionSheetRole(null, deckDisplayAvailable = true, rolesSwapped = true))
        assertEquals(Deck, companionSheetRole(onTop.copy(gameHost = null), deckDisplayAvailable = true, rolesSwapped = true))
        assertEquals(Deck, companionSheetRole(onTop, deckDisplayAvailable = false, rolesSwapped = true))
    }
}
