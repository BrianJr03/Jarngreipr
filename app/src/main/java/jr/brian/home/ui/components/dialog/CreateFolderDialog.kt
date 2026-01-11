package jr.brian.home.ui.components.dialog

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.data.CustomIconManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalCustomIconManager

@Composable
fun CreateFolderDialog(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    pageIndex: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folderManager = LocalFolderManager.current
    val customIconManager = LocalCustomIconManager.current
    val appPositionManager = LocalAppPositionManager.current
    val defaultFolderName = stringResource(R.string.folder_default_name)
    var selectedApps by remember { mutableStateOf(emptySet<String>()) }

    val sortedApps = remember(apps) {
        apps.sortedBy { it.label.lowercase() }
    }

    val allSelected = remember(selectedApps, apps) {
        selectedApps.size == apps.size && apps.isNotEmpty()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(
                    color = OledCardColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dialog_create_folder_title),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(
                                R.string.dialog_create_folder_apps_selected,
                                selectedApps.size
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialog_create_folder_close),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        text = stringResource(
                            if (allSelected) R.string.dialog_create_folder_deselect_all
                            else R.string.dialog_create_folder_select_all
                        ),
                        onClick = {
                            selectedApps = if (allSelected) {
                                emptySet()
                            } else {
                                apps.map { it.packageName }.toSet()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = stringResource(R.string.dialog_create_folder_ok),
                        onClick = {
                            if (selectedApps.isNotEmpty()) {
                                scope.launch {
                                    val firstApp = apps.first { it.packageName in selectedApps }
                                    val firstAppPosition = appPositionManager.getPosition(
                                        pageIndex,
                                        firstApp.packageName
                                    )
                                        ?: AppPosition(
                                            packageName = firstApp.packageName,
                                            x = 100f,
                                            y = 100f,
                                            iconSize = 64f
                                        )

                                    val folder = Folder(
                                        id = UUID.randomUUID().toString(),
                                        name = defaultFolderName,
                                        appPackageNames = selectedApps.toList(),
                                        position = firstAppPosition
                                    )

                                    folderManager.createFolder(pageIndex, folder)

                                    Toast.makeText(
                                        context,
                                        "Folder created with ${selectedApps.size} apps",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedApps, key = { it.packageName }) { app ->
                        AppSelectionItem(
                            app = app,
                            isSelected = app.packageName in selectedApps,
                            customIconManager = customIconManager,
                            onToggle = {
                                selectedApps = if (app.packageName in selectedApps) {
                                    selectedApps - app.packageName
                                } else {
                                    selectedApps + app.packageName
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    customIconManager: CustomIconManager,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.6f),
            )
        } else {
            listOf(
                OledCardLightColor.copy(alpha = 0.5f),
                OledCardColor.copy(alpha = 0.5f),
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(
                    isFocused = isFocused,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .focusable()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                customIconManager = customIconManager,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = app.label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (isSelected) "Selected" else "Not Selected",
                tint = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(animatedRotation(isFocused))
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.7f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )
        } else {
            listOf(
                OledCardLightColor.copy(alpha = 0.7f),
                OledCardColor.copy(alpha = 0.7f),
            )
        }
    )

    Box(
        modifier = modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .focusable()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
        )
    }
}
