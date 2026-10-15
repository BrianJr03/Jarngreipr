package jr.brian.frontend.ui.input

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Compose modifier that intercepts gamepad / keyboard key-ups and
 * dispatches them as semantic [GamepadAction]s. B = back is wired
 * everywhere by default per plan §3.2 — screens pass only the
 * actions they handle, [onAction] returns true to consume.
 *
 * Attach to the root composable of every screen so a focused child
 * doesn't need to know. The modifier makes itself focusable so key
 * events route through Compose's focus system rather than falling
 * through to the Activity.
 */
@Composable
fun Modifier.handleGamepad(onAction: (GamepadAction) -> Boolean): Modifier {
    val requester = remember { FocusRequester() }
    return this
        .focusRequester(requester)
        .focusable()
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
            val action = GamepadKeyRouter.actionFor(event.nativeKeyEvent.keyCode)
                ?: return@onKeyEvent false
            onAction(action)
        }
}

/**
 * Pure helper so a direct KeyEvent dispatch (from tests, or an
 * Activity dispatchKeyEvent override that wants a last-chance back
 * handler) can route through the same translation.
 */
fun routeKeyEvent(event: KeyEvent, onAction: (GamepadAction) -> Boolean): Boolean {
    if (event.action != KeyEvent.ACTION_UP) return false
    val action = GamepadKeyRouter.actionFor(event.keyCode) ?: return false
    return onAction(action)
}
