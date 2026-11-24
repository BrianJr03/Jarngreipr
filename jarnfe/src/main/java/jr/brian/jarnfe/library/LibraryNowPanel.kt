package jr.brian.jarnfe.library

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.theme.LocalJarnScreenActive
import jr.brian.jarnfe.R
import java.text.DateFormat
import java.util.Date

private const val MINUTES_PER_HOUR = 60L
internal val NowTopZone = 240.dp

@Composable
internal fun LibraryNowPanel(selection: LibrarySelection) {
    val settled = rememberSettledSelection(selection)
    val sceneNow = settled.sceneSystemId()?.let { rememberSystemNow(it) }
    val reveal = rememberNowReveal(settled, followsRoute = ::isRouteChange) { it.focusKey() }
    val shown = reveal.shown
    val shownNow = rememberShownSystemNow(sceneNow, shown.sceneSystemId())
    CompositionLocalProvider(LocalNowRevealGate provides reveal.gate) {
        NowScene(settled, sceneNow) {
            NowShelfReturnLayer(settled) {
                NowFocusKeeper(shown) {
                    NowRevealBox(reveal) {
                        when (shown) {
                            LibrarySelection.None -> JarnEmptyState(
                                title = stringResource(R.string.jarnfe_empty_now_title),
                                message = stringResource(R.string.jarnfe_empty_now_message),
                            )
                            is LibrarySelection.System -> key(shown.system.id) {
                                NowSystemView(shown.system, shownNow)
                            }
                            is LibrarySelection.Game -> NowGameView(shown.game)
                        }
                    }
                }
            }
        }
    }
}

/** A switch between a system and a game, which is mostly the Stage opening a system or going back. */
internal fun isRouteChange(from: LibrarySelection, to: LibrarySelection): Boolean =
    from !is LibrarySelection.None && to !is LibrarySelection.None && from::class != to::class

/**
 * The Stage can change the selection while the controller is on Now. If that removes the focused
 * row, focus lands back inside the panel instead of disappearing from the active screen.
 */
@Composable
private fun NowFocusKeeper(selection: LibrarySelection, content: @Composable () -> Unit) {
    val requester = remember { FocusRequester() }
    val holdsFocus = remember { booleanArrayOf(false) }
    val selectionKey = selection.focusKey()
    val hadFocus = remember(selectionKey) { holdsFocus[0] }
    val isActive = LocalJarnScreenActive.current
    LaunchedEffect(selectionKey) {
        if (!hadFocus || !isActive) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        if (!holdsFocus[0] && JarnNowShelf.pendingCoverId == null) {
            runCatching { requester.requestFocus() }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { holdsFocus[0] = it.hasFocus }
            .focusRequester(requester)
            .focusGroup(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

internal fun LibrarySelection.focusKey(): String? = when (this) {
    LibrarySelection.None -> null
    is LibrarySelection.System -> "system:${system.id}"
    is LibrarySelection.Game -> "game:${game.id}"
}

@Composable
internal fun playSummary(metadata: JarnGameMetadata): String? {
    val playtime = metadata.playtimeMinutes?.takeIf { it > 0 }?.let { playtimeText(it) }
    val lastPlayed = metadata.lastPlayedAtMs?.let {
        stringResource(R.string.jarnfe_library_last_played, formatDate(it))
    }
    return listOfNotNull(playtime, lastPlayed).joinToString(". ").ifEmpty { null }
}

@Composable
private fun playtimeText(minutes: Long): String = if (minutes >= MINUTES_PER_HOUR) {
    stringResource(R.string.jarnfe_library_playtime_hours, minutes / MINUTES_PER_HOUR)
} else {
    stringResource(R.string.jarnfe_library_playtime_minutes, minutes)
}

@Composable
internal fun formatDate(epochMs: Long): String =
    remember(epochMs) { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs)) }
