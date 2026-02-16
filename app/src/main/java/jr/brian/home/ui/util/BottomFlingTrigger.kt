package jr.brian.home.ui.util

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Creates a NestedScrollConnection that triggers a callback when the user
 * drags past the bottom of the list (pull-up gesture while finger is on screen).
 *
 * @param gridState The LazyGridState to check scroll position
 * @param overscrollThreshold Pixels of overscroll needed to trigger (default 80f)
 * @param onFlingAtBottom Called when user overscroll at the bottom
 */
@Composable
fun rememberBottomFlingTrigger(
    gridState: LazyGridState,
    overscrollThreshold: Float = 80f,
    onFlingAtBottom: () -> Unit
): NestedScrollConnection {
    return remember(gridState, overscrollThreshold, onFlingAtBottom) {
        BottomFlingNestedScrollConnection(
            gridState = gridState,
            overscrollThreshold = overscrollThreshold,
            onTrigger = onFlingAtBottom
        )
    }
}

private class BottomFlingNestedScrollConnection(
    private val gridState: LazyGridState,
    private val overscrollThreshold: Float,
    private val onTrigger: () -> Unit
) : NestedScrollConnection {
    private var accumulatedOverscroll = 0f
    private var hasTriggeredThisGesture = false

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // Only trigger during drag gestures (finger on screen)
        if (source == NestedScrollSource.Drag) {
            // available.y < 0 means trying to scroll down with nowhere to go
            if (available.y < 0 && !gridState.canScrollForward) {
                accumulatedOverscroll += -available.y
                if (accumulatedOverscroll >= overscrollThreshold && !hasTriggeredThisGesture) {
                    hasTriggeredThisGesture = true
                    onTrigger()
                }
            } else {
                // Reset if scrolling in other direction or not at bottom
                accumulatedOverscroll = 0f
                hasTriggeredThisGesture = false
            }
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        accumulatedOverscroll = 0f
        hasTriggeredThisGesture = false
        return Velocity.Zero
    }
}
