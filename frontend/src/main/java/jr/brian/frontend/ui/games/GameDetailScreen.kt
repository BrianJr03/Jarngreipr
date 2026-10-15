package jr.brian.frontend.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.ui.input.GamepadAction
import jr.brian.frontend.ui.input.handleGamepad

/**
 * Game detail. Shows the row's text metadata and a Launch button.
 * Launch posts to the bus so :app's existing RomGameLauncher
 * handles the intent build (plan §Phase 4 migrates launch into
 * :frontend).
 */
@Composable
fun GameDetailScreen(
    gameId: Long,
    onBack: () -> Unit,
    viewModel: GamesViewModel = hiltViewModel(),
) {
    val game by produceState<GameEntity?>(initialValue = null, key1 = gameId) {
        value = viewModel.findGame(gameId)
    }
    GameDetailContent(
        game = game,
        onLaunch = { viewModel.requestLaunch(it) },
        onBack = onBack,
    )
}

@Composable
fun GameDetailContent(
    game: GameEntity?,
    onLaunch: (GameEntity) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .handleGamepad { action ->
                when (action) {
                    GamepadAction.Back -> { onBack(); true }
                    GamepadAction.Confirm -> {
                        game?.let { onLaunch(it) }; true
                    }
                    else -> false
                }
            },
        color = MaterialTheme.colorScheme.background,
    ) {
        if (game == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurface)
            }
            return@Surface
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = game.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            game.developer?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurface)
            }
            game.genre?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurface)
            }
            game.description?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onLaunch(game) }) { Text("Launch") }
        }
    }
}
