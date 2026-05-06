package jr.brian.home.ui.components.dialog

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun AppsAndWidgetsOptionsDialog(
    onDismiss: () -> Unit,
    onAddWidget: () -> Unit,
    onAddApp: () -> Unit,
    onSwapSections: () -> Unit,
    onToggleEditMode: () -> Unit,
    isEditModeActive: Boolean = false,
    isEmpty: Boolean = false,
    isLogoPositionLocked: Boolean = false,
    onToggleMarqueePositionLock: (() -> Unit)? = null,
    onAddRom: (() -> Unit)? = null,
    onCustomIcon: (() -> Unit)? = null,
    currentRomDisplayPreference: DisplayPreference? = null,
    onRomDisplayPreferenceChange: ((DisplayPreference) -> Unit)? = null
) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_widget_options_title),
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
                            contentDescription = stringResource(R.string.dialog_cancel),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GridOptionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.widget_page_add_widget),
                        icon = Icons.Default.Add,
                        onClick = {
                            onDismiss()
                            onAddWidget()
                        }
                    )

                    GridOptionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.settings_app_visibility_title),
                        icon = Icons.Default.Visibility,
                        onClick = {
                            onDismiss()
                            onAddApp()
                        }
                    )
                }

                if (!isEmpty) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GridOptionButton(
                            modifier = Modifier.weight(1f),
                            title = stringResource(
                                if (isEditModeActive) R.string.widget_page_edit_mode_exit
                                else R.string.widget_page_edit_mode
                            ),
                            icon = Icons.Default.Edit,
                            onClick = {
                                onDismiss()
                                onToggleEditMode()
                            }
                        )

                        GridOptionButton(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.widget_page_swap_sections),
                            icon = Icons.Default.SwapVert,
                            onClick = {
                                onDismiss()
                                onSwapSections()
                            }
                        )
                    }
                }

                if (onAddRom != null || onCustomIcon != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onAddRom != null) {
                            GridOptionButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.add_rom),
                                icon = Icons.Default.VideogameAsset,
                                onClick = {
                                    onDismiss()
                                    onAddRom()
                                }
                            )
                        }

                        if (onCustomIcon != null) {
                            GridOptionButton(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.app_options_custom_icon),
                                icon = Icons.Default.Palette,
                                onClick = {
                                    onDismiss()
                                    onCustomIcon()
                                }
                            )
                        }
                    }
                }

                if (onRomDisplayPreferenceChange != null && currentRomDisplayPreference != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GridOptionButton(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.app_options_launch_primary_descr),
                            icon = Icons.Default.Tv,
                            isSelected = currentRomDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                            onClick = { onRomDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY) }
                        )
                        GridOptionButton(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.app_options_launch_external_descr),
                            icon = Icons.Default.PhoneAndroid,
                            isSelected = currentRomDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                            onClick = { onRomDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY) }
                        )
                    }
                }

                if (onToggleMarqueePositionLock != null) {
                    GridOptionButton(
                        modifier = Modifier.fillMaxWidth(),
                        title = if (isLogoPositionLocked) {
                            stringResource(R.string.marquee_position_unlock)
                        } else {
                            stringResource(R.string.marquee_position_lock)
                        },
                        icon = if (isLogoPositionLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        onClick = {
                            onDismiss()
                            onToggleMarqueePositionLock()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridOptionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val highlighted = isFocused || isSelected

    Box(
        modifier = modifier
            .scale(animatedFocusedScale(highlighted))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = highlighted),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (highlighted) 3.dp else 2.dp,
                brush = borderBrush(isFocused = highlighted),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
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


