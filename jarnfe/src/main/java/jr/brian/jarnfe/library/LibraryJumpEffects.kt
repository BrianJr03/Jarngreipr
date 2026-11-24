package jr.brian.jarnfe.library

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sign

/** Longest a jump waits for a freshly opened list to put focus back before it starts moving. */
private const val JUMP_SETTLE_WAIT_MS = 600L

/** A running jump. Any press while it runs cuts it short and lands at once. */
internal class LibraryJumpLanding {
    var landNow by mutableStateOf<(() -> Unit)?>(null)
}

internal fun Modifier.libraryJumpStopper(landing: LibraryJumpLanding): Modifier = onPreviewKeyEvent { event ->
    val stop = landing.landNow ?: return@onPreviewKeyEvent false
    if (event.type == KeyEventType.KeyDown) stop()
    true
}

/**
 * Moves the grid to [jump]'s game by animating its scroll, never its focus, so only the rows on
 * screen are composed. Focus and Now follow once [onLanded] runs at the end.
 */
@Composable
internal fun rememberLibraryGridJump(
    state: LazyGridState,
    jump: LibraryJump?,
    itemIndex: Int,
    headerCount: Int,
    isSettled: () -> Boolean,
    onLanded: (String) -> Unit,
): LibraryJumpLanding {
    val landing = remember { LibraryJumpLanding() }
    val motion = JarnTheme.motion
    val land by rememberUpdatedState(onLanded)
    val settled by rememberUpdatedState(isSettled)
    LaunchedEffect(jump?.id) {
        val request = jump ?: return@LaunchedEffect
        if (itemIndex < 0) {
            land(request.gameId)
            return@LaunchedEffect
        }
        val index = itemIndex + headerCount
        snapshotFlow { state.layoutInfo.totalItemsCount }.first { it > index }
        withTimeoutOrNull(JUMP_SETTLE_WAIT_MS) { snapshotFlow { settled() }.first { it } }
        withFrameNanos { }
        val token = if (request.spin) motion.listSpin else motion.listJump
        val run = launch { state.scrollToJump(index, token, request.spin) }
        landing.landNow = { run.cancel() }
        run.join()
        landing.landNow = null
        if (run.isCancelled) state.scrollToItem(index)
        land(request.gameId)
    }
    return landing
}

/**
 * Spins a carousel to [jump]'s game, stepping its center on one eased curve, or just recenters
 * for a plain jump. [stepTo] moves the center and [setSpinning] holds Now and focus until landing.
 */
@Composable
internal fun LibraryShelfJump(
    jump: LibraryJump?,
    center: Int,
    targetIndex: Int,
    size: Int,
    landing: LibraryJumpLanding,
    stepTo: (Int) -> Unit,
    setSpinning: (Boolean) -> Unit,
    onLanded: (String) -> Unit,
) {
    val token = JarnTheme.motion.listSpin
    val land by rememberUpdatedState(onLanded)
    val from by rememberUpdatedState(center)
    LaunchedEffect(jump?.id) {
        val request = jump ?: return@LaunchedEffect
        if (targetIndex < 0) {
            land(request.gameId)
            return@LaunchedEffect
        }
        if (!request.spin || token.durationMillis == 0) {
            stepTo(targetIndex)
            land(request.gameId)
            return@LaunchedEffect
        }
        val steps = libraryShelfSpinSteps(from, targetIndex, size)
        val times = libraryShelfSpinTimes(abs(steps), token.durationMillis, token.easing)
        setSpinning(true)
        val run = launch {
            var elapsed = 0L
            var at = from
            times.forEach { time ->
                delay(time - elapsed)
                elapsed = time
                at = shelfStep(at, steps.sign, size)
                stepTo(at)
            }
        }
        landing.landNow = { run.cancel() }
        run.join()
        landing.landNow = null
        stepTo(targetIndex)
        setSpinning(false)
        land(request.gameId)
    }
}
