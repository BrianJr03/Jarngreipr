package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.util.MediaPickerLauncher

@Composable
fun DrawerOptionsDialog(
    onDismiss: () -> Unit,
) {
    val wallpaperManager = LocalWallpaperManager.current
    val mediaPickerLauncher = MediaPickerLauncher()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            color = OledBackgroundColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.drawer_options_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = stringResource(R.string.drawer_options_wallpaper_section),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                WallpaperGridButton(
                    text = stringResource(id = R.string.settings_wallpaper_default),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.NONE,
                    onClick = {
                        wallpaperManager.clearWallpaper()
                        onDismiss()
                    }
                )

                WallpaperGridButton(
                    text = stringResource(id = R.string.settings_wallpaper_transparent),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.TRANSPARENT,
                    onClick = {
                        wallpaperManager.setTransparent()
                        onDismiss()
                    }
                )

                WallpaperGridButton(
                    text = stringResource(id = R.string.settings_wallpaper_image_picker),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.IMAGE,
                    onClick = {
                        mediaPickerLauncher.launch(arrayOf("image/*"))
                    }
                )

                WallpaperGridButton(
                    text = stringResource(id = R.string.settings_wallpaper_gif_picker),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.GIF,
                    onClick = {
                        mediaPickerLauncher.launch(arrayOf("image/gif"))
                    }
                )

                WallpaperGridButton(
                    text = stringResource(id = R.string.settings_wallpaper_video_picker),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.VIDEO,
                    onClick = {
                        mediaPickerLauncher.launch(arrayOf("video/*"))
                    }
                )
            }
        }
    }
}

@Composable
private fun WallpaperGridButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isFocused -> Color.White.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val textColor = if (isSelected) ThemePrimaryColor else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp
        )
    }
}
