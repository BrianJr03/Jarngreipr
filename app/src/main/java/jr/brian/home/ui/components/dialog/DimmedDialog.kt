package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType

/**
 * A dialog wrapper that applies ESDE dimming overlay behind the dialog content.
 * This ensures dialogs respect the global dimming settings.
 */
@Composable
fun DimmedDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val wallpaperManager = LocalWallpaperManager.current
    val esdePreferencesManager = LocalESDEPreferencesManager.current
    val esdePrefsState by esdePreferencesManager.state.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (wallpaperManager.getWallpaperType() == WallpaperType.ESDE
                && esdePrefsState.dimmingLevelFloat > 0f
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = esdePrefsState.dimmingLevelFloat))
                )
            }
            content()
        }
    }
}
