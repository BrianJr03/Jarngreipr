package jr.brian.home.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.wallpaper.WallpaperOptionButton
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.util.MediaPickerLauncher

@Composable
fun DrawerOptionsDialog(
    onDismiss: () -> Unit,
    onPowerClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onQuickDeleteClick: () -> Unit
) {
    val wallpaperManager = LocalWallpaperManager.current
    val powerSettingsManager = LocalPowerSettingsManager.current
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val isQuickDeleteVisible by powerSettingsManager.quickDeleteVisible.collectAsStateWithLifecycle()
    var isWallpaperExpanded by remember { mutableStateOf(false) }
    val mediaPickerLauncher = MediaPickerLauncher(
        onResult = {
            isWallpaperExpanded = false
            onDismiss()
        }
    )

    Dialog(
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

                if (!isHeaderVisible) {
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
                                icon = Icons.Default.FolderOpen,
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

                DrawerOptionCard(
                    title = if (isHeaderVisible) {
                        stringResource(R.string.drawer_options_hide_header)
                    } else {
                        stringResource(R.string.drawer_options_show_header)
                    },
                    description = stringResource(R.string.drawer_options_header_description),
                    icon = if (isHeaderVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    onClick = {
                        powerSettingsManager.setHeaderVisibility(!isHeaderVisible)
                        onDismiss()
                    }
                )

                AnimatedVisibility(
                    visible = !isWallpaperExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DrawerOptionCard(
                        title = stringResource(R.string.settings_wallpaper_title),
                        description = stringResource(R.string.settings_wallpaper_description),
                        icon = Icons.Default.Wallpaper,
                        onClick = {
                            isWallpaperExpanded = true
                        }
                    )
                }

                AnimatedVisibility(
                    visible = isWallpaperExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WallpaperOptionButton(
                            text = stringResource(R.string.settings_wallpaper_default),
                            isSelected = wallpaperManager.getWallpaperType() == WallpaperType.NONE,
                            onClick = {
                                wallpaperManager.clearWallpaper()
                                isWallpaperExpanded = false
                                onDismiss()
                            }
                        )

                        WallpaperOptionButton(
                            text = stringResource(R.string.settings_wallpaper_transparent),
                            isSelected = wallpaperManager.getWallpaperType() == WallpaperType.TRANSPARENT,
                            onClick = {
                                wallpaperManager.setTransparent()
                                isWallpaperExpanded = false
                                onDismiss()
                            }
                        )

                        WallpaperOptionButton(
                            text = stringResource(R.string.settings_wallpaper_image_picker),
                            isSelected = wallpaperManager.getWallpaperType() == WallpaperType.IMAGE,
                            onClick = {
                                mediaPickerLauncher.launch(arrayOf("image/*"))
                            }
                        )

                        WallpaperOptionButton(
                            text = stringResource(R.string.settings_wallpaper_gif_picker),
                            isSelected = wallpaperManager.getWallpaperType() == WallpaperType.GIF,
                            onClick = {
                                mediaPickerLauncher.launch(arrayOf("image/gif"))
                            }
                        )

                        WallpaperOptionButton(
                            text = stringResource(R.string.settings_wallpaper_video_picker),
                            isSelected = wallpaperManager.getWallpaperType() == WallpaperType.VIDEO,
                            onClick = {
                                mediaPickerLauncher.launch(arrayOf("video/*"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.9f)
            )
        } else if (isSelected) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.5f)
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.4f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 3.dp else if (isSelected) 2.dp else 2.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.5f)
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
            .clickable { onClick() }
            .focusable()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
            .clickable { onClick() }
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
