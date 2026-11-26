package jr.brian.jarnfe.shell

import androidx.compose.runtime.Composable
import jr.brian.core.ui.components.JarnTabReorder
import jr.brian.core.ui.components.rememberJarnTabReorder
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode

/** A strip's reorder that hands [onSave] its tabs in their new order. */
@Composable
internal fun <T> rememberStripReorder(tabs: List<T>, onSave: (List<T>) -> Unit): JarnTabReorder =
    rememberJarnTabReorder { order -> onSave(order.map(tabs::get)) }

/**
 * While a tab is lifted, the strip owns every button: the D-pad moves it, A saves, B cancels, and
 * the shoulders, Up and Down do nothing, so focus never leaves the strip mid-move.
 */
@Composable
internal fun JarnTabReorderInput(reorder: JarnTabReorder) {
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = reorder.isEditing,
        bindings = { tabReorderBindings(reorder) },
    )
}

internal fun JarnBindingsBuilder.tabReorderBindings(reorder: JarnTabReorder) {
    on(JarnAction.MoveLeft, R.string.jarnfe_hint_move) { reorder.move(-1) }
    on(JarnAction.MoveRight, R.string.jarnfe_hint_move) { reorder.move(1) }
    on(JarnAction.Confirm, R.string.jarnfe_hint_done, onPress = reorder::commit)
    on(JarnAction.Back, R.string.jarnfe_hint_cancel, onPress = reorder::cancel)
}
