package jr.brian.home.esde.ui.sections

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.ui.components.PathSetting
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun CustomPathsSectionContent(
    prefsState: ESDEPrefsState,
    onSelectSystemImagesPath: () -> Unit,
    onClearSystemImagesPath: () -> Unit,
    onSystemBgVideoMutedChange: (Boolean) -> Unit,
    onSystemBgVideoLoopingChange: (Boolean) -> Unit,
    onSelectSystemLogosPath: () -> Unit,
    onClearSystemLogosPath: () -> Unit,
    onSelectSingleSystemImage: () -> Unit,
    onClearSingleSystemImage: () -> Unit,
    onSelectSingleSystemLogo: () -> Unit,
    onClearSingleSystemLogo: () -> Unit,
    onSelectSingleGameImage: () -> Unit,
    onClearSingleGameImage: () -> Unit,
    onSelectSingleGameLogo: () -> Unit,
    onClearSingleGameLogo: () -> Unit,
    onSelectMediaPath: () -> Unit,
    onClearMediaPath: () -> Unit
) {
    SystemBgPathSetting(
        title = stringResource(R.string.esde_settings_custom_system_images_path),
        description = stringResource(R.string.esde_settings_custom_system_images_path_description),
        currentPath = prefsState.customSystemImagesPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        fallbackPath = SetupPreferences.DEFAULT_MEDIA_PATH + "/system_images",
        onSelectPath = onSelectSystemImagesPath,
        onClearPath = onClearSystemImagesPath,
        videoMuted = prefsState.systemBgVideoMuted,
        videoLooping = prefsState.systemBgVideoLooping,
        onVideoMutedChange = onSystemBgVideoMutedChange,
        onVideoLoopingChange = onSystemBgVideoLoopingChange
    )

    SystemBgPathSetting(
        title = stringResource(R.string.esde_settings_single_system_image),
        description = stringResource(R.string.esde_settings_single_system_image_description),
        currentPath = prefsState.singleSystemImagePath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        fallbackPath = null,
        onSelectPath = onSelectSingleSystemImage,
        onClearPath = onClearSingleSystemImage,
        videoMuted = prefsState.systemBgVideoMuted,
        videoLooping = prefsState.systemBgVideoLooping,
        onVideoMutedChange = onSystemBgVideoMutedChange,
        onVideoLoopingChange = onSystemBgVideoLoopingChange
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_custom_system_logos_path),
        description = stringResource(R.string.esde_settings_custom_system_logos_path_description),
        currentPath = prefsState.customSystemLogosPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSystemLogosPath,
        onClearPath = onClearSystemLogosPath,
        fallbackPath = SetupPreferences.DEFAULT_MEDIA_PATH + "/system_logos"
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_single_system_logo),
        description = stringResource(R.string.esde_settings_single_system_logo_description),
        currentPath = prefsState.singleSystemLogoPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSingleSystemLogo,
        onClearPath = onClearSingleSystemLogo
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_single_game_image),
        description = stringResource(R.string.esde_settings_single_game_image_description),
        currentPath = prefsState.singleGameImagePath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSingleGameImage,
        onClearPath = onClearSingleGameImage
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_single_game_logo),
        description = stringResource(R.string.esde_settings_single_game_logo_description),
        currentPath = prefsState.singleGameLogoPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSingleGameLogo,
        onClearPath = onClearSingleGameLogo
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_custom_media_path),
        description = stringResource(R.string.esde_settings_custom_media_path_description),
        currentPath = prefsState.customMediaPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectMediaPath,
        onClearPath = onClearMediaPath,
        fallbackPath = SetupPreferences.DEFAULT_MEDIA_PATH
    )
}

@Composable
private fun SystemBgPathSetting(
    title: String,
    description: String,
    currentPath: String?,
    defaultText: String,
    fallbackPath: String?,
    onSelectPath: () -> Unit,
    onClearPath: () -> Unit,
    videoMuted: Boolean,
    videoLooping: Boolean,
    onVideoMutedChange: (Boolean) -> Unit,
    onVideoLoopingChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .background(
                brush = subtleCardGradient(isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { onSelectPath() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            IconButton(onClick = onSelectPath) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.esde_settings_select_folder),
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (currentPath != null) {
                IconButton(onClick = onClearPath) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.esde_settings_clear_path),
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val displayPath = currentPath ?: fallbackPath ?: defaultText
        val pathColor = when {
            currentPath != null -> ThemePrimaryColor
            fallbackPath != null -> Color.Gray.copy(alpha = 0.8f)
            else -> Color.Gray
        }
        Text(
            text = displayPath,
            color = pathColor,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = if (expanded) stringResource(R.string.esde_settings_video_options_hide)
                       else stringResource(R.string.esde_settings_video_options_more),
                color = ThemePrimaryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        }
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        expanded = !expanded
                    }
                    .padding(top = 8.dp, bottom = 2.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )
                VideoOptionRow(
                    label = stringResource(R.string.esde_settings_system_bg_video_muted),
                    description = stringResource(R.string.esde_settings_system_bg_video_muted_description),
                    checked = videoMuted,
                    onCheckedChange = onVideoMutedChange
                )
                Spacer(modifier = Modifier.height(12.dp))
                VideoOptionRow(
                    label = stringResource(R.string.esde_settings_system_bg_video_looping),
                    description = stringResource(R.string.esde_settings_system_bg_video_looping_description),
                    checked = videoLooping,
                    onCheckedChange = onVideoLoopingChange
                )
            }
        }
    }
}

@Composable
private fun VideoOptionRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThemePrimaryColor,
                checkedTrackColor = ThemeSecondaryColor.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
