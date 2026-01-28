package jr.brian.home.ui.components.dialog

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.OledCardColor

import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun AppsTabOptionsDialog(
    onDismiss: () -> Unit,
    onShowAppVisibility: () -> Unit,
    isFreeModeEnabled: Boolean = false,
    onToggleFreeMode: (() -> Unit)? = null,
    onResetPositions: () -> Unit = {},
    isDragLocked: Boolean = false,
    onToggleDragLock: (lockOnly: Boolean?) -> Unit = {},
    title: String = stringResource(R.string.app_drawer_options_title)
) {
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
            }
        }
    }
}

@Composable
private fun GridOptionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
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
            .clickable { onClick() }
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


