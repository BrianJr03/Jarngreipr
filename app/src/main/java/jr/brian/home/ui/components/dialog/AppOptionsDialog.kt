package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.apps.AppOptionsMenuContent
import jr.brian.home.ui.components.apps.rememberAppOptionsMenuFocusRequesters
import jr.brian.home.ui.theme.OledCardColor

@Composable
fun AppOptionsDialog(
    app: AppInfo,
    currentDisplayPreference: DisplayPreference,
    onDismiss: () -> Unit,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    hasExternalDisplay: Boolean = false,
    currentIconSize: Float = 64f,
    onIconSizeChange: (Float) -> Unit = {},
    showResizeOption: Boolean = false,
    onHideApp: () -> Unit = {},
    onCustomIconClick: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.widget_page_app_options_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.dialog_cancel),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        text = {
            val focusRequesters = rememberAppOptionsMenuFocusRequesters(
                hasResizeOption = showResizeOption,
                hasExternalDisplay = hasExternalDisplay
            )
            var focusedIndex by remember { mutableIntStateOf(0) }

            AppOptionsMenuContent(
                appLabel = "", // Already displayed in title
                currentDisplayPreference = currentDisplayPreference,
                onAppInfoClick = onAppInfoClick,
                onDisplayPreferenceChange = onDisplayPreferenceChange,
                hasExternalDisplay = hasExternalDisplay,
                focusRequesters = focusRequesters,
                onFocusedIndexChange = { focusedIndex = it },
                onDismiss = onDismiss,
                app = if (showResizeOption) app else null,
                currentIconSize = currentIconSize,
                onIconSizeChange = onIconSizeChange,
                onToggleVisibility = onHideApp,
                onCustomIconClick = onCustomIconClick
            )

        },
        confirmButton = {}
    )
}
