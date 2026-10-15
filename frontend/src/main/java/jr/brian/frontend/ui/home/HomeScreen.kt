package jr.brian.frontend.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.ui.input.GamepadAction
import jr.brian.frontend.ui.input.handleGamepad

/**
 * Home — Systems grid plus Favorites and Recent rows reading from
 * [HomeViewModel]. Plan §3.2 `ui/home/`.
 *
 * This screen is a minimum-viable version. Hero backdrops, continuous
 * scroll, letter jump, sort chips live in later Phase 3 commits once
 * the real design tokens land on :core-ui.
 */
@Composable
fun HomeScreen(
    onSystemSelected: (SystemEntity) -> Unit,
    onGameSelected: (GameEntity) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onSetup: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    HomeContent(
        state = state,
        onSystemSelected = onSystemSelected,
        onGameSelected = onGameSelected,
        onSearch = onSearch,
        onSettings = onSettings,
        onSetup = onSetup,
    )
}

/** Pure Composable split so previews / future tests can bind state directly. */
@Composable
fun HomeContent(
    state: HomeUiState,
    onSystemSelected: (SystemEntity) -> Unit,
    onGameSelected: (GameEntity) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onSetup: () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .handleGamepad { action ->
                when (action) {
                    GamepadAction.Search -> { onSearch(); true }
                    GamepadAction.Settings -> { onSettings(); true }
                    GamepadAction.Back -> false // Phase 3 leaves back-on-home for 3h
                    else -> false
                }
            },
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state.isEmpty) {
            EmptyLibraryState(onSetup = onSetup)
            return@Surface
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (state.favorites.isNotEmpty()) {
                HomeRow(
                    title = "Favorites",
                    games = state.favorites,
                    onGameSelected = onGameSelected,
                )
            }
            if (state.recent.isNotEmpty()) {
                HomeRow(
                    title = "Recent",
                    games = state.recent,
                    onGameSelected = onGameSelected,
                )
            }
            SystemsGrid(
                systems = state.systems,
                onSystemSelected = onSystemSelected,
            )
        }
    }
}

@Composable
private fun SystemsGrid(
    systems: List<SystemEntity>,
    onSystemSelected: (SystemEntity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Systems",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(systems, key = { it.id }) { system ->
                SystemTile(system = system, onClick = { onSystemSelected(system) })
            }
        }
    }
}

@Composable
private fun SystemTile(system: SystemEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .focusable(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = system.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeRow(
    title: String,
    games: List<GameEntity>,
    onGameSelected: (GameEntity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(games, key = { it.id }) { game ->
                GameTile(game = game, onClick = { onGameSelected(game) })
            }
        }
    }
}

@Composable
private fun GameTile(game: GameEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(140.dp)
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
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(onSetup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No library yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add a ROMs folder to get started.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onSetup) {
            Text("Set up")
        }
    }
}
