package jr.brian.jarnfe.canvas

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.shell.JarnNotices

private val ControlOutlineWidth = 3.dp

/** A clickable area inside a hosted widget, relative to the widget's top-left corner. */
internal data class CanvasControlBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

/** The nearest control in the pressed direction, or [current] when there is none that way. */
internal fun nextCanvasControl(
    controls: List<CanvasControlBounds>,
    current: Int,
    deltaX: Int,
    deltaY: Int,
): Int {
    val from = controls.getOrNull(current) ?: return current
    return controls.indices
        .filter { it != current }
        .filter { index ->
            val to = controls[index]
            when {
                deltaX > 0 -> to.centerX > from.centerX
                deltaX < 0 -> to.centerX < from.centerX
                deltaY > 0 -> to.centerY > from.centerY
                deltaY < 0 -> to.centerY < from.centerY
                else -> false
            }
        }
        .minByOrNull { index ->
            val to = controls[index]
            val along = if (deltaX != 0) to.centerX - from.centerX else to.centerY - from.centerY
            val across = if (deltaX != 0) to.centerY - from.centerY else to.centerX - from.centerX
            along.toLong() * along + 4L * across * across
        } ?: current
}

private fun View.clickableControls(): List<View> {
    if (!isShown) return emptyList()
    if (isClickable) return listOf(this)
    val group = this as? ViewGroup ?: return emptyList()
    return (0 until group.childCount).flatMap { group.getChildAt(it).clickableControls() }
}

private fun View.boundsWithin(root: View): CanvasControlBounds {
    val own = IntArray(2).also(::getLocationInWindow)
    val base = IntArray(2).also(root::getLocationInWindow)
    val left = own[0] - base[0]
    val top = own[1] - base[1]
    return CanvasControlBounds(left, top, left + width, top + height)
}

/** Holds the host only while this screen is started, and only on a page that shows a widget. */
@Composable
internal fun rememberCanvasWidgetLease(data: JarnCanvasData, enabled: Boolean): JarnWidgetLease? {
    val owner = LocalLifecycleOwner.current
    var lease by remember { mutableStateOf<JarnWidgetLease?>(null) }
    DisposableEffect(owner, data, enabled) {
        val release = {
            lease?.close()
            lease = null
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (enabled && lease == null) lease = data.acquireWidgetHost()
                Lifecycle.Event.ON_STOP -> release()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            release()
        }
    }
    return lease
}

@Composable
internal fun BoxScope.CanvasWidgetTile(
    data: JarnCanvasData,
    widgetId: Int,
    lease: JarnWidgetLease?,
    revision: Int,
    editing: Boolean,
    interacting: Boolean,
    frozen: Boolean,
    touch: CanvasWidgetTouchBinding,
    onEditTap: () -> Unit,
    onLeave: () -> Unit,
) {
    val health = remember(widgetId, revision, lease) { data.widgetHealth(widgetId) }
    if (health != JarnWidgetHealth.Live) {
        CanvasWidgetProblem(health)
        return
    }
    if (lease == null) return
    var hostView by remember { mutableStateOf<View?>(null) }
    key(lease, widgetId, revision) {
        val context = LocalContext.current
        val view = remember { lease.createView(context, widgetId) }
        if (view == null) {
            CanvasWidgetProblem(JarnWidgetHealth.Gone)
        } else {
            AndroidView(
                factory = { frameContext ->
                    (view.parent as? ViewGroup)?.removeView(view)
                    (view as? ViewGroup)?.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    view.isFocusable = false
                    hostView = view
                    CanvasWidgetTouchFrame(frameContext).apply {
                        isFocusable = false
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    }
                },
                update = { frame -> frame.binding = touch },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
    CanvasWidgetFreeze(hostView, frozen)
    if (editing) {
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onEditTap() }) },
        )
    }
    val root = hostView
    if (interacting && root != null) CanvasWidgetControls(root, onLeave)
}

@Composable
private fun BoxScope.CanvasWidgetControls(root: View, onLeave: () -> Unit) {
    val controls = remember(root) { root.clickableControls() }
    val bounds = remember(controls) { controls.map { it.boundsWithin(root) } }
    var selected by remember(root) { mutableIntStateOf(0) }
    LaunchedEffect(controls) {
        if (controls.isEmpty()) {
            JarnNotices.show(R.string.jarnfe_canvas_notice_no_controls)
            onLeave()
        }
    }
    val move: (Int, Int) -> Unit = { x, y -> selected = nextCanvasControl(bounds, selected, x, y) }
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = controls.isNotEmpty(),
        bindings = {
            on(JarnAction.MoveUp, R.string.jarnfe_hint_move) { move(0, -1) }
            on(JarnAction.MoveDown) { move(0, 1) }
            on(JarnAction.MoveLeft) { move(-1, 0) }
            on(JarnAction.MoveRight) { move(1, 0) }
            on(JarnAction.Confirm, R.string.jarnfe_canvas_hint_press) {
                controls.getOrNull(selected)?.performClick()
            }
            on(JarnAction.Back, R.string.jarnfe_canvas_hint_leave, onPress = onLeave)
        },
    )
    bounds.getOrNull(selected)?.let { CanvasControlOutline(it) }
}

@Composable
private fun CanvasControlOutline(control: CanvasControlBounds) {
    val density = LocalDensity.current
    val width = with(density) { (control.right - control.left).toDp() }
    val height = with(density) { (control.bottom - control.top).toDp() }
    Box(
        Modifier
            .offset { IntOffset(control.left, control.top) }
            .size(width, height)
            .border(ControlOutlineWidth, JarnTheme.colors.focus, JarnTheme.radius.smShape),
    )
}

@Composable
private fun BoxScope.CanvasWidgetProblem(health: JarnWidgetHealth) {
    val (title, detail) = if (health == JarnWidgetHealth.Unbound) {
        R.string.jarnfe_canvas_widget_unbound to R.string.jarnfe_canvas_widget_unbound_detail
    } else {
        R.string.jarnfe_canvas_widget_gone to R.string.jarnfe_canvas_widget_gone_detail
    }
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxSize()
            .padding(JarnTheme.space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs, Alignment.CenterVertically),
    ) {
        JarnText(
            text = stringResource(title),
            style = JarnTheme.type.captionStrong,
            color = JarnTheme.colors.textHigh,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
        JarnText(
            text = stringResource(detail),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            maxLines = 3,
            textAlign = TextAlign.Center,
        )
    }
}
