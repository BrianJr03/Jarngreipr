package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

private val CanvasRemoveDockHeight = 56.dp

/** Where the remove bar sits in the window, and whether the held tile is over it. */
@Stable
internal class CanvasRemoveDock {
    var armed by mutableStateOf(false)
        private set

    var bounds: Rect? = null

    /** Returns true only on the move that arms the bar, so the haptic fires once. */
    fun update(windowPoint: Offset): Boolean {
        val over = bounds?.contains(windowPoint) == true
        val newlyArmed = over && !armed
        armed = over
        return newlyArmed
    }

    fun reset() {
        armed = false
    }
}

/** The outer box never moves, so its window bounds stay true while the bar slides in. */
@Composable
internal fun CanvasRemoveDockBar(dock: CanvasRemoveDock, visible: Boolean, modifier: Modifier = Modifier) {
    val motion = JarnTheme.motion
    val shown by animateFloatAsState(if (visible) 1f else 0f, motion.small.spec())
    SideEffect { if (!visible) dock.bounds = null }
    if (!visible && shown == 0f) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CanvasRemoveDockHeight)
            .onGloballyPositioned { if (visible) dock.bounds = it.boundsInWindow() },
    ) {
        CanvasRemoveDockFace(dock.armed, shown)
    }
}

@Composable
private fun CanvasRemoveDockFace(armed: Boolean, shown: Float) {
    val colors = JarnTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = size.height * (1f - shown)
                alpha = shown
            }
            .background(if (armed) colors.danger else colors.raised),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(
            text = stringResource(
                if (armed) R.string.jarnfe_canvas_dock_release else R.string.jarnfe_canvas_dock_drop,
            ),
            style = JarnTheme.type.labelStrong,
            color = if (armed) colors.onDanger else colors.textHigh,
            maxLines = 1,
        )
    }
}
