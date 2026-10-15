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
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsContent(
        state = state,
        onToggleFavoritesOnTop = viewModel::setFavoritesOnTop,
        onToggleShowHidden = viewModel::setShowHidden,
        onToggleGamelistDecoration = viewModel::setGamelistDecoration,
        onScrapeAll = {
            jr.brian.frontend.scrape.ScrapeWorker.enqueueAll(context.applicationContext)
        },
        onSwapScreens = viewModel::swapScreens,
        onBack = onBack,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onToggleFavoritesOnTop: (Boolean) -> Unit,
    onToggleShowHidden: (Boolean) -> Unit,
    onToggleGamelistDecoration: (Boolean) -> Unit,
    onScrapeAll: () -> Unit,
    onSwapScreens: () -> Unit,
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
                item {
                    ActionRow(
                        label = "Scrape all now",
                        description = "Kicks a background scrape of every scanned game. " +
                            "You can keep using the launcher while it runs.",
                        onClick = onScrapeAll,
                    )
                }
                item { SectionHeader("Dual-screen") }
                item {
                    ActionRow(
                        label = "Swap screens",
                        description = "Move the frontend to the other display and the launcher " +
                            "to this one. Persisted — survives reboots and Home presses.",
                        onClick = onSwapScreens,
                    )
                }
                item { SectionHeader("Appearance · Input") }
                item {
                    PlaceholderRow(
                        label = "Theme, focus animation",
                        description = "Settings for these groups land in later commits.",
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
private fun ActionRow(label: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().focusable(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
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
