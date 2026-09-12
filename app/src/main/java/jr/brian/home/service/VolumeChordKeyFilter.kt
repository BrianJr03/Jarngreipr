package jr.brian.home.service

import android.view.KeyEvent

/**
 * Pure key-event logic for the "Select-held + Volume Up/Down adjusts the bottom
 * screen" chord. Kept separate from [HomeInterceptorService] so it can be
 * unit-tested without the full AccessibilityService lifecycle.
 *
 * Rules:
 * - Select (`KEYCODE_BUTTON_SELECT`) is tracked but never consumed — foreground
 *   games must still see it.
 * - Volume Up/Down without Select held: pass through. Stock system volume UI.
 * - Volume Up/Down with Select held: consume both DOWN and UP so the system
 *   volume panel never appears; DOWN emits an [Result.AdjustBottom]. Auto-repeat
 *   is throttled to one step per [minStepIntervalMs] to make long-holds smooth
 *   rather than instant 0-to-15 jumps.
 * - Anything else: pass through.
 */
internal class VolumeChordKeyFilter(
    private val minStepIntervalMs: Long = DEFAULT_MIN_STEP_INTERVAL_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) {
    sealed interface Result {
        data object PassThrough : Result
        data object Consume : Result
        data class AdjustBottom(val delta: Int) : Result
    }

    private var selectHeld: Boolean = false
    private var lastStepAtMs: Long = 0L

    fun onKey(event: KeyEvent, enabled: Boolean): Result {
        if (!enabled) {
            selectHeld = false
            return Result.PassThrough
        }

        return when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_SELECT -> handleSelect(event)
            KeyEvent.KEYCODE_VOLUME_UP -> handleVolume(event, delta = +1)
            KeyEvent.KEYCODE_VOLUME_DOWN -> handleVolume(event, delta = -1)
            else -> Result.PassThrough
        }
    }

    /** Called from the service's `onServiceConnected`. */
    fun reset() {
        selectHeld = false
        lastStepAtMs = 0L
    }

    private fun handleSelect(event: KeyEvent): Result {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> selectHeld = true
            KeyEvent.ACTION_UP -> selectHeld = false
        }
        return Result.PassThrough
    }

    private fun handleVolume(event: KeyEvent, delta: Int): Result {
        if (!selectHeld) return Result.PassThrough

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                val now = nowMs()
                if (event.repeatCount == 0) {
                    lastStepAtMs = now
                    Result.AdjustBottom(delta)
                } else if (now - lastStepAtMs >= minStepIntervalMs) {
                    lastStepAtMs = now
                    Result.AdjustBottom(delta)
                } else {
                    Result.Consume
                }
            }
            KeyEvent.ACTION_UP -> Result.Consume
            else -> Result.Consume
        }
    }

    companion object {
        const val DEFAULT_MIN_STEP_INTERVAL_MS = 150L
    }
}
