package jr.brian.jarnfe.editor

import androidx.compose.runtime.Composable
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.session.JarnSession

/**
 * The canvas owns every button while it has control. X goes to the tools: the Deck when it has a
 * window, else the sheet beside the canvas.
 */
@Composable
internal fun ThemeEditorCanvasInput(editor: ThemeEditorState, toolsOnDeck: Boolean) {
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = !editor.sheetFocused,
        bindings = {
            on(JarnAction.TabPrevious, R.string.jarnfe_editor_hint_select) { JarnThemeEditor.update { withSelectionStep(-1) } }
            on(JarnAction.TabNext) { JarnThemeEditor.update { withSelectionStep(1) } }
            toolsTabBindings()
            on(JarnAction.SwitchScreen, R.string.jarnfe_editor_hint_tools) {
                if (toolsOnDeck) {
                    JarnSession.update { switchActiveScreen() }
                } else {
                    JarnThemeEditor.update { copy(sheetFocused = true) }
                }
            }
            on(JarnAction.Back, R.string.jarnfe_editor_hint_leave, onPress = JarnThemeEditor::leave)
        },
    )
}

/**
 * The tools leave D-pad and A to their rows. [onCanvas] is the sheet's way back to the canvas;
 * on the Deck, X is the shell's screen switch.
 */
@Composable
internal fun ThemeEditorToolsInput(editor: ThemeEditorState, onCanvas: (() -> Unit)?) {
    JarnInputLayer(
        mode = if (onCanvas != null) JarnLayerMode.Modal else JarnLayerMode.Passive,
        enabled = onCanvas == null || editor.sheetFocused,
        bindings = {
            toolsTabBindings()
            if (onCanvas != null) {
                on(JarnAction.SwitchScreen, R.string.jarnfe_editor_hint_canvas, onPress = onCanvas)
                on(JarnAction.Back, R.string.jarnfe_editor_hint_canvas, onPress = onCanvas)
            } else {
                on(JarnAction.Back, R.string.jarnfe_editor_hint_leave, onPress = JarnThemeEditor::leave)
            }
        },
    )
}

private fun JarnBindingsBuilder.toolsTabBindings() {
    on(JarnAction.BottomTabPrevious) { JarnThemeEditor.update { withTab(-1) } }
    on(JarnAction.BottomTabNext) { JarnThemeEditor.update { withTab(1) } }
}
