package jr.brian.jarnfe.systems

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.jarnfe.R

private const val SHOW_ALL_KEY = "show_all_hidden_games"

/** Opens the list of games hidden in this system. */
@Composable
internal fun HiddenGamesRow(hiddenCount: Int, onOpen: () -> Unit) {
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_systems_hidden_games),
        supporting = pluralStringResource(R.plurals.jarnfe_systems_hidden_games_count, hiddenCount, hiddenCount),
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A press of A on a game shows it again. The page stays open so several can be shown in a row. */
@Composable
internal fun SystemHiddenGamesPage(
    system: JarnManagedSystem,
    manager: JarnSystemManager,
    onBack: () -> Unit,
) {
    val flow = remember(system.id) { manager.hiddenGames(system.id) }
    val games by flow.collectAsState(initial = null)
    val showAll = stringResource(R.string.jarnfe_systems_show_all_hidden_games)
    val rows = games?.let { hidden -> hiddenGameRows(hidden, showAll, manager) }
    SystemPickerPage(
        title = stringResource(R.string.jarnfe_systems_hidden_games_title, system.title),
        rows = rows,
        focusKey = system.id to rows?.size,
        emptyRes = R.string.jarnfe_systems_no_hidden_games,
        onBack = onBack,
        supporting = stringResource(R.string.jarnfe_systems_hidden_games_hint).takeIf { !rows.isNullOrEmpty() },
    )
}

private fun hiddenGameRows(
    hidden: List<JarnHiddenGame>,
    showAllLabel: String,
    manager: JarnSystemManager,
): List<SystemPickerRow> {
    if (hidden.isEmpty()) return emptyList()
    val games = hidden.map { game -> SystemPickerRow(game.key, game.title) { manager.showGames(listOf(game.key)) } }
    if (hidden.size == 1) return games
    return listOf(SystemPickerRow(SHOW_ALL_KEY, showAllLabel) { manager.showGames(hidden.map { it.key }) }) + games
}
