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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.ui.extensions.handleDPadNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun AppOptionsMenuContent(
    appLabel: String,
    currentDisplayPreference: DisplayPreference,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    hasExternalDisplay: Boolean,
    focusRequesters: List<FocusRequester>,
    onFocusedIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (appLabel.isNotEmpty()) {
            Text(
                text = appLabel,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        MenuOption(
            icon = Icons.Default.Info,
            label = stringResource(id = R.string.app_options_app_info),
            focusRequester = focusRequesters[0],
            onClick = {
                onAppInfoClick()
                onDismiss()
            },
            onNavigateUp = {
                // Stay on first item
            },
            onNavigateDown = {
                if (hasExternalDisplay && focusRequesters.size > 1) {
                    focusRequesters[1].requestFocus()
                    onFocusedIndexChange(1)
                }
            },
            onFocusChanged = { focused ->
                if (focused)
                    onFocusedIndexChange(0)
            }
        )

        if (hasExternalDisplay) {
            MenuOption(
                icon = Icons.AutoMirrored.Filled.ScreenShare,
                label = stringResource(id = R.string.app_options_launch_external),
                isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                focusRequester = focusRequesters[1],
                onClick = {
                    onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                    onDismiss()
                },
                onNavigateUp = {
                    focusRequesters[0].requestFocus()
                    onFocusedIndexChange(0)
                },
                onNavigateDown = {
                    focusRequesters[2].requestFocus()
                    onFocusedIndexChange(2)
                },
                onFocusChanged = { focused ->
                    if (focused) onFocusedIndexChange(1)
                }
            )

            MenuOption(
                icon = Icons.AutoMirrored.Filled.ScreenShare,
                label = stringResource(id = R.string.app_options_launch_primary),
                isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                focusRequester = focusRequesters[2],
                onClick = {
                    onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                    onDismiss()
                },
                onNavigateUp = {
                    focusRequesters[1].requestFocus()
                    onFocusedIndexChange(1)
                },
                onNavigateDown = {
                    // Stay on last item
                },
                onFocusChanged = { focused ->
                    if (focused) onFocusedIndexChange(2)
                }
            )
        }
    }
}

@Composable
private fun MenuOption(
    icon: ImageVector,
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    var isFocused by remember { mutableIntStateOf(0) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = when {
                    isFocused == 1 -> Color.White.copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = if (it.isFocused) 1 else 0
                onFocusChanged(it.isFocused)
            }
            .handleDPadNavigation(
                onNavigateUp = onNavigateUp,
                onNavigateDown = onNavigateDown,
                onEnterPress = onClick
            )
            .focusable(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            val color = if (isSelected) ThemePrimaryColor else Color.White
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                color = color,
                fontSize = 16.sp
            )
        }
    }
}