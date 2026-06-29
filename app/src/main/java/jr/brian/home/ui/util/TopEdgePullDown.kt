package jr.brian.home.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Detects a downward pull-down gesture starting from the top edge of the
 * composable. Designed for the unified canvas which can scroll vertically OR
 * horizontally — a single gesture works in both orientations because we read
 * pointer events at [PointerEventPass.Initial] (so an inner verticalScroll
 * doesn't swallow the drag before we see it).
 *
 * The original drag is NOT consumed while accumulating, so children continue
 * to behave normally; we only consume once the trigger threshold is crossed.
 *
 * @param enabled when false the modifier is a no-op (lets caller gate cheaply).
 * @param isAtTop must return true for the trigger to fire — caller's job to
 *        ensure the host isn't mid-scroll (in vertical mode, pass `scrollState.value == 0`).
 * @param edgeHeight only drags whose initial down-event is within this top
 *        strip are considered.
 * @param triggerThreshold cumulative downward Y delta needed to fire.
 */
@Composable
fun Modifier.topEdgePullDown(
    enabled: Boolean,
    isAtTop: () -> Boolean,
    onTrigger: () -> Unit,
    edgeHeight: Dp = 48.dp,
    triggerThreshold: Dp = 80.dp
): Modifier = composed {
    if (!enabled) return@composed this
    val density = LocalDensity.current
    val edgePx = with(density) { edgeHeight.toPx() }
    val triggerPx = with(density) { triggerThreshold.toPx() }
    this.pointerInput(enabled, edgePx, triggerPx) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial
            )
            if (down.position.y > edgePx) return@awaitEachGesture
            if (!isAtTop()) return@awaitEachGesture
            var accumulated = 0f
            var triggered = false
            while (!triggered) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change: PointerInputChange =
                    event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (!change.pressed) return@awaitEachGesture
                val dy = change.positionChange().y
                if (dy < -2f) return@awaitEachGesture
                if (dy > 0f) {
                    accumulated += dy
                    if (accumulated >= triggerPx) {
                        triggered = true
                        change.consume()
                        onTrigger()
                    }
                }
            }
        }
    }
}
