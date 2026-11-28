package jr.brian.jarnfe.editor

import androidx.compose.runtime.Composable
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.session.JarnSession

/**
 * The canvas owns every button while it has control, with Canvas's buttons: A picks up and drops,
 * B puts back or leaves, L1/R1 resize, Select flips the axis, Y undoes and Start opens options.
 * Browsing, the D-pad jumps between pieces and L1/R1 step through them; X goes to the tools.
 * While a piece is held the sticks also work: the left one moves it, the right one resizes it.
 */
@Composable
internal fun ThemeEditorCanvasInput(
    editor: ThemeEditorState,
    toolsOnDeck: Boolean,
    paused: Boolean,
    onOptions: () -> Unit,
    onLayerOptions: () -> Unit,
) {
    val canvasActive = !editor.sheetFocused && !paused
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = canvasActive && editor.held == null,
        bindings = {
            browseBindings(toolsOnDeck, onOptions)
            if (editor.selected is ThemeEditorTarget.Layer) {
                on(JarnAction.Context, R.string.jarnfe_editor_layer_options, onPress = onLayerOptions)
            }
        },
    )
    val holding = canvasActive && editor.held != null
    val sticks = rememberThemeEditorStickDriver()
    ThemeEditorStickFrames(holding, sticks)
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = holding,
        onSticks = sticks::onSticks,
        bindings = { heldBindings(editor.axis) },
    )
}

private fun JarnBindingsBuilder.browseBindings(toolsOnDeck: Boolean, onOptions: () -> Unit) {
    on(JarnAction.Confirm, R.string.jarnfe_editor_hint_pick_up, onPress = JarnThemeEditor::pickUp)
    on(JarnAction.MoveUp, R.string.jarnfe_canvas_hint_select) { JarnThemeEditor.update { jumped(0, -1) } }
    on(JarnAction.MoveDown) { JarnThemeEditor.update { jumped(0, 1) } }
    on(JarnAction.MoveLeft) { JarnThemeEditor.update { jumped(-1, 0) } }
    on(JarnAction.MoveRight) { JarnThemeEditor.update { jumped(1, 0) } }
    on(JarnAction.TabPrevious) { JarnThemeEditor.update { withSelectionStep(-1) } }
    on(JarnAction.TabNext) { JarnThemeEditor.update { withSelectionStep(1) } }
    if (JarnThemeEditor.canUndo) on(JarnAction.Search, R.string.jarnfe_canvas_undo, onPress = JarnThemeEditor::undo)
    on(JarnAction.Menu, R.string.jarnfe_hint_context, onPress = onOptions)
    toolsTabBindings()
    on(JarnAction.SwitchScreen, R.string.jarnfe_editor_hint_tools) {
        if (toolsOnDeck) {
            JarnSession.update { switchActiveScreen() }
        } else {
            JarnThemeEditor.update { copy(sheetFocused = true) }
        }
    }
    on(JarnAction.Back, R.string.jarnfe_editor_hint_leave, onPress = JarnThemeEditor::leave)
}

private fun JarnBindingsBuilder.heldBindings(axis: ThemeEditorAxis) {
    nudge(JarnAction.MoveUp, 0, -1, R.string.jarnfe_hint_move)
    nudge(JarnAction.MoveDown, 0, 1)
    nudge(JarnAction.MoveLeft, -1, 0)
    nudge(JarnAction.MoveRight, 1, 0)
    resize(JarnAction.TabPrevious, -1, R.string.jarnfe_canvas_hint_shrink)
    resize(JarnAction.TabNext, 1, R.string.jarnfe_canvas_hint_grow)
    val axisLabel = when (axis) {
        ThemeEditorAxis.Width -> R.string.jarnfe_canvas_hint_axis_width
        ThemeEditorAxis.Height -> R.string.jarnfe_canvas_hint_axis_height
    }
    on(JarnAction.Context, axisLabel) { JarnThemeEditor.update { axisToggled() } }
    on(JarnAction.Confirm, R.string.jarnfe_canvas_hint_drop, onPress = JarnThemeEditor::drop)
    on(JarnAction.Back, R.string.jarnfe_editor_hint_put_back, onPress = JarnThemeEditor::cancel)
}

private fun JarnBindingsBuilder.nudge(action: JarnAction, dx: Int, dy: Int, label: Int? = null) {
    on(action, label, onRepeat = { JarnThemeEditor.nudge(dx, dy, fresh = false) }) {
        JarnThemeEditor.nudge(dx, dy, fresh = true)
    }
}

private fun JarnBindingsBuilder.resize(action: JarnAction, direction: Int, label: Int) {
    on(action, label, repeats = true, onRepeat = { JarnThemeEditor.resize(direction, fresh = false) }) {
        JarnThemeEditor.resize(direction, fresh = true)
    }
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
            if (JarnThemeEditor.canUndo) on(JarnAction.Search, R.string.jarnfe_canvas_undo, onPress = JarnThemeEditor::undo)
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
