package jr.brian.jarnfe.session

internal const val DECK_RETRY_LIMIT = 3
internal const val DECK_STABLE_MS = 30_000L
internal const val DECK_SELECT_CONFIRM_MS = 6_000L
internal const val DECK_FOCUS_SETTLE_MS = 400L
private val DECK_RETRY_DELAYS_MS = longArrayOf(500L, 2_000L, 5_000L)

/**
 * [Covered] means the Deck was already hidden by something else when it closed, so that
 * something may still own the display.
 */
enum class JarnDeckClose { Unexpected, Covered, Deliberate }

/** Null for a configuration change, where Android recreates the window by itself. */
fun jarnDeckCloseKind(
    isDeliberate: Boolean,
    isRecreating: Boolean,
    wasCovered: Boolean,
): JarnDeckClose? = when {
    isDeliberate -> JarnDeckClose.Deliberate
    isRecreating -> null
    wasCovered -> JarnDeckClose.Covered
    else -> JarnDeckClose.Unexpected
}

data class JarnDeckSurroundings(
    val hasDeckDisplay: Boolean,
    val isStageResumed: Boolean,
    val isStageFinishing: Boolean,
    val isDeckHosted: Boolean,
    val isLaunchPending: Boolean,
) {
    val canLaunch: Boolean
        get() = hasDeckDisplay && isStageResumed && !isStageFinishing && !isDeckHosted && !isLaunchPending
}

enum class JarnDeckSelectAction { NotMissing, Relaunch, AskConfirm, Waiting }

data class JarnDeckSelectResult(val next: JarnDeckRecovery, val action: JarnDeckSelectAction)

/**
 * Android offers no way to see which app is on another display, so ownership is inferred and
 * every doubt counts as owned. An owned display is only reclaimed by a confirmed press of X, the switch screen button.
 *
 * [isFocusUnclear] covers the moment after Stage loses focus: a cover of Stage pauses it soon
 * after, while an app opening on the Deck display leaves it resumed.
 */
data class JarnDeckRecovery(
    val isMissing: Boolean = false,
    val isDisplayHeld: Boolean = false,
    val isFocusUnclear: Boolean = false,
    val attempts: Int = 0,
    val upSinceMs: Long? = null,
    val confirmUntilMs: Long? = null,
) {
    val isOutOfRetries: Boolean get() = attempts >= DECK_RETRY_LIMIT

    /** Automatic recovery has stopped, so only X can bring the Deck back. */
    val needsSelect: Boolean get() = isMissing && !isFocusUnclear && (isDisplayHeld || isOutOfRetries)

    fun onDeckUp(nowMs: Long): JarnDeckRecovery = JarnDeckRecovery(attempts = attempts, upSinceMs = nowMs)

    fun onDeckClosed(close: JarnDeckClose, nowMs: Long): JarnDeckRecovery {
        if (close == JarnDeckClose.Deliberate) return JarnDeckRecovery()
        val wasStable = upSinceMs != null && nowMs - upSinceMs >= DECK_STABLE_MS
        return JarnDeckRecovery(
            isMissing = true,
            isDisplayHeld = close == JarnDeckClose.Covered,
            attempts = if (wasStable) 0 else attempts,
        )
    }

    fun nextRetryDelayMs(surroundings: JarnDeckSurroundings): Long? = when {
        !isMissing || isDisplayHeld || isFocusUnclear || isOutOfRetries -> null
        !surroundings.canLaunch -> null
        else -> DECK_RETRY_DELAYS_MS[attempts]
    }

    fun onRetryLaunched(): JarnDeckRecovery = copy(attempts = attempts + 1)

    fun onStageLostFocus(): JarnDeckRecovery =
        if (isMissing && !isDisplayHeld) copy(isFocusUnclear = true) else this

    fun onStageFocusBack(): JarnDeckRecovery = copy(isFocusUnclear = false)

    fun onStagePaused(): JarnDeckRecovery = copy(isFocusUnclear = false)

    fun onFocusSettled(): JarnDeckRecovery =
        if (isFocusUnclear) copy(isFocusUnclear = false, isDisplayHeld = true) else this

    fun onOtherAppOpened(): JarnDeckRecovery =
        if (isMissing) copy(isDisplayHeld = true, isFocusUnclear = false) else this

    /** A manual relaunch spends every retry, so a failure waits for the next press. */
    fun onSelect(surroundings: JarnDeckSurroundings, nowMs: Long): JarnDeckSelectResult {
        val needsConfirm = isDisplayHeld || isFocusUnclear
        val isConfirmed = confirmUntilMs != null && nowMs <= confirmUntilMs
        return when {
            !isMissing -> JarnDeckSelectResult(this, JarnDeckSelectAction.NotMissing)
            !surroundings.canLaunch -> JarnDeckSelectResult(this, JarnDeckSelectAction.Waiting)
            needsConfirm && !isConfirmed -> JarnDeckSelectResult(
                copy(confirmUntilMs = nowMs + DECK_SELECT_CONFIRM_MS),
                JarnDeckSelectAction.AskConfirm,
            )
            else -> JarnDeckSelectResult(
                copy(
                    isDisplayHeld = false,
                    isFocusUnclear = false,
                    attempts = DECK_RETRY_LIMIT,
                    confirmUntilMs = null,
                ),
                JarnDeckSelectAction.Relaunch,
            )
        }
    }
}
