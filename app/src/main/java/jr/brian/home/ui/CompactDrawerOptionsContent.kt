package jr.brian.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.data.LocalJinglesManager
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.model.WallpaperToggleTarget
import jr.brian.home.esde.ui.video.VideoPresentationManager
import jr.brian.home.esde.viewmodels.ESDEViewModel
import jr.brian.home.ui.components.dialog.DrawerOptionButton
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalBgMusicManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.util.findActivity
import jr.brian.home.util.launchApp

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun CompactDrawerOptionsContent(
    onDismiss: () -> Unit,
    onPowerClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onQuickDeleteClick: () -> Unit,
    onCreateFolderClick: (() -> Unit)?,
    onDockSettingsClick: () -> Unit,
    onESDESetupClick: () -> Unit = {},
    onNavigateToSystemApps: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    val powerSettingsManager = LocalPowerSettingsManager.current
    val bgMusicManager = LocalBgMusicManager.current
    val esdePreferencesManager = LocalESDEPreferencesManager.current
    val esdePrefsState by esdePreferencesManager.state.collectAsStateWithLifecycle()
    val lastSelectedSystem = esdePrefsState.lastSelectedSystem
    val showWallpaperToggle = esdePrefsState.selectButtonWallpaperToggle
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val isQuickDeleteVisible by powerSettingsManager.quickDeleteVisible.collectAsStateWithLifecycle()
    val jinglesManager = LocalJinglesManager.current
    val isMuted by jinglesManager.isMuted.collectAsStateWithLifecycle()
    val esdeViewModel: ESDEViewModel = hiltViewModel(context.findActivity())

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDrawerTile(
                icon = Icons.Default.Dashboard,
                label = stringResource(R.string.dock_settings_title),
                modifier = Modifier.weight(1f),
                onClick = { onDockSettingsClick(); onDismiss() }
            )
            CompactDrawerTile(
                icon = Icons.Default.Home,
                label = stringResource(R.string.drawer_options_tabs),
                modifier = Modifier.weight(1f),
                onClick = { onTabsClick(); onDismiss() }
            )
            CompactDrawerTile(
                icon = Icons.Default.Menu,
                label = stringResource(R.string.drawer_options_menu),
                modifier = Modifier.weight(1f),
                onClick = { onMenuClick(); onDismiss() }
            )
            CompactDrawerTile(
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                label = stringResource(if (isMuted) R.string.music_options_unmute_all else R.string.music_options_mute_all),
                isActive = !isMuted,
                modifier = Modifier.weight(1f),
                onClick = {
                    val newMuted = !isMuted
                    jinglesManager.setMuted(newMuted)
                    esdeViewModel.musicController.setMuted(newMuted)
                    VideoPresentationManager.setMuted(newMuted)
                    bgMusicManager.setMuted(newMuted)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isPowerButtonVisible) {
                CompactDrawerTile(
                    icon = Icons.Default.PowerSettingsNew,
                    label = stringResource(R.string.header_power_button),
                    modifier = Modifier.weight(1f),
                    onClick = { onPowerClick(); onDismiss() }
                )
            }
            if (isQuickDeleteVisible) {
                CompactDrawerTile(
                    icon = Icons.Default.SdStorage,
                    label = stringResource(R.string.header_folder_options),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickDeleteClick(); onDismiss() }
                )
            }
            CompactDrawerTile(
                icon = Icons.Default.SportsEsports,
                label = stringResource(R.string.rom_search_icon_description),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToRomSearch(); onDismiss() }
            )
            if (showWallpaperToggle) {
                CompactDrawerTile(
                    icon = Icons.Default.SwapHoriz,
                    label = stringResource(R.string.header_wallpaper_toggle),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val currentType = wallpaperManager.getWallpaperType()
                        val target = esdePrefsState.wallpaperToggleTarget
                        if (currentType == WallpaperType.ESDE) {
                            when (target) {
                                WallpaperToggleTarget.SystemWallpaper -> wallpaperManager.setTransparent()
                                WallpaperToggleTarget.SavedImage -> {
                                    val uri = wallpaperManager.savedImageUri
                                    if (uri != null) wallpaperManager.setWallpaper(uri, WallpaperType.IMAGE)
                                    else wallpaperManager.setTransparent()
                                }
                                WallpaperToggleTarget.SavedGif -> {
                                    val uri = wallpaperManager.savedGifUri
                                    if (uri != null) wallpaperManager.setWallpaper(uri, WallpaperType.GIF)
                                    else wallpaperManager.setTransparent()
                                }
                                WallpaperToggleTarget.SavedVideo -> {
                                    val uri = wallpaperManager.savedVideoUri
                                    if (uri != null) wallpaperManager.setWallpaper(uri, WallpaperType.VIDEO)
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onCreateFolderClick != null) {
                DrawerOptionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.dialog_create_folder_title),
                    icon = Icons.Default.FolderOpen,
                    onClick = { onCreateFolderClick(); onDismiss() }
                )
            }
            CompactDrawerTile(
                icon = if (isHeaderVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                label = stringResource(if (isHeaderVisible) R.string.drawer_options_hide_header else R.string.drawer_options_show_header),
                modifier = Modifier.weight(1f),
                onClick = { powerSettingsManager.setHeaderVisibility(!isHeaderVisible); onDismiss() }
            )
        }

        if (wallpaperManager.getWallpaperType() == WallpaperType.ESDE && lastSelectedSystem != null) {
            val packageName = esdePrefsState.systemAppMap[lastSelectedSystem]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (packageName != null) {
                    DrawerOptionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.drawer_options_launch_system_app),
                        onClick = {
                            val displayPref = if (esdePrefsState.systemTopScreenSet.contains(lastSelectedSystem)) {
                                DisplayPreference.PRIMARY_DISPLAY
                            } else {
                                DisplayPreference.CURRENT_DISPLAY
                            }
                            launchApp(context = context, packageName = packageName, displayPreference = displayPref)
                            onDismiss()
                        }
                    )
                }
                DrawerOptionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.drawer_options_configure_system_app),
                    onClick = { onNavigateToSystemApps(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun CompactDrawerTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val iconTint = if (isActive) ThemePrimaryColor else Color.White.copy(alpha = 0.3f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
