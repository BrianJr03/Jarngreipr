package jr.brian.home.ui.components.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.ui.components.PathSetting
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.components.wallpaper.WallpaperOptionButton
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.util.WallpaperUtils

@Composable
fun WallpaperSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {},
    onESDESetupClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    val setupPreferences = remember { SetupPreferences(context) }
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            val copiedUri = WallpaperUtils.copyWallpaperToInternalStorage(
                context, uri, WallpaperType.IMAGE
            )
            if (copiedUri != null) {
                wallpaperManager.updateSavedImageUri(copiedUri)
                wallpaperManager.setWallpaper(copiedUri, WallpaperType.IMAGE)
            }
        }
    }

    val gifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            val copiedUri = WallpaperUtils.copyWallpaperToInternalStorage(
                context, uri, WallpaperType.GIF
            )
            if (copiedUri != null) {
                wallpaperManager.updateSavedGifUri(copiedUri)
                wallpaperManager.setWallpaper(copiedUri, WallpaperType.GIF)
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            val copiedUri = WallpaperUtils.copyWallpaperToInternalStorage(
                context, uri, WallpaperType.VIDEO
            )
            if (copiedUri != null) {
                wallpaperManager.updateSavedVideoUri(copiedUri)
                wallpaperManager.setWallpaper(copiedUri, WallpaperType.VIDEO)
            }
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            mainCardFocusRequester.requestFocus()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester ?: mainCardFocusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .background(
                            brush = subtleCardGradient(isFocused),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            brush =
                                borderBrush(
                                    isFocused = isFocused,
                                    colors =
                                        listOf(
                                            ThemePrimaryColor.copy(alpha = 0.8f),
                                            ThemeSecondaryColor.copy(alpha = 0.6f),
                                        ),
                                ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onExpandChanged(true)
                        }
                        .focusable()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = stringResource(R.string.settings_wallpaper_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_wallpaper_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_wallpaper_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PathSetting(
                    title = stringResource(R.string.wallpaper_saved_image),
                    description = stringResource(R.string.wallpaper_saved_image_description),
                    currentPath = wallpaperManager.savedImageUri,
                    defaultText = stringResource(R.string.wallpaper_not_set),
                    onSelectPath = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                    },
                    onClearPath = {
                        wallpaperManager.updateSavedImageUri(null)
                    }
                )

                PathSetting(
                    title = stringResource(R.string.wallpaper_saved_gif),
                    description = stringResource(R.string.wallpaper_saved_gif_description),
                    currentPath = wallpaperManager.savedGifUri,
                    defaultText = stringResource(R.string.wallpaper_not_set),
                    onSelectPath = {
                        gifPickerLauncher.launch(arrayOf("image/gif"))
                    },
                    onClearPath = {
                        wallpaperManager.updateSavedGifUri(null)
                    }
                )

                PathSetting(
                    title = stringResource(R.string.wallpaper_saved_video),
                    description = stringResource(R.string.wallpaper_saved_video_description),
                    currentPath = wallpaperManager.savedVideoUri,
                    defaultText = stringResource(R.string.wallpaper_not_set),
                    onSelectPath = {
                        videoPickerLauncher.launch(arrayOf("video/*"))
                    },
                    onClearPath = {
                        wallpaperManager.updateSavedVideoUri(null)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_default),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.NONE,
                    onClick = {
                        wallpaperManager.clearWallpaper()
                        onExpandChanged(false)
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_transparent),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.TRANSPARENT,
                    onClick = {
                        wallpaperManager.setTransparent()
                        onExpandChanged(false)
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_esde),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.ESDE,
                    onClick = {
                        if (setupPreferences.setupCompleted) {
                            wallpaperManager.setESDE()
                            onExpandChanged(false)
                        } else {
                            onExpandChanged(false)
                            onESDESetupClick()
                        }
                    }
                )
            }
        }
    }
}
