package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign

private const val PAGE_ENTER_MS = 240
private const val PAGE_FADE_REDUCED_MS = 120
private const val RUBBER_SETTLE_MS = 160
private val PageEnterShift = 24.dp
private val PageEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** Starting values from the approved prototype. They still need tuning on a real device. */
internal data class CanvasSwipeThresholds(
    val slopDp: Float = 8f,
    val pageDp: Float = 72f,
    val flingDpPerSec: Float = 900f,
    val flingMinDp: Float = 16f,
    val rubberBand: Float = 0.45f,
)

/**
 * One finger's sideways swipe, measured against the scroll so only the distance past the end counts.
 * A swipe only changes page toward an end it started at, so a scroll can't run on into a new page.
 */
internal class CanvasPageSwipeGesture(
    private val density: Float,
    private val canGoBack: Boolean,
    private val canGoForward: Boolean,
    private val startedAtStart: Boolean,
    private val startedAtEnd: Boolean,
    private val startScroll: Float = 0f,
    private val thresholds: CanvasSwipeThresholds = CanvasSwipeThresholds(),
) {
    var overPx = 0f
        private set

    var isSideways = false
        private set

    private var isAbandoned = false

    val isArmed: Boolean get() = abs(overPx) >= thresholds.pageDp * density

    fun moved(dx: Float, dy: Float, scroll: Float = startScroll) {
        if (isAbandoned) return
        if (!isSideways) {
            if (hypot(dx, dy) < thresholds.slopDp * density) return
            if (abs(dy) > abs(dx)) {
                isAbandoned = true
                return
            }
            isSideways = true
        }
        val past = dx + (scroll - startScroll)
        overPx = when {
            past > 0f && startedAtStart && canGoBack -> past
            past < 0f && startedAtEnd && canGoForward -> past
            else -> 0f
        }
    }

    /** The page direction to go, or 0 to spring back. */
    fun released(velocityPxPerSec: Float): Int {
        if (overPx == 0f) return 0
        val flung = abs(velocityPxPerSec) >= thresholds.flingDpPerSec * density &&
            abs(overPx) >= thresholds.flingMinDp * density &&
            sign(velocityPxPerSec) == sign(overPx)
        return if (isArmed || flung) -sign(overPx).toInt() else 0
    }
}

/** Owns the drawn overscroll, so a swipe redraws one layer and never recomposes the board. */
@Stable
internal class CanvasPageSwipeHost(
    private val driver: CanvasTouchDriver,
    private val editor: CanvasEditor,
    private val scope: CoroutineScope,
    private val thresholds: CanvasSwipeThresholds = CanvasSwipeThresholds(),
) {
    private val settle = Animatable(0f)
    private var active: CanvasPageSwipeGesture? = null
    private var startX = 0f
    private var startY = 0f

    var enabled = false
    var hasPrevious = false
    var hasNext = false
    var reduced = false
    var onPage: (Int) -> Unit = {}

    var overPx by mutableFloatStateOf(0f)
        private set

    var armed by mutableFloatStateOf(0f)
        private set

    val rubberPx: Float get() = if (reduced) 0f else overPx * thresholds.rubberBand

    private val blocked: Boolean
        get() = !enabled || editor.grab != null || editor.editing || driver.heldId != null ||
            driver.widgetClaimedFinger

    /** A page without the board, like RSS, has its own scrolling rows that keep their swipes. */
    val yieldsToContent: Boolean get() = driver.scrollPort == null

    fun begin(density: Float, x: Float, y: Float): CanvasPageSwipeGesture? {
        if (blocked || (!hasPrevious && !hasNext)) return null
        val port = driver.scrollPort
        val along = port?.takeIf { it.horizontal }
        startX = x
        startY = y
        scope.launch { settle.stop() }
        overPx = 0f
        armed = 0f
        return CanvasPageSwipeGesture(
            density = density,
            canGoBack = hasPrevious,
            canGoForward = hasNext,
            startedAtStart = along?.atStart ?: true,
            startedAtEnd = along?.atEnd ?: true,
            startScroll = along?.value ?: 0f,
            thresholds = thresholds,
        ).also { active = it }
    }

    /** False once something else has taken the finger, which ends this swipe. */
    fun moved(gesture: CanvasPageSwipeGesture, x: Float, y: Float): Boolean {
        if (blocked) {
            cancel()
            return false
        }
        val along = driver.scrollPort?.takeIf { it.horizontal }
        gesture.moved(x - startX, y - startY, along?.value ?: 0f)
        overPx = gesture.overPx
        armed = if (gesture.isArmed) 1f else 0f
        return true
    }

    fun release(gesture: CanvasPageSwipeGesture, velocityPxPerSec: Float) {
        val direction = gesture.released(velocityPxPerSec)
        active = null
        armed = 0f
        if (direction != 0) {
            overPx = 0f
            onPage(direction)
        } else {
            springBack()
        }
    }

    fun cancel() {
        if (active == null) return
        active = null
        armed = 0f
        springBack()
    }

    private fun springBack() {
        val from = overPx
        if (from == 0f) return
        if (reduced) {
            overPx = 0f
            return
        }
        scope.launch {
            settle.snapTo(from)
            settle.animateTo(0f, tween(RUBBER_SETTLE_MS, easing = PageEnterEasing)) { overPx = value }
        }
    }
}

@Composable
internal fun rememberCanvasPageSwipe(
    driver: CanvasTouchDriver,
    editor: CanvasEditor,
    enabled: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPage: (Int) -> Unit,
): CanvasPageSwipeHost {
    val scope = rememberCoroutineScope()
    val host = remember(driver, editor) { CanvasPageSwipeHost(driver, editor, scope) }
    val reduced = JarnTheme.motion.reduced
    SideEffect {
        host.enabled = enabled
        host.hasPrevious = hasPrevious
        host.hasNext = hasNext
        host.reduced = reduced
        host.onPage = onPage
    }
    return host
}

/**
 * Reads the Final pass, after the board and the scroll have had their say. Consuming a sideways
 * swipe there stops the tile under the finger from also taking it as a tap.
 */
internal fun Modifier.canvasPageSwipe(host: CanvasPageSwipeHost): Modifier = this
    .pointerInput(host) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val gesture = host.begin(density, down.position.x, down.position.y) ?: return@awaitEachGesture
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.fastFirstOrNull { it.id == down.id }
                if (change == null || event.changes.size > 1) {
                    host.cancel()
                    return@awaitEachGesture
                }
                if (!change.pressed) {
                    host.release(gesture, tracker.calculateVelocity().x)
                    return@awaitEachGesture
                }
                if (change.isConsumed && host.yieldsToContent) {
                    host.cancel()
                    return@awaitEachGesture
                }
                tracker.addPosition(change.uptimeMillis, change.position)
                if (!host.moved(gesture, change.position.x, change.position.y)) return@awaitEachGesture
                if (gesture.isSideways) change.consume()
            }
        }
    }
    .graphicsLayer { translationX = host.rubberPx }

@Composable
internal fun CanvasPagePeek(host: CanvasPageSwipeHost, pageIndex: Int) {
    val direction by remember(host) { derivedStateOf { -sign(host.overPx).toInt() } }
    val armed by remember(host) { derivedStateOf { host.armed > 0f } }
    if (direction == 0) return
    val number = pageIndex + direction + 1
    val label = if (armed) R.string.jarnfe_canvas_peek_release else R.string.jarnfe_canvas_peek
    Box(Modifier.fillMaxSize().padding(JarnTheme.space.md)) {
        JarnSurface(Modifier.align(if (direction < 0) Alignment.CenterStart else Alignment.CenterEnd)) {
            JarnText(
                text = stringResource(label, number),
                style = JarnTheme.type.labelStrong,
                color = JarnTheme.colors.textHigh,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
            )
        }
    }
}

/** A new page eases in from the side it came from, or fades when motion is reduced. */
@Composable
internal fun rememberCanvasPageMotion(selected: Int): Modifier {
    val reduced = JarnTheme.motion.reduced
    val shiftPx = with(LocalDensity.current) { PageEnterShift.toPx() }
    val alpha = remember { Animatable(1f) }
    val shift = remember { Animatable(0f) }
    var shown by remember { mutableIntStateOf(selected) }
    LaunchedEffect(selected) {
        val direction = (selected - shown).coerceIn(-1, 1)
        shown = selected
        if (direction == 0) return@LaunchedEffect
        alpha.snapTo(0f)
        shift.snapTo(if (reduced) 0f else shiftPx * direction)
        coroutineScope {
            launch {
                alpha.animateTo(
                    1f,
                    if (reduced) tween(PAGE_FADE_REDUCED_MS, easing = LinearEasing) else tween(PAGE_ENTER_MS, easing = PageEnterEasing),
                )
            }
            launch { shift.animateTo(0f, tween(PAGE_ENTER_MS, easing = PageEnterEasing)) }
        }
    }
    return remember(alpha, shift) {
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationX = shift.value
        }
    }
}
