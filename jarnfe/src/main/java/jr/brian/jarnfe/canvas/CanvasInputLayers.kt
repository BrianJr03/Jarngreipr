package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Composable
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode

/**
 * [onPageLeft] and [onPageRight] exist only while the cursor is at that edge, so the router binds
 * them per press. A held direction that reaches the edge stays with focus and can't flip the page.
 */
internal class CanvasPagePush(
    val onPageLeft: (() -> Unit)?,
    val onPageRight: (() -> Unit)?,
    val onCarry: (Int) -> Unit,
)

@Composable
internal fun CanvasInputLayers(
    editor: CanvasEditor,
    hasTiles: Boolean,
    confirmLabel: Int?,
    sheetOpen: Boolean,
    pagePush: CanvasPagePush,
    onContext: () -> Unit,
    onOptions: () -> Unit,
    onUndo: (() -> Unit)?,
    onDrop: () -> Unit,
    onUndoAdd: () -> Unit,
) {
    val grab = editor.grab
    JarnInputLayer(
        enabled = !editor.editing && !sheetOpen,
        bindings = {
            if (hasTiles && confirmLabel != null) hint(JarnAction.Confirm, confirmLabel)
            on(JarnAction.Context, R.string.jarnfe_hint_context, onPress = onContext)
            on(JarnAction.Menu, R.string.jarnfe_canvas_options_title, onPress = onOptions)
            onUndo?.let { on(JarnAction.Search, R.string.jarnfe_canvas_undo, onPress = it) }
            pagePush.onPageLeft?.let { on(JarnAction.MoveLeft, repeats = false, onPress = it) }
            pagePush.onPageRight?.let { on(JarnAction.MoveRight, repeats = false, onPress = it) }
        },
    )
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        enabled = editor.editing && grab == null && !sheetOpen,
        bindings = {
            if (hasTiles) hint(JarnAction.Confirm, R.string.jarnfe_canvas_hint_grab)
            on(JarnAction.Menu, R.string.jarnfe_canvas_options_title, onPress = onOptions)
            on(JarnAction.Back, R.string.jarnfe_canvas_hint_done, onPress = editor::exit)
            onUndo?.let { on(JarnAction.Search, R.string.jarnfe_canvas_undo, onPress = it) }
        },
    )
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = grab != null,
        bindings = {
            if (grab?.driver == CanvasGrabDriver.Touch) {
                grabCancelBinding(editor, grab.isNewTile, onUndoAdd)
            } else {
                grabBindings(
                    editor = editor,
                    axis = grab?.axis ?: CanvasResizeAxis.Width,
                    isNewTile = grab?.isNewTile == true,
                    onCarry = pagePush.onCarry,
                    onDrop = onDrop,
                    onUndoAdd = onUndoAdd,
                )
            }
        },
    )
}

/** A fresh press that can't move the tile any further that way carries it to the next page. */
internal fun CanvasEditor.pushGrabSideways(direction: Int, onCarry: (Int) -> Unit) {
    val before = grab?.rect ?: return
    step { moved(direction, 0) }
    if (grab?.rect == before) onCarry(direction)
}

private fun JarnBindingsBuilder.grabBindings(
    editor: CanvasEditor,
    axis: CanvasResizeAxis,
    isNewTile: Boolean,
    onCarry: (Int) -> Unit,
    onDrop: () -> Unit,
    onUndoAdd: () -> Unit,
) {
    on(JarnAction.MoveUp, R.string.jarnfe_hint_move) { editor.step { moved(0, -1) } }
    on(JarnAction.MoveDown) { editor.step { moved(0, 1) } }
    on(JarnAction.MoveLeft, onRepeat = { editor.step { moved(-1, 0) } }) {
        editor.pushGrabSideways(-1, onCarry)
    }
    on(JarnAction.MoveRight, onRepeat = { editor.step { moved(1, 0) } }) {
        editor.pushGrabSideways(1, onCarry)
    }
    on(JarnAction.TabPrevious, R.string.jarnfe_canvas_hint_shrink) { editor.step { resized(-1) } }
    on(JarnAction.TabNext, R.string.jarnfe_canvas_hint_grow) { editor.step { resized(1) } }
    val axisLabel = when (axis) {
        CanvasResizeAxis.Width -> R.string.jarnfe_canvas_hint_axis_width
        CanvasResizeAxis.Height -> R.string.jarnfe_canvas_hint_axis_height
    }
    on(JarnAction.Context, axisLabel) { editor.step { axisToggled() } }
    on(JarnAction.Confirm, R.string.jarnfe_canvas_hint_drop, onPress = onDrop)
    grabCancelBinding(editor, isNewTile, onUndoAdd)
}

private fun JarnBindingsBuilder.grabCancelBinding(
    editor: CanvasEditor,
    isNewTile: Boolean,
    onUndoAdd: () -> Unit,
) {
    if (isNewTile) {
        on(JarnAction.Back, R.string.jarnfe_canvas_hint_undo_add, onPress = onUndoAdd)
    } else {
        on(JarnAction.Back, R.string.jarnfe_canvas_hint_cancel, onPress = editor::cancelGrab)
    }
}
