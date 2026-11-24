package jr.brian.jarnfe.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified

internal class CanvasWidgetTouchBinding(
    val driver: CanvasTouchDriver,
    val space: CanvasBoardSpace,
    val itemId: String,
    val enabled: Boolean,
    val onBlankTap: () -> Unit,
)

private fun View.scrollAxesAt(x: Float, y: Float): CanvasScrollAxes {
    if (!isShown || x < 0 || y < 0 || x >= width || y >= height) return CanvasScrollAxes(false, false)
    var horizontal = canScrollHorizontally(1) || canScrollHorizontally(-1)
    var vertical = canScrollVertically(1) || canScrollVertically(-1)
    (this as? ViewGroup)?.let { group ->
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            val axes = child.scrollAxesAt(x + group.scrollX - child.left, y + group.scrollY - child.top)
            horizontal = horizontal || axes.horizontal
            vertical = vertical || axes.vertical
        }
    }
    return CanvasScrollAxes(horizontal, vertical)
}

/**
 * Sits around a hosted widget so a hold can lift it while its own buttons and lists keep working.
 * The hold runs from a timer in dispatch, because a finger held still sends no events to intercept.
 */
@SuppressLint("ViewConstructor")
internal class CanvasWidgetTouchFrame(context: Context) : FrameLayout(context), CanvasWidgetFinger {
    var binding: CanvasWidgetTouchBinding? = null
        set(value) {
            field = value
            if (isAttachedToWindow) value?.let { it.driver.attachWidget(it.itemId, this) }
        }

    private val density = resources.displayMetrics.density
    private val claimPx = CanvasTouchThresholds().widgetClaimDp * density
    private val gesture = CanvasTouchGesture(density)
    private var claimDecided = false
    private var tracking = false
    private var swallowing = false
    private var childTook = false
    private var pointerId = MotionEvent.INVALID_POINTER_ID
    private var downTime = 0L
    private var pressRawX = 0f
    private var pressRawY = 0f
    private var pressBoard = Offset.Zero
    private var lastBoard = Offset.Zero
    private var scrollAxes = CanvasScrollAxes(false, false)
    private val lift = Runnable { onHoldElapsed() }

    override fun requestDisallowInterceptTouchEvent(disallow: Boolean) {
        if (disallow) {
            binding?.driver?.widgetClaimedFinger = true
            gesture.claimedElsewhere()
            removeCallbacks(lift)
        }
        super.requestDisallowInterceptTouchEvent(disallow)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) tracking = beginTracking(ev)
        if (!tracking) return if (swallowing) swallow(ev) else super.dispatchTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(ev)
            MotionEvent.ACTION_MOVE -> onMove(ev)
            MotionEvent.ACTION_POINTER_UP ->
                if (ev.getPointerId(ev.actionIndex) == pointerId) finish(gesture.up())
            MotionEvent.ACTION_UP -> finish(gesture.up())
            MotionEvent.ACTION_CANCEL -> finish(gesture.cancelled())
        }
        if (swallowing) return swallow(ev)
        val handled = super.dispatchTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            childTook = handled
            scrollAxes = if (handled) scrollAxesAt(ev.x, ev.y) else CanvasScrollAxes(false, false)
        }
        return true
    }

    private fun swallow(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            swallowing = false
        }
        return true
    }

    override fun onOtherFinger(palmLike: Boolean) {
        if (tracking) apply(gesture.otherFinger(palmLike))
        if (gesture.phase != CanvasTouchPhase.Pressing) removeCallbacks(lift)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding?.let { it.driver.attachWidget(it.itemId, this) }
    }

    override fun onDetachedFromWindow() {
        if (tracking) finish(gesture.cancelled())
        swallowing = false
        binding?.let { it.driver.detachWidget(it.itemId, this) }
        super.onDetachedFromWindow()
    }

    private fun beginTracking(ev: MotionEvent): Boolean {
        removeCallbacks(lift)
        swallowing = false
        claimDecided = false
        val active = binding?.takeIf { it.enabled && it.driver.widgetCanLift() } ?: return false
        val board = active.space.fromScreen(ev.rawX, ev.rawY)
        if (!board.isSpecified) return false
        pointerId = ev.getPointerId(0)
        downTime = ev.downTime
        pressRawX = ev.rawX
        pressRawY = ev.rawY
        pressBoard = board
        lastBoard = board
        active.driver.activeWidget = this
        active.driver.widgetClaimedFinger = false
        gesture.down(CanvasTouchStart.BrowseWidget, ev.rawX, ev.rawY, ev.eventTime)
        gesture.liftAtMs?.let { postDelayed(lift, (it - ev.eventTime).coerceAtLeast(1L)) }
        return true
    }

    private fun onHoldElapsed() {
        val effect = gesture.liftAtMs?.let(gesture::timeReached) ?: return
        apply(effect)
        if (gesture.phase != CanvasTouchPhase.Lifted) return
        swallowing = true
        val now = SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(downTime, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    private fun onPointerDown(ev: MotionEvent) {
        val index = ev.actionIndex
        val space = binding?.space ?: return onOtherFinger(false)
        val point = space.fromScreen(ev.getRawX(index), ev.getRawY(index))
        val palm = point.isSpecified && space.isPalm(ev.getTouchMajor(index), point)
        onOtherFinger(palm)
    }

    private fun onMove(ev: MotionEvent) {
        val index = ev.findPointerIndex(pointerId)
        if (index < 0) return
        val rawX = ev.getRawX(index)
        val rawY = ev.getRawY(index)
        binding?.space?.let { space ->
            val board = space.fromScreen(rawX, rawY)
            if (board.isSpecified) lastBoard = board
        }
        claimScrollIfAlongAxis(rawX - pressRawX, rawY - pressRawY)
        apply(gesture.moved(rawX, rawY))
        if (gesture.phase == CanvasTouchPhase.PassedThrough) removeCallbacks(lift)
    }

    private fun claimScrollIfAlongAxis(deltaX: Float, deltaY: Float) {
        if (claimDecided || gesture.phase != CanvasTouchPhase.Pressing) return
        val claim = canvasWidgetClaim(deltaX, deltaY, scrollAxes, claimPx) ?: return
        claimDecided = true
        if (claim) {
            binding?.driver?.widgetClaimedFinger = true
            parent?.requestDisallowInterceptTouchEvent(true)
        }
    }

    private fun finish(effect: CanvasTouchEffect) {
        removeCallbacks(lift)
        apply(effect)
        if (effect == CanvasTouchEffect.Tap && !childTook) binding?.onBlankTap?.invoke()
        binding?.driver?.takeIf { it.activeWidget === this }?.let {
            it.activeWidget = null
            it.widgetClaimedFinger = false
        }
        tracking = false
    }

    private fun apply(effect: CanvasTouchEffect) {
        val active = binding ?: return
        active.driver.handle(gesture, effect, active.itemId, pressBoard, lastBoard)
    }
}
