package jr.brian.jarnfe.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnGameSort
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.systems.JarnSystems

/**
 * The order of the game's whole system. Each press of A moves to the next order, so it needs no
 * page of its own, and focus stays on the same game as the list reorders behind the Deck.
 */
@Composable
internal fun GameSortRow(game: JarnLibraryGame) {
    val manager = JarnSystems.manager ?: return
    val flow = remember(manager) { manager.gameSorts }
    val sorts by flow.collectAsState(initial = null)
    val current = sorts?.get(game.systemId) ?: JarnGameSort.Default
    JarnValueRow(
        label = stringResource(R.string.jarnfe_sort_row),
        value = stringResource(current.labelRes),
        supporting = stringResource(R.string.jarnfe_sort_row_hint),
        onClick = { if (sorts != null) manager.setGameSort(game.systemId, current.next()) },
    )
}
