package jr.brian.frontend.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.ui.input.GamepadAction
import jr.brian.frontend.ui.input.handleGamepad

/**
 * Search surface — in-window QWERTY on the bottom half, live
 * results on the top half. Replaces the Phase 1 approach where the
 * keyboard rendered in a separate activity on the bottom screen.
 */
@Composable
fun SearchScreen(
    onGameSelected: (GameEntity) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    SearchContent(
        query = query,
        results = results,
        onChar = viewModel::appendChar,
        onBackspace = viewModel::backspace,
        onClear = viewModel::clear,
        onGameSelected = onGameSelected,
        onBack = onBack,
    )
}

@Composable
fun SearchContent(
    query: String,
    results: List<GameEntity>,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
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
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            QueryBar(query)
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)) {
                ResultsList(results, onGameSelected)
            }
            OnScreenKeyboard(onChar = onChar, onBackspace = onBackspace, onClear = onClear)
        }
    }
}

@Composable
private fun QueryBar(query: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = query.ifEmpty { "Type to search…" },
            modifier = Modifier.padding(16.dp),
            color = if (query.isEmpty()) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ResultsList(results: List<GameEntity>, onGameSelected: (GameEntity) -> Unit) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Nothing found",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(results, key = { it.id }) { game ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusable(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                onClick = { onGameSelected(game) },
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = game.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = game.systemId,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
