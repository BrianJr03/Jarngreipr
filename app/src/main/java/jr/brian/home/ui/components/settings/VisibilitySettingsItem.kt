package jr.brian.home.ui.components.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager

@Composable
fun VisibilitySettingsItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {}
) {
    val appVisibilityManager = LocalAppVisibilityManager.current
    val powerSettingsManager = LocalPowerSettingsManager.current
    
    val showAppNames = appVisibilityManager.showAppNames
    val showFolderNames = appVisibilityManager.showFolderNames
    val showSettingsBackButton = appVisibilityManager.showSettingsBackButton
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }
    val firstOptionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            firstOptionFocusRequester.requestFocus()
        } else {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                    )
                } else {
                    listOf(
                        OledCardLightColor,
                        OledCardColor,
                    )
                },
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
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
                        brush = cardGradient,
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
                        onExpandChanged(!isExpanded)
                    }
                    .focusable()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.settings_visibility_options_title),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column {
                        Text(
                            text = stringResource(id = R.string.settings_visibility_options_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_visibility_options_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VisibilityToggleOption(
                    focusRequester = firstOptionFocusRequester,
                    icon = Icons.Default.Api,
                    title = stringResource(id = R.string.settings_show_app_names_title),
                    description = stringResource(id = R.string.settings_show_app_names_description),
                    isEnabled = showAppNames,
                    onClick = { appVisibilityManager.toggleShowAppNames() }
                )

                VisibilityToggleOption(
                    icon = Icons.Default.Folder,
                    title = stringResource(id = R.string.settings_show_folder_names_title),
                    description = stringResource(id = R.string.settings_show_folder_names_description),
                    isEnabled = showFolderNames,
                    onClick = { appVisibilityManager.toggleShowFolderNames() }
                )

                VisibilityToggleOption(
                    icon = Icons.Default.Visibility,
                    title = stringResource(id = R.string.settings_header_visibility_title),
                    description = stringResource(id = R.string.settings_header_visibility_description),
                    isEnabled = isHeaderVisible,
                    onClick = { powerSettingsManager.setHeaderVisibility(!isHeaderVisible) }
                )

                VisibilityToggleOption(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    title = stringResource(id = R.string.settings_show_back_button_title),
                    description = stringResource(id = R.string.settings_show_back_button_description),
                    isEnabled = showSettingsBackButton,
                    onClick = { appVisibilityManager.toggleShowSettingsBackButton() }
                )
            }
        }
    }
}

@Composable
private fun VisibilityToggleOption(
    focusRequester: FocusRequester? = null,
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val localFocusRequester = remember { FocusRequester() }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.6f),
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                } else {
                    listOf(
                        OledCardLightColor.copy(alpha = 0.7f),
                        OledCardColor.copy(alpha = 0.7f),
                    )
                },
        )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester ?: localFocusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused
                }
                .background(
                    brush = cardGradient,
                    shape = RoundedCornerShape(12.dp),
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
                    shape = RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .focusable()
                .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White.copy(alpha = 0.9f),
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (isFocused) 16.sp else 14.sp,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.Gray,
                        fontSize = 12.sp,
                    )
                }
            }

            Text(
                text = if (isEnabled) stringResource(R.string.settings_toggle_on)
                else stringResource(R.string.settings_toggle_off),
                color = if (isEnabled) Color.Green else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
