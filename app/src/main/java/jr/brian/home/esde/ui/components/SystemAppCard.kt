package jr.brian.home.esde.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.model.SystemLaunchTrigger
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.components.settings.displayName
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalCustomIconManager

@Composable
internal fun SystemAppCard(
    systemFolderName: String,
    packageName: String?,
    allApps: List<AppInfo>,
    launchTrigger: SystemLaunchTrigger,
    bottomScreenEnabled: Boolean,
    onLaunchTriggerChanged: (SystemLaunchTrigger) -> Unit,
    onBottomScreenToggle: () -> Unit,
    onChangeClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    val matchedApp = remember(packageName, allApps) {
        if (packageName != null) allApps.find { it.packageName == packageName } else null
    }
    val appLabel = matchedApp?.displayName()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(10.dp)
    ) {
        // Header row: system name + action icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = systemFolderName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onChangeClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.esde_system_apps_change),
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { showRemoveDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.esde_system_apps_remove),
                    tint = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // App info
        if (appLabel != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(20.dp)) {
                    val customIconManager = LocalCustomIconManager.current
                    AppIconImage(
                        defaultIcon = matchedApp.icon,
                        packageName = matchedApp.packageName,
                        contentDescription = null,
                        customIconManager = customIconManager,
                        shape = RectangleShape,
                        modifier = Modifier.matchParentSize()
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = appLabel,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Launch trigger dropdown
            LaunchTriggerDropdown(
                selectedTrigger = launchTrigger,
                onTriggerSelected = onLaunchTriggerChanged
            )

            Spacer(modifier = Modifier.height(2.dp))

            CompactToggleRow(
                label = stringResource(R.string.esde_system_apps_bottom_screen),
                checked = bottomScreenEnabled,
                onCheckedChange = { onBottomScreenToggle() }
            )
        } else {
            Text(
                text = stringResource(R.string.esde_system_apps_no_app_assigned),
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = OledCardColor,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.7f),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(text = stringResource(R.string.esde_system_apps_remove_confirm_title, systemFolderName))
            },
            text = {
                Text(text = stringResource(R.string.esde_system_apps_remove_confirm_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemoveClick()
                }) {
                    Text(
                        text = stringResource(R.string.esde_system_apps_remove_confirm_yes),
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(
                        text = stringResource(R.string.esde_system_apps_remove_confirm_no),
                        color = ThemePrimaryColor
                    )
                }
            }
        )
    }
}

@Composable
private fun LaunchTriggerDropdown(
    selectedTrigger: SystemLaunchTrigger,
    onTriggerSelected: (SystemLaunchTrigger) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val triggerLabel = when (selectedTrigger) {
        SystemLaunchTrigger.NoAction -> stringResource(R.string.esde_system_apps_trigger_no_action)
        SystemLaunchTrigger.GameStart -> stringResource(R.string.esde_system_apps_trigger_game_start)
        SystemLaunchTrigger.GameSelect -> stringResource(R.string.esde_system_apps_trigger_game_select)
        SystemLaunchTrigger.SystemSelect -> stringResource(R.string.esde_system_apps_trigger_system_select)
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.esde_system_apps_launch_trigger),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                Text(
                    text = triggerLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = OledCardColor
        ) {
            SystemLaunchTrigger.entries.forEach { trigger ->
                val label = when (trigger) {
                    SystemLaunchTrigger.NoAction -> stringResource(R.string.esde_system_apps_trigger_no_action)
                    SystemLaunchTrigger.GameStart -> stringResource(R.string.esde_system_apps_trigger_game_start)
                    SystemLaunchTrigger.GameSelect -> stringResource(R.string.esde_system_apps_trigger_game_select)
                    SystemLaunchTrigger.SystemSelect -> stringResource(R.string.esde_system_apps_trigger_system_select)
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (trigger == selectedTrigger) FontWeight.Bold else FontWeight.Normal,
                            color = if (trigger == selectedTrigger) ThemePrimaryColor else Color.White
                        )
                    },
                    onClick = {
                        onTriggerSelected(trigger)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.55f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThemePrimaryColor,
                checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
            )
        )
    }
}
