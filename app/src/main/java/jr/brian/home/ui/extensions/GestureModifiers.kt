package jr.brian.home.ui.extensions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A composable modifier that handles tap gestures (press, double-tap, long-press)
 * without blocking parent horizontal swipe gestures (e.g., HorizontalPager).
 *
 * @param key Optional key(s) to trigger recomposition of the pointer input
 * @param interactionSource The interaction source to emit press interactions to
 * @param isPressedState Mutable state to track press state for animations
 * @param onDoubleTap Callback when double-tap is detected
 * @param onLongPress Callback when long-press is detected
 * @param onTap Optional callback when single tap is detected
 */
fun Modifier.pagerFriendlyClickable(
    vararg key: Any?,
    interactionSource: MutableInteractionSource,
    isPressedState: MutableState<Boolean>,
    onDoubleTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null
): Modifier = this.pointerInput(*key) {
    detectTapGestures(
        onPress = { offset ->
            isPressedState.value = true
            val press = PressInteraction.Press(offset)
            interactionSource.emit(press)
            try {
                awaitRelease()
            } finally {
                isPressedState.value = false
                interactionSource.emit(PressInteraction.Release(press))
            }
        },
        onDoubleTap = onDoubleTap?.let { { _ -> it() } },
        onLongPress = onLongPress?.let { { _ -> it() } },
        onTap = onTap
    )
}

/**
 * A simplified version that uses standard combinedClickable without press animation.
 *
 * @param onDoubleTap Callback when double-tap is detected
 * @param onLongPress Callback when long-press is detected
 * @param onTap Optional callback when single tap is detected
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerFriendlyClickableSimple(
    onDoubleTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null
): Modifier = this.combinedClickable(
    onClick = onTap ?: {},
    onDoubleClick = onDoubleTap,
    onLongClick = onLongPress,
    indication = null,
    interactionSource = remember { MutableInteractionSource() }
)

/**
 * A modifier that detects press/release events and triggers haptic feedback on press.
 * Use this with [onPressScaleAndOffset()] for consistent press animations across the app.
 *
 * NOTE: This uses raw pointer events and will intercept touches during scrolling.
 * Only use in non-scrollable containers (e.g., dock, keyboard).
 * For scrollable lists, use [clickWithHaptic] or [combinedClickWithHaptic] instead.
 *
 * @param keys Optional key(s) to trigger recomposition of the pointer input
 * @param haptic The haptic feedback instance from LocalHapticFeedback.current
 * @param onPressChange Callback to update the pressed state (true on press, false on release)
 * @param onClick Optional callback when the press is released (finger lifted)
 */
fun Modifier.pressWithHaptic(
    vararg keys: Any?,
    haptic: HapticFeedback,
    onPressChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
): Modifier = this.pointerInput(keys) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when {
                event.changes.any { it.pressed } && !event.changes.all { it.previousPressed } -> {
                    onPressChange(true)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                event.changes.none { it.pressed } && event.changes.any { it.previousPressed } -> {
                    onPressChange(false)
                    onClick?.invoke()
                }
            }
        }
    }
}

/**
 * A scroll-safe clickable modifier that triggers haptic feedback on press (touch down).
 * Unlike [pressWithHaptic], this uses the standard [clickable] which respects
 * scroll gestures and won't fire during scrolling.
 *
 * Haptic fires immediately on press via [MutableInteractionSource], which is scroll-aware —
 * in scrollable containers, press is only recognized after touch slop is exceeded.
 *
 * Use this in scrollable containers (LazyColumn, LazyGrid, verticalScroll, etc.).
 *
 * @param haptic The haptic feedback instance from LocalHapticFeedback.current
 * @param onClick Callback when the item is clicked
 */
fun Modifier.clickWithHaptic(
    haptic: HapticFeedback,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

/**
 * A scroll-safe combined clickable modifier that triggers haptic feedback on press (touch down).
 * Unlike [pressWithHaptic], this uses the standard [combinedClickable] which respects
 * scroll gestures and won't fire during scrolling.
 *
 * Haptic fires immediately on press via [MutableInteractionSource], which is scroll-aware —
 * in scrollable containers, press is only recognized after touch slop is exceeded.
 *
 * Use this in scrollable containers (LazyColumn, LazyGrid, verticalScroll, etc.).
 *
 * @param haptic The haptic feedback instance from LocalHapticFeedback.current
 * @param onClick Callback when the item is clicked
 * @param onDoubleClick Optional callback when the item is double-clicked
 * @param onLongClick Optional callback when the item is long-clicked
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickWithHaptic(
    haptic: HapticFeedback,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        onLongClick = onLongClick
    )
}
