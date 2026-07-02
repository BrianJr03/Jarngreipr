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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType

/**
 * Wallpaper picker grid shared by the drawer options dialog and the canvas
 * edit dialog. Toggles visible state from a parent flag; on selection it
 * mutates [wallpaperManager] (or launches [mediaPickerLauncher] for an OS
 * media pick) and then fires [onBack] + [onDismiss] so the host can collapse
 * back to its main view and close the surrounding dialog.
 *
 * Extracted verbatim from `DrawerOptionsDialog` so both surfaces stay in
 * lockstep — no behavior change.
 */
@Composable
fun WallpaperOptionsSection(
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
                brush = borderBrush(isFocused = isFocused),
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
