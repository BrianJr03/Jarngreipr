package jr.brian.home.ui.components.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.ui.extensions.handleDPadNavigation
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun AppOptionsMenu(
    appLabel: String,
    currentDisplayPreference: AppDisplayPreferenceManager.DisplayPreference,
    onDismiss: () -> Unit,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (AppDisplayPreferenceManager.DisplayPreference) -> Unit,
    hasExternalDisplay: Boolean = false
) {
    val optionCount = if (hasExternalDisplay) 3 else 1
    val focusRequesters = remember {
        List(optionCount) { FocusRequester() }
    }
    var focusedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

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
                appLabel,
                currentDisplayPreference,
                onAppInfoClick,
                onDisplayPreferenceChange,
                hasExternalDisplay,
                focusRequesters,
                onFocusedIndexChange = { focusedIndex = it },
                onDismiss
            )
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = OledCardColor,
        shape = RoundedCornerShape(16.dp),
    )
}