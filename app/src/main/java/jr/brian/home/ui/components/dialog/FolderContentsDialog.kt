package jr.brian.home.ui.components.dialog

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.data.CustomIconManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.settings.AppName
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.util.launchApp
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.launch

@Composable
fun FolderContentsDialog(
    folderName: String,
    apps: List<AppInfo>,
    folderId: String,
    pageIndex: Int,
    allApps: List<AppInfo> = apps,
    tabType: String = jr.brian.home.data.FolderManager.TAB_TYPE_APPS,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customIconManager = LocalCustomIconManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val folderManager = LocalFolderManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var editableName by remember { mutableStateOf(folderName) }
    var showEditAppsDialog by remember { mutableStateOf(false) }
    var folderAppPackages by remember { mutableStateOf(apps.map { it.packageName }.toSet()) }
    var selectedAppForOptions by remember { mutableStateOf<AppInfo?>(null) }
    var appForCustomIcon by remember { mutableStateOf<AppInfo?>(null) }

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                folderManager.deleteFolder(pageIndex, folderId, tabType)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.folder_delete),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    BasicTextField(
                        value = editableName,
                        onValueChange = { newName ->
                            editableName = newName
                            scope.launch {
                                folderManager.renameFolder(pageIndex, folderId, newName, tabType)
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(ThemePrimaryColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    Row {
                        IconButton(
                            onClick = { showEditAppsDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit folder apps",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.dialog_app_visibility_close),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val currentFolderApps = allApps.filter { it.packageName in folderAppPackages }

                Text(
                    text = "${currentFolderApps.size} apps",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (currentFolderApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.folder_empty),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(currentFolderApps, key = { it.packageName }) { app ->
                            FolderAppItem(
                                app = app,
                                customIconManager = customIconManager,
                                showAppNames = appVisibilityManager.showAppNames,
                                onClick = {
                                    val displayPreference = if (hasExternalDisplay) {
                                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                                    } else {
                                        DisplayPreference.CURRENT_DISPLAY
                                    }
                                    launchApp(
                                        context = context,
                                        packageName = app.packageName,
                                        displayPreference = displayPreference
                                    )
                                    onDismiss()
                                },
                                onLongClick = {
                                    selectedAppForOptions = app
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditAppsDialog) {
        AppVisibilityDialog(
            apps = allApps,
            onDismiss = { showEditAppsDialog = false },
            pageIndex = pageIndex,
            isWidgetTabMode = true,
            visibleAppsOverride = folderAppPackages,
            onToggleAppOverride = { packageName ->
                val newPackages = if (packageName in folderAppPackages) {
                    folderAppPackages - packageName
                } else {
                    folderAppPackages + packageName
                }
                folderAppPackages = newPackages
                scope.launch {
                    if (newPackages.isEmpty()) {
                        folderManager.deleteFolder(pageIndex, folderId, tabType)
                        showEditAppsDialog = false
                        onDismiss()
                    } else {
                        folderManager.updateFolderApps(pageIndex, folderId, newPackages.toList(), tabType)
                    }
                }
            },
            onShowAllOverride = {
                val newPackages = allApps.map { it.packageName }.toSet()
                folderAppPackages = newPackages
                scope.launch {
                    folderManager.updateFolderApps(pageIndex, folderId, newPackages.toList(), tabType)
                }
            },
            onHideAllOverride = {
                folderAppPackages = emptySet()
                scope.launch {
                    folderManager.deleteFolder(pageIndex, folderId, tabType)
                    showEditAppsDialog = false
                    onDismiss()
                }
            }
        )
    }

    if (selectedAppForOptions != null) {
        val app = selectedAppForOptions!!
        AppOptionsDialog(
            app = app,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                app.packageName
            ),
            onDismiss = { selectedAppForOptions = null },
            onAppInfoClick = {
                openAppInfo(context, app.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    app.packageName,
                    preference
                )
            },
            hasExternalDisplay = hasExternalDisplay,
            onHideApp = {
                val newPackages = folderAppPackages - app.packageName
                folderAppPackages = newPackages
                scope.launch {
                    if (newPackages.isEmpty()) {
                        folderManager.deleteFolder(pageIndex, folderId, tabType)
                        selectedAppForOptions = null
                        onDismiss()
                    } else {
                        folderManager.updateFolderApps(pageIndex, folderId, newPackages.toList(), tabType)
                    }
                }
                selectedAppForOptions = null
            },
            onCustomIconClick = {
                appForCustomIcon = app
                selectedAppForOptions = null
            }
        )
    }

    if (appForCustomIcon != null) {
        CustomIconDialog(
            packageName = appForCustomIcon!!.packageName,
            appLabel = appForCustomIcon!!.label,
            onDismiss = { appForCustomIcon = null },
            onIconChanged = { }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderAppItem(
    app: AppInfo,
    customIconManager: CustomIconManager,
    showAppNames: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
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
                OledCardLightColor.copy(alpha = 0.3f),
                OledCardColor.copy(alpha = 0.3f),
            )
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .focusable()
            .padding(8.dp)
    ) {
        AppIconImage(
            defaultIcon = app.icon,
            packageName = app.packageName,
            contentDescription = stringResource(R.string.app_icon_description, app.label),
            customIconManager = customIconManager,
            modifier = Modifier.size(56.dp)
        )

        if (showAppNames) {
            Spacer(modifier = Modifier.height(4.dp))
            app.AppName()
        }
    }
}
