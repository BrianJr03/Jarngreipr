package jr.brian.jarnfe.canvas

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.shell.JarnNotices

internal enum class CanvasTapAction { Open, Grab, Ignore }

/** A tap during a grab never restarts it, so a move in progress can't be thrown away. */
internal fun canvasTapAction(hasGrab: Boolean, editing: Boolean): CanvasTapAction = when {
    hasGrab -> CanvasTapAction.Ignore
    editing -> CanvasTapAction.Grab
    else -> CanvasTapAction.Open
}

@Stable
internal class CanvasTouch(val driver: CanvasTouchDriver, val space: CanvasBoardSpace)

@Composable
internal fun rememberCanvasTouch(
    editor: CanvasEditor,
    page: JarnCanvasPage?,
    enabled: Boolean,
    minSpans: (CanvasItem) -> Pair<Int, Int>,
    openMenu: (CanvasItem) -> Unit,
    commit: (CanvasLayout, Map<String, GridRect>) -> Unit,
    onGrabbed: (String) -> Unit,
): CanvasTouch {
    val density = LocalDensity.current.density
    val view = LocalView.current
    val context = LocalContext.current
    val touch = remember(editor, density) {
        CanvasTouch(CanvasTouchDriver(editor), CanvasBoardSpace(density))
    }
    val layout = page?.takeIf { it.kind == JarnCanvasPageKind.Canvas }?.layout
    SideEffect {
        touch.driver.actions = CanvasTouchActions(
            enabled = enabled && layout != null,
            layout = layout,
            minSpans = minSpans,
            openMenu = openMenu,
            commit = commit,
            onGrabbed = onGrabbed,
            haptic = {
                if (context.jarnLookStoreOrNull()?.display?.buttonHaptics != false) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            },
            showBlocked = { JarnNotices.show(R.string.jarnfe_canvas_notice_touch_moving_tile) },
        )
    }
    return touch
}
