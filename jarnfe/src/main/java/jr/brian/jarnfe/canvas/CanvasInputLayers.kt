package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Composable
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import kotlin.math.abs

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
    ownsButtons: Boolean,
    sheetOpen: Boolean,
    pagePush: CanvasPagePush,
    onContext: () -> Unit,
    onOptions: () -> Unit,
    onUndo: (() -> Unit)?,
    onDrop: () -> Unit,
    onUndoAdd: () -> Unit,
    onArrangeMove: (deltaCol: Int, deltaRow: Int) -> Unit,
) {
    val grab = editor.grab
    JarnInputLayer(
        enabled = !editor.editing && !sheetOpen && ownsButtons,
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
            arrangeFocusBindings(hasTiles, onArrangeMove)
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

/** Canvas buttons follow grid focus. A page with nothing to focus still needs its options. */
internal fun canvasOwnsButtons(gridHasFocus: Boolean, hasFocusTarget: Boolean): Boolean =
    gridHasFocus || !hasFocusTarget

/**
 * The next tile that way, or null at the edge so Arrange keeps focus in the grid. A tile sharing the
 * cursor's row or column always beats a closer diagonal one.
 */
internal fun canvasFocusStep(
    placements: Map<String, GridRect>,
    fromId: String?,
    deltaCol: Int,
    deltaRow: Int,
): String? {
    val from = placements[fromId] ?: return null
    return placements.entries
        .filter { (id, rect) -> id != fromId && rect.liesPast(from, deltaCol, deltaRow) }
        .minWithOrNull(
            compareBy(
                { (_, rect) -> !rect.sharesLane(from, deltaCol) },
                { (_, rect) -> rect.gapFrom(from, deltaCol, deltaRow) },
                { (_, rect) -> rect.laneOffset(from, deltaCol) },
            ),
        )
        ?.key
}

private fun GridRect.liesPast(from: GridRect, deltaCol: Int, deltaRow: Int): Boolean = when {
    deltaCol > 0 -> col >= from.right
    deltaCol < 0 -> right <= from.col
    deltaRow > 0 -> row >= from.bottom
    else -> bottom <= from.row
}

private fun GridRect.sharesLane(from: GridRect, deltaCol: Int): Boolean =
    if (deltaCol != 0) row < from.bottom && bottom > from.row else col < from.right && right > from.col

private fun GridRect.gapFrom(from: GridRect, deltaCol: Int, deltaRow: Int): Int = when {
    deltaCol > 0 -> col - from.right
    deltaCol < 0 -> from.col - right
    deltaRow > 0 -> row - from.bottom
    else -> from.row - bottom
}

private fun GridRect.laneOffset(from: GridRect, deltaCol: Int): Int =
    if (deltaCol != 0) abs(row - from.row) else abs(col - from.col)

private fun JarnBindingsBuilder.arrangeFocusBindings(hasTiles: Boolean, onMove: (Int, Int) -> Unit) {
    val label = R.string.jarnfe_canvas_hint_choose.takeIf { hasTiles }
    on(JarnAction.MoveUp, label) { onMove(0, -1) }
    on(JarnAction.MoveDown) { onMove(0, 1) }
    on(JarnAction.MoveLeft) { onMove(-1, 0) }
    on(JarnAction.MoveRight) { onMove(1, 0) }
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
