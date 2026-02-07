package jr.brian.home.ui.extensions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A composable modifier that handles tap gestures (press, double-tap, long-press)
 * without blocking parent horizontal swipe gestures (e.g., HorizontalPager).
 *
 * Unlike `combinedClickable`, this uses `detectTapGestures` which waits to determine
 * the gesture type before consuming events, allowing parent gesture detectors
 * (like HorizontalPager) to intercept horizontal swipes.
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
 * Simplified version that creates its own interaction source and press state.
 * Returns the interaction source and press state for use in animations.
 */
data class PagerFriendlyClickableState(
    val interactionSource: MutableInteractionSource,
    val isPressed: MutableState<Boolean>
)

@Composable
fun rememberPagerFriendlyClickableState(): PagerFriendlyClickableState {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { androidx.compose.runtime.mutableStateOf(false) }
    return remember(interactionSource, isPressed) {
        PagerFriendlyClickableState(interactionSource, isPressed)
    }
}
