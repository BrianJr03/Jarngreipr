package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.wallpaper.JarnWallpaperGate
import kotlinx.coroutines.delay

/**
 * Follows [value] once its key has rested for [settleMillis]. New data for the same key passes at
 * once, and so does any change [skipWait] allows.
 */
@Composable
internal fun <T> rememberSettled(
    value: T,
    settleMillis: Long,
    keyOf: (T) -> Any?,
    skipWait: (held: T, next: T) -> Boolean = { _, _ -> false },
): T {
    var settled by remember { mutableStateOf(value) }
    val latest by rememberUpdatedState(value)
    val key = keyOf(value)
    val isImmediate = key == keyOf(settled) || skipWait(settled, value)
    if (isImmediate) SideEffect { settled = value }
    LaunchedEffect(key, isImmediate) {
        if (isImmediate) return@LaunchedEffect
        delay(settleMillis)
        settled = latest
    }
    return if (isImmediate) value else settled
}

/**
 * The selection Now shows. Scrolling within a list waits to rest, while entering or leaving a list
 * and a game the actions controller already moved to change right away.
 */
@Composable
internal fun rememberSettledSelection(selection: LibrarySelection): LibrarySelection {
    val actionsGameId = JarnGameActions.gameId
    return rememberSettled(
        value = selection,
        settleMillis = JarnTheme.motion.artDebounceMillis.toLong(),
        keyOf = { it.focusKey() },
        skipWait = { held, next ->
            (held::class != next::class && next !is LibrarySelection.None) ||
                (next is LibrarySelection.Game && next.game.id == actionsGameId)
        },
    )
}

/** False while a game is up. Art holders and Now queries keep what they have until it clears. */
@Composable
internal fun rememberJarnArtCanLoad(): Boolean = JarnWallpaperGate.allowsArtWork()

/** Pass a settled system. The result never holds another system's shelf. */
@Composable
internal fun rememberSystemNow(
    systemId: String,
    canLoad: Boolean = rememberJarnArtCanLoad(),
): JarnSystemNow {
    val source = JarnLibrary.source
    val now by produceState<JarnSystemNow?>(null, source, systemId, canLoad) {
        if (canLoad) source.observeSystemNow(systemId).collect { value = it }
    }
    return now?.takeIf { it.systemId == systemId } ?: JarnSystemNow(systemId)
}

/** Pass a settled game. Holds the last path while the next resolves, for [LibraryHeldArt]. */
@Composable
internal fun rememberGameWideArt(
    game: JarnLibraryGame,
    canLoad: Boolean = rememberJarnArtCanLoad(),
): String? {
    val source = JarnLibrary.source
    val path by produceState<String?>(null, source, game.id, canLoad) {
        if (canLoad) source.observeWideArt(game.id).collect { value = it }
    }
    return path
}
