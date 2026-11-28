package jr.brian.jarnfe.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import jr.brian.jarnfe.input.JarnSticks
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.truncate

/** Screen widths per second a held piece moves at full deflection. */
internal const val STICK_MOVE_SPEED = 0.5f

/** Screen widths per second a held piece grows or shrinks at full deflection. */
internal const val STICK_RESIZE_SPEED = 0.3f

private const val NANOS_PER_SECOND = 1_000_000_000f

/** Float travel lands a hair under a whole step; this keeps it from losing one. */
private const val STEP_TOLERANCE = 1e-3f

/** Longest frame gap counted, so a stalled frame never throws a piece across the screen. */
private const val MAX_FRAME_SECONDS = 0.1f

/**
 * Stick travel not yet turned into whole steps. Moves happen in the same steps as the D-pad (the
 * grid with snapping, fine steps without), so snapping and clamping behave exactly the same; this
 * keeps the remainder between frames so slow, small deflections still move.
 */
internal class ThemeEditorStickTravel {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f

    fun reset() {
        x = 0f
        y = 0f
        width = 0f
        height = 0f
    }
}

/**
 * [seconds] of [sticks] applied to the held piece: the left stick moves it, the right stick
 * resizes it (right is wider, up is taller). Speed follows how far each stick is pushed.
 */
internal fun ThemeEditorState.stickStep(sticks: JarnSticks, seconds: Float, travel: ThemeEditorStickTravel): ThemeEditorState {
    if (held == null) return this
    val step = if (snap) SNAP_STEP else FINE_STEP
    val time = seconds.coerceIn(0f, MAX_FRAME_SECONDS)
    travel.x += sticks.left.x * STICK_MOVE_SPEED * time
    travel.y += sticks.left.y * STICK_MOVE_SPEED * time
    travel.width += sticks.right.x * STICK_RESIZE_SPEED * time
    travel.height -= sticks.right.y * STICK_RESIZE_SPEED * time
    var next = this
    takeSteps(travel.x, step) { steps -> travel.x -= steps * step; next = next.nudged(steps.sign, 0, abs(steps)) }
    takeSteps(travel.y, step) { steps -> travel.y -= steps * step; next = next.nudged(0, steps.sign, abs(steps)) }
    takeSteps(travel.width, step) { steps ->
        travel.width -= steps * step
        next = next.copy(axis = ThemeEditorAxis.Width).resized(steps.sign, abs(steps)).copy(axis = axis)
    }
    takeSteps(travel.height, step) { steps ->
        travel.height -= steps * step
        next = next.copy(axis = ThemeEditorAxis.Height).resized(steps.sign, abs(steps)).copy(axis = axis)
    }
    return next
}

private inline fun takeSteps(travel: Float, step: Float, apply: (Int) -> Unit) {
    val steps = truncate(travel / step + STEP_TOLERANCE * travel.sign).toInt()
    if (steps != 0) apply(steps)
}


/**
 * The sticks while a piece is held: the latest reading, applied every frame until both sticks
 * are centered. Each push, from leaving center to returning, is one undo step.
 */
internal class ThemeEditorStickDriver {
    private var latest = JarnSticks()
    private var lastFrameNanos = 0L
    private val travel = ThemeEditorStickTravel()

    fun onSticks(sticks: JarnSticks) {
        latest = sticks
    }

    /** One frame: begins a gesture on the first push, ends it once both sticks are centered. */
    fun onFrame(frameNanos: Long) {
        val seconds = if (lastFrameNanos == 0L) 0f else (frameNanos - lastFrameNanos) / NANOS_PER_SECOND
        lastFrameNanos = frameNanos
        val sticks = latest
        if (sticks.isCentered) {
            travel.reset()
            JarnThemeEditor.endStickGesture()
            return
        }
        JarnThemeEditor.beginStickGesture()
        JarnThemeEditor.update { stickStep(sticks, seconds, travel) }
    }

    fun stop() {
        latest = JarnSticks()
        lastFrameNanos = 0L
        travel.reset()
        JarnThemeEditor.endStickGesture()
    }
}

/** Runs [driver] every frame while [held]; a dropped or put back piece ends any gesture. */
@Composable
internal fun ThemeEditorStickFrames(held: Boolean, driver: ThemeEditorStickDriver) {
    LaunchedEffect(held) {
        if (!held) return@LaunchedEffect
        try {
            while (true) withFrameNanos(driver::onFrame)
        } finally {
            driver.stop()
        }
    }
}

@Composable
internal fun rememberThemeEditorStickDriver(): ThemeEditorStickDriver = remember { ThemeEditorStickDriver() }
