package jr.brian.jarnfe.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

/**
 * Every visible game across systems, in name order. One line above the games names the focused
 * game's system, so a game that exists in two systems is never ambiguous in either view.
 */
@Composable
internal fun LibraryAllGames(library: JarnLibraryStore, emptyActions: @Composable () -> Unit) {
    LaunchedEffect(library) { library.wantAllGames() }
    val state = library.state
    Column(Modifier.fillMaxSize()) {
        AllGamesFocusLine(state)
        Box(Modifier.weight(1f)) {
            LibraryGames(
                feed = state.allGames,
                focusedKey = state.focusedAllGame?.id,
                onFocused = { id -> library.update { focusAllGame(id) } },
                library = library,
                empty = { LibraryEmptyMessage(AllGamesEmptyCopy, emptyActions) },
            )
        }
    }
}

@Composable
private fun AllGamesFocusLine(state: JarnLibraryState) {
    val line = allGamesFocusLine(state) ?: return
    val position = stringResource(R.string.jarnfe_library_game_position, line.position, line.total)
    JarnText(
        text = stringResource(R.string.jarnfe_library_system_line, line.systemTitle, position),
        style = JarnTheme.type.caption,
        color = JarnTheme.colors.textMid,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = JarnTheme.space.stageMargin),
    )
}

private val AllGamesEmptyCopy =
    LibraryEmptyCopy(R.string.jarnfe_empty_all_games_title, R.string.jarnfe_library_no_all_games_message)

internal data class AllGamesFocus(val systemTitle: String, val position: Int, val total: Int)

/** Null until the focused game and its system are both known. */
internal fun allGamesFocusLine(state: JarnLibraryState): AllGamesFocus? {
    val games = state.allGames.items
    val game = state.focusedAllGame ?: return null
    val system = state.systems.items.firstOrNull { it.id == game.systemId } ?: return null
    return AllGamesFocus(system.title, games.indexOf(game) + 1, games.size)
}
