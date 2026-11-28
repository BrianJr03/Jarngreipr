package jr.brian.jarnfe.editor

import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.StageRoute
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeEditorTryItTest {
    @After
    fun tearDown() {
        if (ThemeEditorTryIt.active) ThemeEditorTryIt.backToEditing()
        JarnThemeEditor.leave()
        JarnSession.update { openRoute(StageRoute.Library) }
    }

    @Test
    fun `trying browses the library, blocks games and returns to the editor`() {
        JarnSession.update { openSettings() }
        JarnThemeEditor.open("mine", editorWith().spec)
        ThemeEditorTryIt.start()
        assertTrue(ThemeEditorTryIt.active)
        assertEquals(R.string.jarnfe_editor_try_strip, ThemeEditorTryIt.notice?.messageRes)
        assertEquals(StageRoute.Library, JarnSession.state.stageRoute)

        val launches = JarnLibrary.launches
        JarnLibrary.play(JarnLibraryGame(id = "g", systemId = "s", title = "Game"))
        assertEquals(launches, JarnLibrary.launches)
        assertEquals(R.string.jarnfe_editor_try_no_games, ThemeEditorTryIt.notice?.messageRes)

        ThemeEditorTryIt.backToEditing()
        assertFalse(ThemeEditorTryIt.active)
        assertEquals(listOf(StageRoute.Library, StageRoute.Settings, StageRoute.ThemeEditor), JarnSession.state.stageStack)
        assertNotNull(JarnThemeEditor.state)
    }
}
