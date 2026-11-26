package jr.brian.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/** How long A stays down on a tab before the strip enters reorder. */
const val JARN_TAB_HOLD_MS = 500L

/** The held tab's lift while the strip is being reordered. */
internal const val JARN_TAB_LIFT_SCALE = 1.06f

/**
 * [order] with the item at [position] moved [delta] places, one neighbour at a time. A move past
 * either end leaves the order as it is.
 */
fun jarnMovedOrder(order: List<Int>, position: Int, delta: Int): List<Int> {
    val target = position + delta
    if (position !in order.indices || target !in order.indices) return order
    return order.toMutableList().apply { add(target, removeAt(position)) }
}

/**
 * [saved] ids in a strip's saved order, kept only when [known], with every known id missing from
 * it added at the end in [known]'s order. A tab added later still shows, and one that went away
 * is dropped.
 */
fun <T> jarnNormalizedOrder(saved: List<String>, known: List<T>, idOf: (T) -> String): List<T> {
    val byId = known.associateBy(idOf)
    val kept = saved.distinct().mapNotNull(byId::get)
    return kept + known.filterNot { it in kept }
}

/**
 * A strip's reorder: hold A or long-press a tab to lift it, step it with [move] or drag it, then
 * [commit] or [cancel]. [order] holds the caller's tab indices in their working order, and
 * [onCommit] gets it only when it changed. Input that moves, saves and cancels comes from the
 * caller, since only the caller sees B.
 */
@Stable
class JarnTabReorder(onCommit: (List<Int>) -> Unit) {
    internal var onCommit: (List<Int>) -> Unit = onCommit

    var held by mutableStateOf<Int?>(null)
        private set

    var order by mutableStateOf<List<Int>>(emptyList())
        private set

    internal var dragOffset by mutableStateOf(0f)
        private set

    /** True while a finger moves the held tab, so it follows the finger instead of sliding. */
    internal var dragging by mutableStateOf(false)
        private set

    internal val widths = mutableStateMapOf<Int, Int>()
    private var original: List<Int> = emptyList()

    val isEditing: Boolean get() = held != null

    fun begin(index: Int, count: Int) {
        if (isEditing || index !in 0 until count) return
        original = List(count) { it }
        order = original
        held = index
        dragOffset = 0f
    }

    internal fun beginDrag(index: Int, count: Int) {
        begin(index, count)
        dragging = isEditing
    }

    fun move(delta: Int) {
        val index = held ?: return
        order = jarnMovedOrder(order, order.indexOf(index), delta)
    }

    fun commit() {
        if (!isEditing) return
        val changed = order != original
        val result = order
        end()
        if (changed) onCommit(result)
    }

    fun cancel() = end()

    /** Follows a drag, swapping the held tab with a neighbour once it is dragged past half of it. */
    internal fun drag(dx: Float, gapPx: Float) {
        val index = held ?: return
        dragOffset += dx
        val position = order.indexOf(index)
        val next = order.getOrNull(position + if (dragOffset > 0) 1 else -1) ?: return
        val reach = (widths[next] ?: return) + gapPx
        if (abs(dragOffset) < reach / 2) return
        val delta = if (dragOffset > 0) 1 else -1
        order = jarnMovedOrder(order, position, delta)
        dragOffset -= delta * reach
    }

    private fun end() {
        held = null
        order = emptyList()
        dragOffset = 0f
        dragging = false
    }
}

@Composable
fun rememberJarnTabReorder(onCommit: (List<Int>) -> Unit): JarnTabReorder {
    val latest by rememberUpdatedState(onCommit)
    return remember { JarnTabReorder { latest(it) } }
}

/**
 * While [reorder] is lifted: Left and Right move the tab, a fresh A saves, B cancels, and Up and
 * Down stay on the strip. The release and repeats of the A that lifted the tab are not a fresh A.
 * Jarn's own input layer usually takes these presses first; this covers a strip used without it.
 */
internal fun Modifier.jarnReorderKeys(reorder: JarnTabReorder): Modifier = onPreviewKeyEvent { event ->
    if (!reorder.isEditing) return@onPreviewKeyEvent false
    val down = event.type == KeyEventType.KeyDown
    val fresh = down && event.nativeKeyEvent.repeatCount == 0
    when (event.key) {
        Key.DirectionLeft -> if (down) reorder.move(-1)
        Key.DirectionRight -> if (down) reorder.move(1)
        Key.ButtonA, Key.Enter, Key.DirectionCenter -> {
            if (!fresh) return@onPreviewKeyEvent event.key == Key.ButtonA
            reorder.commit()
        }
        Key.ButtonB, Key.Back, Key.Escape -> if (fresh) reorder.cancel()
        Key.DirectionUp, Key.DirectionDown, Key.ButtonL1, Key.ButtonR1, Key.ButtonL2, Key.ButtonR2 -> Unit
        else -> return@onPreviewKeyEvent false
    }
    true
}

/**
 * Takes A on a tab of a reorderable strip: a tap runs [onTap] as before, and holding it for
 * [JARN_TAB_HOLD_MS] runs [onHold] once. The release after a hold does nothing.
 */
@Composable
internal fun Modifier.jarnHoldA(onTap: () -> Unit, onHold: () -> Unit): Modifier {
    val press = remember { JarnHoldPress() }
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)
    return onPreviewKeyEvent { event ->
        if (event.key != Key.ButtonA) return@onPreviewKeyEvent false
        val native = event.nativeKeyEvent
        val time = native.eventTime
        when (event.type) {
            KeyEventType.KeyDown -> if (press.down(time, native.repeatCount)) hold()
            KeyEventType.KeyUp -> when (press.up(time)) {
                JarnHoldPress.Release.Tap -> tap()
                JarnHoldPress.Release.Hold -> hold()
                JarnHoldPress.Release.None -> Unit
            }
        }
        true
    }
}

/** One A press on a tab, told apart as a tap or a hold from its key times. */
internal class JarnHoldPress {
    enum class Release { Tap, Hold, None }

    private var downAt = NOT_DOWN
    private var holdDone = false

    /** True when this event is the one that makes the press a hold. */
    fun down(time: Long, repeatCount: Int): Boolean {
        if (repeatCount == 0) {
            downAt = time
            holdDone = false
            return false
        }
        if (holdDone || downAt == NOT_DOWN || time - downAt < JARN_TAB_HOLD_MS) return false
        holdDone = true
        return true
    }

    fun up(time: Long): Release {
        val release = when {
            holdDone || downAt == NOT_DOWN -> Release.None
            time - downAt >= JARN_TAB_HOLD_MS -> Release.Hold
            else -> Release.Tap
        }
        downAt = NOT_DOWN
        holdDone = false
        return release
    }

    private companion object {
        const val NOT_DOWN = -1L
    }
}

/** Long-press a tab to lift it, drag sideways to move it, and let go to save the new order. */
internal fun Modifier.jarnTabDrag(reorder: JarnTabReorder, index: Int, count: () -> Int, gapPx: Float): Modifier =
    pointerInput(reorder, index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { reorder.beginDrag(index, count()) },
            onDragEnd = { reorder.commit() },
            onDragCancel = { reorder.cancel() },
            onDrag = { change, amount ->
                change.consume()
                reorder.drag(amount.x, gapPx)
            },
        )
    }

/** Remembers each tab's width, so a drag knows when it has passed a neighbour. */
internal fun Modifier.jarnTabWidth(reorder: JarnTabReorder, index: Int): Modifier =
    onSizeChanged { reorder.widths[index] = it.width }

/** Slides a tab to its new place when the strip's order changes, instead of jumping there. */
@Composable
internal fun Modifier.jarnSlideToPlace(skip: Boolean): Modifier {
    val scope = rememberCoroutineScope()
    val spec = JarnTheme.motion.panel
    val offset = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    val last = remember { arrayOfNulls<IntOffset>(1) }
    return onPlaced { coordinates ->
        val now = coordinates.positionInParent().round()
        val before = last[0]
        last[0] = now
        if (before == null || before == now || skip) return@onPlaced
        scope.launch {
            offset.snapTo(offset.value + (before - now))
            offset.animateTo(IntOffset.Zero, spec.spec())
        }
    }.then(Modifier.layoutOffset { offset.value })
}

private fun Modifier.layoutOffset(offset: () -> IntOffset): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.placeRelative(offset()) }
    }
