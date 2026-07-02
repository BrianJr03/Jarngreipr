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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
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
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient

@Composable
fun AppsTabOptionsDialog(
    onDismiss: () -> Unit,
    onShowAppVisibility: () -> Unit,
    isFreeModeEnabled: Boolean = false,
    onToggleFreeMode: (() -> Unit)? = null,
    onResetPositions: () -> Unit = {},
    isDragLocked: Boolean = false,
    onToggleDragLock: (lockOnly: Boolean?) -> Unit = {},
    title: String = stringResource(R.string.app_drawer_options_title),
    isLogoPositionLocked: Boolean = false,
    onToggleMarqueePositionLock: (() -> Unit)? = null,
    onAddRom: (() -> Unit)? = null,
    currentRomDisplayPreference: DisplayPreference? = null,
    onRomDisplayPreferenceChange: ((DisplayPreference) -> Unit)? = null
) {
    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
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
                        text = title,
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
                        title = stringResource(R.string.settings_app_visibility_title),
                        icon = Icons.Default.Visibility,
                        onClick = {
                            onDismiss()
                            onToggleDragLock(true)
                            onShowAppVisibility()
                        }
                    )

                    if (onToggleFreeMode != null) {
                        GridOptionButton(
                            modifier = Modifier.weight(1f),
                            title = if (isFreeModeEnabled) {
                                stringResource(R.string.app_drawer_layout_grid)
                            } else {
                                stringResource(R.string.app_drawer_layout_free)
                            },
                            icon = if (isFreeModeEnabled) Icons.Default.GridOn else Icons.Default.OpenWith,
                            onClick = {
                                onDismiss()
                                onToggleFreeMode()
                            }
                        )
                    }
                }

                if (isFreeModeEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GridOptionButton(
                            modifier = Modifier.weight(1f),
                            title = if (isDragLocked) {
                                stringResource(R.string.app_drawer_unlock_drag_mode)
                            } else {
                                stringResource(R.string.app_drawer_lock_drag_mode)
                            },
                            icon = if (isDragLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            onClick = {
                                onDismiss()
                                onToggleDragLock(null)
                            }
                        )

                        GridOptionButton(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.app_drawer_reset_positions),
                            icon = Icons.Default.RestartAlt,
                            onClick = {
                                onDismiss()
                                onResetPositions()
                            }
                        )
                    }
                }

                if (onAddRom != null || onToggleMarqueePositionLock != null) {
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

                        if (onToggleMarqueePositionLock != null) {
                            GridOptionButton(
                                modifier = Modifier.weight(1f),
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


