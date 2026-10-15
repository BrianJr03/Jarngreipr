package jr.brian.frontend.ui.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.frontend.ui.input.GamepadAction
import jr.brian.frontend.ui.input.handleGamepad

/**
 * Settings — one list of grouped rows per plan §3.2 (≤5 groups:
 * Library, Appearance, Input, Scraping, Dual-screen). Phase 3f ships
 * the shell + three live toggles that have real values in
 * ESDEPreferencesManager; the rest render as placeholder rows the
 * follow-up phases populate.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SettingsContent(
        state = state,
        onToggleFavoritesOnTop = viewModel::setFavoritesOnTop,
        onToggleShowHidden = viewModel::setShowHidden,
        onToggleGamelistDecoration = viewModel::setGamelistDecoration,
        onBack = onBack,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onToggleFavoritesOnTop: (Boolean) -> Unit,
    onToggleShowHidden: (Boolean) -> Unit,
    onToggleGamelistDecoration: (Boolean) -> Unit,
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
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SectionHeader("Library") }
                item {
                    ToggleRow(
                        label = "Show favorites first on Home",
                        checked = state.favoritesOnTop,
                        onChange = onToggleFavoritesOnTop,
                    )
                }
                item {
                    ToggleRow(
                        label = "Show hidden games",
                        checked = state.showHidden,
                        onChange = onToggleShowHidden,
                    )
                }
                item { SectionHeader("Scraping") }
                item {
                    ToggleRow(
                        label = "Read ES-DE gamelist.xml metadata",
                        checked = state.gamelistDecoration,
                        onChange = onToggleGamelistDecoration,
                    )
                }
                item { SectionHeader("Appearance · Input · Dual-screen") }
                item {
                    PlaceholderRow(
                        label = "Theme, focus animation, screen swap",
                        description = "Settings for these groups land in later Phase 3 commits.",
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().focusable(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        onClick = { onChange(!checked) },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun PlaceholderRow(label: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
