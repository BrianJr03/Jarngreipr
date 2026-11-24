package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.StageRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Where Up goes from a game view's first row: the header shortcuts while they are shown. */
internal val LocalNowShortcutUp = staticCompositionLocalOf { FocusRequester.Default }

/**
 * Random and Last Played for a game picked from a list, using the game's own system. Hidden for
 * Continue playing and Search results, where the game isn't being browsed from its system.
 */
internal fun nowGameShowsShortcuts(game: LibrarySelection.Game, route: StageRoute): Boolean =
    !game.resume && route != StageRoute.Search

/** Last Played, unless it is the game already shown. */
internal fun nowGameLastPlayed(game: JarnLibraryGame, systemNow: JarnSystemNow?): JarnLibraryGame? =
    libraryLastPlayed(systemNow?.shelf.orEmpty())?.takeUnless { it.id == game.id }

/** Offers the header shortcuts while a browsed game is on Now, and lets Up from its content reach them. */
@Composable
internal fun NowGameShortcuts(selection: LibrarySelection.Game, systemNow: JarnSystemNow?, content: @Composable () -> Unit) {
    if (!nowGameShowsShortcuts(selection, JarnSession.state.stageRoute)) {
        content()
        return
    }
    val game = selection.game
    val focus = remember { NowSystemFocus() }
    val display = rememberJarnDisplay()
    val lastPlayed = nowGameLastPlayed(game, systemNow?.takeIf { it.systemId == game.systemId })
    val shown = nowShortcuts(gameCount = 1, lastPlayed, display.randomVisible, display.lastPlayedVisible)
    val scope = rememberCoroutineScope()
    val actions = remember(game.id, shown, lastPlayed?.id) {
        NowSystemActions(
            onRandom = spinFromGame(scope, game).takeIf { NowFocusTarget.Random in shown },
            onLastPlayed = lastPlayed?.let { jumpFromGame(it) }.takeIf { NowFocusTarget.LastPlayed in shown },
            onPlay = null,
            onDetails = null,
        )
    }
    SideEffect { focus.actions = actions.buttons }
    PublishNowShortcuts(actions, focus)
    JarnInputLayer(
        enabled = focus.focused == NowFocusTarget.Random || focus.focused == NowFocusTarget.LastPlayed,
        bindings = { nowShortcutHints(focus.focused) },
    )
    CompositionLocalProvider(LocalNowShortcutUp provides (focus.shortcutEntryFor(actions.buttons) ?: FocusRequester.Default)) {
        content()
    }
}

private fun spinFromGame(scope: CoroutineScope, game: JarnLibraryGame): () -> Unit = {
    scope.launch { JarnNowShelf.spin(game.systemId, avoid = game.id) }
}

private fun jumpFromGame(target: JarnLibraryGame): () -> Unit = {
    JarnNowShelf.jumpTo(target.systemId, target.id, spin = false)
}
