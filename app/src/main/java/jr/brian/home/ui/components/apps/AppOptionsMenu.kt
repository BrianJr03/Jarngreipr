package jr.brian.home.ui.components.apps

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.util.rememberHasExternalDisplay

@Composable
fun AppOptionsMenu(
    appLabel: String,
    currentDisplayPreference: DisplayPreference,
    app: AppInfo? = null,
    isInDock: Boolean = false,
    currentIconSize: Float = 64f,
    onDismiss: () -> Unit,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    onIconSizeChange: (Float) -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onCustomIconClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onRemoveFromDock: () -> Unit = {}
) {
    val hasExternalDisplay = rememberHasExternalDisplay()
    val focusRequesters = rememberAppOptionsMenuFocusRequesters(
        hasResizeOption = app != null,
        hasExternalDisplay = hasExternalDisplay,
        isInDock = isInDock
    )
    var focusedIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.app_options_menu_title),
                color = Color.White,
            )
        },
        text = {
            AppOptionsMenuContent(
                appLabel = appLabel,
                currentDisplayPreference = currentDisplayPreference,
                hasExternalDisplay = hasExternalDisplay,
                focusRequesters = focusRequesters,
                app = app,
                currentIconSize = currentIconSize,
                isInDock = isInDock,
                onDismiss = onDismiss,
                onAppInfoClick = onAppInfoClick,
                onFocusedIndexChange = { focusedIndex = it },
                onDisplayPreferenceChange = onDisplayPreferenceChange,
                onIconSizeChange = onIconSizeChange,
                onToggleVisibility = onToggleVisibility,
                onCustomIconClick = onCustomIconClick,
                onRenameClick = onRenameClick,
                onRemoveFromDock = onRemoveFromDock
            )
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = OledCardColor,
        shape = RoundedCornerShape(16.dp),
    )
}