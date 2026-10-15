package jr.brian.frontend.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.ui.input.GamepadAction
import jr.brian.frontend.ui.input.handleGamepad

/**
 * Games list for one system. Tapping a tile goes to Detail; B goes
 * back to Home.
 *
 * Minimum-viable grid for Phase 3d — chip-bar sort / filter and the
 * details pane live in follow-up Phase 3 commits once the design
 * tokens arrive.
 */
@Composable
fun GamesScreen(
    systemId: String,
    onGameSelected: (GameEntity) -> Unit,
    onBack: () -> Unit,
    viewModel: GamesViewModel = hiltViewModel(),
) {
    LaunchedEffect(systemId) { viewModel.bind(systemId) }
    val state by viewModel.uiState.collectAsState()
    GamesContent(state = state, onGameSelected = onGameSelected, onBack = onBack)
}

@Composable
fun GamesContent(
    state: GamesUiState,
    onGameSelected: (GameEntity) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .handleGamepad { action ->
                if (action == GamepadAction.Back) {
                    onBack(); true
                } else false
            },
        color = MaterialTheme.colorScheme.background,
    ) {
        when (state) {
            GamesUiState.Loading -> Loading()
            is GamesUiState.Loaded -> Loaded(state, onGameSelected)
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading…", color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Loaded(state: GamesUiState.Loaded, onGameSelected: (GameEntity) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.system?.displayName ?: "Games",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (state.games.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No games scanned for this system yet.",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(state.games, key = { it.id }) { game ->
                    GameListTile(game = game, onClick = { onGameSelected(game) })
                }
            }
        }
    }
}

@Composable
private fun GameListTile(game: GameEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .focusable(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Text(
                text = game.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}
