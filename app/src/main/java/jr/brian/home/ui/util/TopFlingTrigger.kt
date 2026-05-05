package jr.brian.home.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Creates a NestedScrollConnection that triggers a callback when the user
 * drags past the top of a LazyColumn (pull-down gesture while finger is on screen).
 *
 * @param listState The LazyListState to check scroll position
 * @param overscrollThreshold Pixels of overscroll needed to trigger (default 80f)
 * @param onFlingAtTop Called when user overscrolls at the top
 */
@Composable
fun rememberTopFlingTrigger(
    listState: LazyListState,
    overscrollThreshold: Float = 80f,
    onFlingAtTop: () -> Unit
): NestedScrollConnection {
    return remember(listState, overscrollThreshold, onFlingAtTop) {
        TopFlingNestedScrollConnection(
            canScrollBackward = { listState.canScrollBackward },
            overscrollThreshold = overscrollThreshold,
            onTrigger = onFlingAtTop
        )
    }
}

/**
 * Creates a NestedScrollConnection that triggers a callback when the user
 * drags past the top of a LazyGrid (pull-down gesture while finger is on screen).
 *
 * @param gridState The LazyGridState to check scroll position
 * @param overscrollThreshold Pixels of overscroll needed to trigger (default 80f)
 * @param onFlingAtTop Called when user overscrolls at the top
 */
@Composable
fun rememberTopFlingTrigger(
    gridState: LazyGridState,
    overscrollThreshold: Float = 80f,
    onFlingAtTop: () -> Unit
): NestedScrollConnection {
    return remember(gridState, overscrollThreshold, onFlingAtTop) {
        TopFlingNestedScrollConnection(
            canScrollBackward = { gridState.canScrollBackward },
            overscrollThreshold = overscrollThreshold,
            onTrigger = onFlingAtTop
        )
    }
}

private class TopFlingNestedScrollConnection(
    private val canScrollBackward: () -> Boolean,
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
        if (source == NestedScrollSource.UserInput) {
            // available.y > 0 means trying to scroll up with nowhere to go (at top)
            if (available.y > 0 && !canScrollBackward()) {
                accumulatedOverscroll += available.y
                if (accumulatedOverscroll >= overscrollThreshold && !hasTriggeredThisGesture) {
                    hasTriggeredThisGesture = true
                    onTrigger()
                }
            } else {
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
