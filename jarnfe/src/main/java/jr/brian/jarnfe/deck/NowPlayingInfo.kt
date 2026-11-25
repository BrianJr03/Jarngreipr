package jr.brian.jarnfe.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import kotlinx.coroutines.delay

/** The Library entry for the launched game, or a stand-in built from the launch when it has none. */
@Composable
internal fun rememberNowPlayingLibraryGame(playing: NowPlayingGame): JarnLibraryGame {
    val source = JarnLibrary.source
    val found by produceState<JarnLibraryGame?>(null, source, playing) {
        source.observeGames(playing.systemId).collect { feed ->
            value = feed.items.firstOrNull { matchesNowPlaying(it, playing) }
        }
    }
    return found ?: remember(playing) {
        JarnLibraryGame(
            id = "${playing.systemId}:${playing.path}",
            systemId = playing.systemId,
            title = playing.title,
            artPath = playing.artworkPath,
        )
    }
}

@Composable
internal fun nowPlayingSystemTitle(systemId: String): String =
    JarnLibrary.state.systems.items.firstOrNull { it.id == systemId }?.title ?: systemId

/**
 * Wall time now, refreshed each time a whole minute has passed since [anchorMs], so a session's
 * HH:MM swaps exactly once a minute, on its own minute.
 */
@Composable
internal fun rememberMinuteClock(anchorMs: Long = 0L): Long {
    var now by remember(anchorMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(anchorMs) {
        while (true) {
            delay(nextMinuteDelayMs(anchorMs, now))
            now = System.currentTimeMillis()
        }
    }
    return now
}

/**
 * Dim state for one page. [wake] lifts it and restarts the delay. [dimmedSinceMs] is null while
 * awake, so the page can tell a long dim from a fresh one.
 */
internal class NowPlayingDimState {
    var lastWakeMs by mutableLongStateOf(System.currentTimeMillis())
        private set
    var dimmedSinceMs by mutableStateOf<Long?>(null)
        private set

    val isDimmed: Boolean get() = dimmedSinceMs != null

    fun wake(nowMs: Long = System.currentTimeMillis()) {
        lastWakeMs = nowMs
        dimmedSinceMs = null
    }

    fun check(dim: JarnPlayingDim, nowMs: Long = System.currentTimeMillis()) {
        if (dimmedSinceMs == null && nowPlayingDimmed(dim, lastWakeMs, nowMs)) dimmedSinceMs = nowMs
    }
}

/** Dims after [dim]'s delay from the last wake. Never dims for [JarnPlayingDim.Never]. */
@Composable
internal fun rememberNowPlayingDim(dim: JarnPlayingDim): NowPlayingDimState {
    val state = remember { NowPlayingDimState() }
    LaunchedEffect(dim, state.lastWakeMs) {
        val delayMs = dim.delayMs() ?: return@LaunchedEffect
        delay((state.lastWakeMs + delayMs - System.currentTimeMillis()).coerceAtLeast(0L))
        state.check(dim)
    }
    return state
}
