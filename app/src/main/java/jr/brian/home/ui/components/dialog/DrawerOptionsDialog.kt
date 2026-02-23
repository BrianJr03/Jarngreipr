package jr.brian.home.ui.components.dialog

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.preferences.WallpaperToggleTarget
import jr.brian.home.esde.setup.SetupPreferences
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.util.MediaPickerLauncher
import jr.brian.home.util.launchApp

@Composable
fun DrawerOptionsDialog(
    onDismiss: () -> Unit,
    onPowerClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onQuickDeleteClick: () -> Unit,
    onCreateFolderClick: (() -> Unit)?,
    onDockSettingsClick: () -> Unit,
    onESDESetupClick: () -> Unit = {},
    onNavigateToSystemApps: () -> Unit = {}
) {
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    val powerSettingsManager = LocalPowerSettingsManager.current
    val setupPreferences = remember { SetupPreferences(context) }
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val isQuickDeleteVisible by powerSettingsManager.quickDeleteVisible.collectAsStateWithLifecycle()
    val esdePreferencesManager = LocalESDEPreferencesManager.current
    val esdePrefsState by esdePreferencesManager.state.collectAsStateWithLifecycle()
    val lastSelectedSystem = esdePrefsState.lastSelectedSystem
    val showWallpaperToggle = esdePrefsState.selectButtonWallpaperToggle
    var isWallpaperExpanded by remember { mutableStateOf(false) }
    val mediaPickerLauncher = MediaPickerLauncher(
        onResult = {
            isWallpaperExpanded = false
            onDismiss()
        }
    )

    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 16.dp)
                .background(
                    color = OledCardColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 2.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.drawer_options_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.drawer_options_close),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                WallpaperOptionsSection(
                    isVisible = isWallpaperExpanded,
                    wallpaperManager = wallpaperManager,
                    setupPreferences = setupPreferences,
                    mediaPickerLauncher = mediaPickerLauncher,
                    onBack = { isWallpaperExpanded = false },
                    onDismiss = onDismiss,
                    onESDESetupClick = onESDESetupClick
                )

                AnimatedVisibility(
                    visible = !isHeaderVisible && !isWallpaperExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickAccessIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.keyboard_label_settings),
                            onClick = {
                                onSettingsClick()
                                onDismiss()
                            }
                        )

                        if (isQuickDeleteVisible) {
                            QuickAccessIconButton(
                                icon = Icons.Default.SdStorage,
                                contentDescription = stringResource(R.string.header_folder_options),
                                onClick = {
                                    onQuickDeleteClick()
                                    onDismiss()
                                }
                            )
                        }

                        QuickAccessIconButton(
                            icon = Icons.Default.Home,
                            contentDescription = stringResource(R.string.drawer_options_tabs),
                            onClick = {
                                onTabsClick()
                                onDismiss()
                            }
                        )

                        if (showWallpaperToggle) {
                            QuickAccessIconButton(
                                icon = Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.header_wallpaper_toggle),
                                onClick = {
                                    val currentType = wallpaperManager.getWallpaperType()
                                    val target = esdePrefsState.wallpaperToggleTarget
                                    if (currentType == WallpaperType.ESDE) {
                                        when (target) {
                                            WallpaperToggleTarget.SystemWallpaper -> wallpaperManager.setTransparent()
                                            WallpaperToggleTarget.SavedImage -> {
                                                val uri = wallpaperManager.savedImageUri
                                                if (uri != null) wallpaperManager.setWallpaper(
                                                    uri = uri,
                                                    type = WallpaperType.IMAGE
                                                )
                                                else wallpaperManager.setTransparent()
                                            }

                                            WallpaperToggleTarget.SavedGif -> {
                                                val uri = wallpaperManager.savedGifUri
                                                if (uri != null) wallpaperManager.setWallpaper(
                                                    uri = uri,
                                                    type = WallpaperType.GIF
                                                )
                                                else wallpaperManager.setTransparent()
                                            }

                                            WallpaperToggleTarget.SavedVideo -> {
                                                val uri = wallpaperManager.savedVideoUri
                                                if (uri != null) wallpaperManager.setWallpaper(
                                                    uri = uri,
                                                    type = WallpaperType.VIDEO
                                                )
                                                else wallpaperManager.setTransparent()
                                            }

                                            WallpaperToggleTarget.Default -> wallpaperManager.setDefault()
                                        }
                                    } else {
                                        wallpaperManager.setESDE()
                                    }
                                    onDismiss()
                                }
                            )
                        }

                        if (isPowerButtonVisible) {
                            QuickAccessIconButton(
                                icon = Icons.Default.PowerSettingsNew,
                                contentDescription = stringResource(R.string.header_power_button),
                                onClick = {
                                    onPowerClick()
                                    onDismiss()
                                }
                            )
                        }

                        QuickAccessIconButton(
                            icon = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.drawer_options_menu),
                            onClick = {
                                onMenuClick()
                                onDismiss()
                            }
                        )

                    }
                }

                AnimatedVisibility(
                    visible = !isWallpaperExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onCreateFolderClick != null) {
                                DrawerOptionButton(
                                    modifier = Modifier.weight(1f),
                                    title = stringResource(R.string.dialog_create_folder_title),
                                    icon = Icons.Default.FolderOpen,
                                    onClick = {
                                        onCreateFolderClick()
                                        onDismiss()
                                    }
                                )
                            }

                            DrawerOptionButton(
                                modifier = Modifier.weight(1f),
                                title = if (isHeaderVisible) {
                                    stringResource(R.string.drawer_options_hide_header)
                                } else {
                                    stringResource(R.string.drawer_options_show_header)
                                },
                                icon = if (isHeaderVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                onClick = {
                                    powerSettingsManager.setHeaderVisibility(!isHeaderVisible)
                                    onDismiss()
                                }
                            )

                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DrawerOptionButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.dock_settings_title),
                                icon = Icons.Default.Dashboard,
                                onClick = {
                                    onDockSettingsClick()
                                    onDismiss()
                                }
                            )


                            DrawerOptionButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.settings_wallpaper_title),
                                icon = Icons.Default.Wallpaper,
                                onClick = {
                                    isWallpaperExpanded = true
                                }
                            )
                        }

                    }
                }

                if (
                    !isWallpaperExpanded
                    && wallpaperManager.getWallpaperType() == WallpaperType.ESDE
                    && lastSelectedSystem != null
                ) {
                    val packageName = esdePrefsState.systemAppMap[lastSelectedSystem]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (packageName != null) {
                            DrawerOptionButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.drawer_options_launch_system_app),
                                onClick = {
                                    launchApp(context, packageName)
                                    onDismiss()
                                }
                            )
                        }

                        DrawerOptionButton(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.drawer_options_configure_system_app),
                            onClick = {
                                onNavigateToSystemApps()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperOptionsSection(
    isVisible: Boolean,
    wallpaperManager: WallpaperManager,
    setupPreferences: SetupPreferences,
    mediaPickerLauncher: ActivityResultLauncher<Array<String>>,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onESDESetupClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WallpaperGridButton(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.drawer_options_back),
                onClick = onBack
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WallpaperGridButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.wallpaper_grid_default),
                    onClick = {
                        wallpaperManager.clearWallpaper()
                        onBack()
                        onDismiss()
                    }
                )

                WallpaperGridButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.wallpaper_grid_system),
                    onClick = {
                        wallpaperManager.setTransparent()
                        onBack()
                        onDismiss()
                    }
                )

                WallpaperGridButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.wallpaper_grid_image),
                    onClick = {
                        val savedUri = wallpaperManager.savedImageUri
                        if (savedUri != null) {
                            wallpaperManager.setWallpaper(savedUri, WallpaperType.IMAGE)
                            onBack()
                            onDismiss()
                        } else {
                            mediaPickerLauncher.launch(arrayOf("image/*"))
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WallpaperGridButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.wallpaper_grid_gif),
                    onClick = {
                        val savedUri = wallpaperManager.savedGifUri
                        if (savedUri != null) {
                            wallpaperManager.setWallpaper(savedUri, WallpaperType.GIF)
                            onBack()
                            onDismiss()
                        } else {
                            mediaPickerLauncher.launch(arrayOf("image/gif"))
                        }
                    }
                )

                WallpaperGridButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.wallpaper_grid_video),
                    onClick = {
                        val savedUri = wallpaperManager.savedVideoUri
                        if (savedUri != null) {
                            wallpaperManager.setWallpaper(savedUri, WallpaperType.VIDEO)
                            onBack()
                            onDismiss()
                        } else {
                            mediaPickerLauncher.launch(arrayOf("video/*"))
                        }
                    }
                )

                WallpaperGridButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.wallpaper_grid_esde),
                    onClick = {
                        onBack()
                        onDismiss()
                        if (setupPreferences.setupCompleted) {
                            wallpaperManager.setESDE()
                        } else {
                            onESDESetupClick()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DrawerOptionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.6f),
                            ThemeSecondaryColor.copy(alpha = 0.4f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickAccessIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val gradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.8f),
                ThemeSecondaryColor.copy(alpha = 0.8f)
            )
        } else {
            listOf(
                OledCardLightColor.copy(alpha = 0.6f),
                OledCardColor.copy(alpha = 0.6f)
            )
        }
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) {
                    Color.White.copy(alpha = 0.8f)
                } else {
                    ThemePrimaryColor.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun WallpaperGridButton(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.4f),
                            ThemeSecondaryColor.copy(alpha = 0.3f)
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
