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
    onDropped: (CanvasGrab, Map<String, GridRect>) -> Unit,
    removeTile: (CanvasGrab) -> Unit,
    hasCarryTarget: (Int) -> Boolean,
    carry: (Int) -> Boolean,
    moveBetweenPages: (CanvasGrabDrop.BetweenPages) -> Unit,
): CanvasTouch {
    val density = LocalDensity.current.density
    val view = LocalView.current
    val context = LocalContext.current
    val touch = remember(editor, density) {
        val space = CanvasBoardSpace(density)
        val driver = CanvasTouchDriver(
            editor = editor,
            density = density,
            toWindow = space::toWindow,
            fromWindow = space::fromWindow,
        )
        CanvasTouch(driver, space)
    }
    val layout = page?.takeIf { it.kind == JarnCanvasPageKind.Canvas }?.layout
    val hapticsOn = { context.jarnLookStoreOrNull()?.display?.buttonHaptics != false }
    SideEffect {
        touch.driver.actions = CanvasTouchActions(
            enabled = enabled && layout != null,
            layout = layout,
            minSpans = minSpans,
            openMenu = openMenu,
            commit = commit,
            onGrabbed = onGrabbed,
            haptic = { if (hapticsOn()) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
            showBlocked = { JarnNotices.show(R.string.jarnfe_canvas_notice_touch_moving_tile) },
            tick = { if (hapticsOn()) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
            onDropped = onDropped,
            removeTile = removeTile,
            hasCarryTarget = hasCarryTarget,
            carry = carry,
            moveBetweenPages = moveBetweenPages,
        )
    }
    return touch
}
