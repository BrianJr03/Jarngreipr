package jr.brian.jarnfe.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.StageRoute

/**
 * The one theme editing session, shared by the canvas on Stage and the tools on the Deck. It
 * outlives the editor route while the player tries the theme, and ends when they leave.
 */
internal object JarnThemeEditor {
    var state by mutableStateOf<ThemeEditorState?>(null)
        private set

    fun open(themeId: String, spec: JarnThemeSpec) {
        state = ThemeEditorState(themeId, spec.withBothViews(LibraryBuiltInThemes.Grid.spec))
        JarnSession.update { openThemeEditor() }
    }

    fun update(transform: ThemeEditorState.() -> ThemeEditorState) {
        state = state?.transform()
    }

    /** Back on the canvas, or the tools: the editor closes and the route underneath returns. */
    fun leave() {
        state = null
        if (JarnSession.state.stageRoute == StageRoute.ThemeEditor) JarnSession.update { back() }
    }
}
