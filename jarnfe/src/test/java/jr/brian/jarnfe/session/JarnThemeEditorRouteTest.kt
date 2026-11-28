package jr.brian.jarnfe.session

import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnThemeEditorRouteTest {
    private val dual = JarnSessionState(deckDisplayAvailable = true, deckHostReady = true)

    @Test
    fun `the editor opens on its canvas, even from the Deck`() {
        val fromSettings = dual.openSettings().openThemeEditor()
        assertEquals(StageRoute.ThemeEditor, fromSettings.stageRoute)
        assertEquals(JarnScreen.Stage, fromSettings.activeScreen)
        assertTrue(fromSettings.editorToolsOnDeck)
    }

    @Test
    fun `leaving the editor returns to the Settings controls`() {
        val back = dual.openSettings().openThemeEditor().back()
        assertEquals(StageRoute.Settings, back.stageRoute)
        assertEquals(JarnScreen.Deck, back.activeScreen)
    }

    @Test
    fun `one screen keeps the tools beside the canvas`() {
        assertFalse(JarnSessionState().openThemeEditor().editorToolsOnDeck)
    }

    @Test
    fun `a restored session never reopens the editor`() {
        val encoded = dual.openSettings().openThemeEditor().encode()
        assertEquals(listOf(StageRoute.Library, StageRoute.Settings), JarnSessionState.decode(encoded)?.stageStack)
    }
}
