package jr.brian.home.esde.ui.frontend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor

@Composable
fun FrontendSettingsDialog(onDismiss: () -> Unit) {
    val prefsManager = LocalESDEPreferencesManager.current
    val state by prefsManager.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        title = { DialogTitle() },
        text = {
            FrontendSettingsContent(
                systemLayout = state.systemLayout,
                gameLayout = state.gameLayout,
                onSystemLayoutChange = prefsManager::setSystemLayout,
                onGameLayoutChange = prefsManager::setGameLayout
            )
        },
        confirmButton = { DialogCloseButton(onClick = onDismiss) },
        dismissButton = {}
    )
}

@Composable
private fun DialogTitle() {
    Text(
        text = stringResource(R.string.frontend_settings_title),
        color = Color.White.copy(alpha = 0.9f),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DialogCloseButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(R.string.frontend_settings_close),
            color = ThemeAccentColor
        )
    }
}

@Composable
private fun FrontendSettingsContent(
    systemLayout: FrontendLayout,
    gameLayout: FrontendLayout,
    onSystemLayoutChange: (FrontendLayout) -> Unit,
    onGameLayoutChange: (FrontendLayout) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleSetting(
            title = stringResource(R.string.frontend_layout_systems_row_title),
            description = stringResource(R.string.frontend_layout_systems_row_description),
            checked = systemLayout == FrontendLayout.Row,
            onCheckedChange = {
                onSystemLayoutChange(if (it) FrontendLayout.Row else FrontendLayout.Grid)
            }
        )
        ToggleSetting(
            title = stringResource(R.string.frontend_layout_games_row_title),
            description = stringResource(R.string.frontend_layout_games_row_description),
            checked = gameLayout == FrontendLayout.Row,
            onCheckedChange = {
                onGameLayoutChange(if (it) FrontendLayout.Row else FrontendLayout.Grid)
            }
        )
    }
}
