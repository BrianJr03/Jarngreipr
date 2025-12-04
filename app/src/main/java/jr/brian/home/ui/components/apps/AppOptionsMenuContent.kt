package jr.brian.home.ui.components.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.extensions.handleDPadNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun AppOptionsMenuContent(
    appLabel: String,
    currentDisplayPreference: DisplayPreference,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    hasExternalDisplay: Boolean,
    focusRequesters: List<FocusRequester>,
    onFocusedIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    app: AppInfo? = null,
    currentIconSize: Float = 64f,
    onIconSizeChange: (Float) -> Unit = {},
    isAppHidden: Boolean = false,
    onToggleVisibility: () -> Unit = {}
) {
    var showResizeMode by remember { mutableStateOf(false) }
    var previewIconSize by remember(currentIconSize) { mutableFloatStateOf(currentIconSize) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (appLabel.isNotEmpty()) {
            Text(
                text = appLabel,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        AnimatedVisibility(
            visible = !showResizeMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuOption(
                    icon = Icons.Default.Info,
                    label = stringResource(id = R.string.app_options_app_info),
                    focusRequester = focusRequesters[0],
                    onClick = {
                        onAppInfoClick()
                        onDismiss()
                    },
                    onNavigateUp = {
                        // Stay on first item
                    },
                    onNavigateDown = {
                        if (focusRequesters.size > 1) {
                            focusRequesters[1].requestFocus()
                            onFocusedIndexChange(1)
                        }
                    },
                    onFocusChanged = { focused ->
                        if (focused)
                            onFocusedIndexChange(0)
                    }
                )

                MenuOption(
                    icon = if (isAppHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    label = stringResource(id = if (isAppHidden) R.string.app_options_show_app else R.string.app_options_hide_app),
                    focusRequester = focusRequesters[1],
                    onClick = {
                        onToggleVisibility()
                        onDismiss()
                    },
                    onNavigateUp = {
                        focusRequesters[0].requestFocus()
                        onFocusedIndexChange(0)
                    },
                    onNavigateDown = {
                        if (app != null && focusRequesters.size > 2) {
                            focusRequesters[2].requestFocus()
                            onFocusedIndexChange(2)
                        } else if (hasExternalDisplay && focusRequesters.size > 2) {
                            focusRequesters[2].requestFocus()
                            onFocusedIndexChange(2)
                        }
                    },
                    onFocusChanged = { focused ->
                        if (focused) onFocusedIndexChange(1)
                    }
                )

                if (app != null) {
                    val resizeIndex = 2
                    val displayStartIndex = 3

                    MenuOption(
                        icon = Icons.Default.Add,
                        label = stringResource(id = R.string.app_options_resize),
                        focusRequester = focusRequesters[resizeIndex],
                        onClick = {
                            showResizeMode = true
                            previewIconSize = currentIconSize
                        },
                        onNavigateUp = {
                            focusRequesters[1].requestFocus()
                            onFocusedIndexChange(1)
                        },
                        onNavigateDown = {
                            if (hasExternalDisplay && focusRequesters.size > displayStartIndex) {
                                focusRequesters[displayStartIndex].requestFocus()
                                onFocusedIndexChange(displayStartIndex)
                            }
                        },
                        onFocusChanged = { focused ->
                            if (focused) onFocusedIndexChange(resizeIndex)
                        }
                    )

                    if (hasExternalDisplay) {
                        MenuOption(
                            icon = Icons.AutoMirrored.Filled.ScreenShare,
                            label = stringResource(id = R.string.app_options_launch_external),
                            isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                            focusRequester = focusRequesters[displayStartIndex],
                            onClick = {
                                onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                                onDismiss()
                            },
                            onNavigateUp = {
                                focusRequesters[resizeIndex].requestFocus()
                                onFocusedIndexChange(resizeIndex)
                            },
                            onNavigateDown = {
                                focusRequesters[displayStartIndex + 1].requestFocus()
                                onFocusedIndexChange(displayStartIndex + 1)
                            },
                            onFocusChanged = { focused ->
                                if (focused) onFocusedIndexChange(displayStartIndex)
                            }
                        )

                        MenuOption(
                            icon = Icons.AutoMirrored.Filled.ScreenShare,
                            label = stringResource(id = R.string.app_options_launch_primary),
                            isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                            focusRequester = focusRequesters[displayStartIndex + 1],
                            onClick = {
                                onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                                onDismiss()
                            },
                            onNavigateUp = {
                                focusRequesters[displayStartIndex].requestFocus()
                                onFocusedIndexChange(displayStartIndex)
                            },
                            onNavigateDown = {
                                // Stay on last item
                            },
                            onFocusChanged = { focused ->
                                if (focused) onFocusedIndexChange(displayStartIndex + 1)
                            }
                        )
                    }
                } else if (hasExternalDisplay) {
                    MenuOption(
                        icon = Icons.AutoMirrored.Filled.ScreenShare,
                        label = stringResource(id = R.string.app_options_launch_external),
                        isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                        focusRequester = focusRequesters[2],
                        onClick = {
                            onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                            onDismiss()
                        },
                        onNavigateUp = {
                            focusRequesters[1].requestFocus()
                            onFocusedIndexChange(1)
                        },
                        onNavigateDown = {
                            focusRequesters[3].requestFocus()
                            onFocusedIndexChange(3)
                        },
                        onFocusChanged = { focused ->
                            if (focused) onFocusedIndexChange(2)
                        }
                    )

                    MenuOption(
                        icon = Icons.AutoMirrored.Filled.ScreenShare,
                        label = stringResource(id = R.string.app_options_launch_primary),
                        isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                        focusRequester = focusRequesters[3],
                        onClick = {
                            onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                            onDismiss()
                        },
                        onNavigateUp = {
                            focusRequesters[2].requestFocus()
                            onFocusedIndexChange(2)
                        },
                        onNavigateDown = {
                            // Stay on last item
                        },
                        onFocusChanged = { focused ->
                            if (focused) onFocusedIndexChange(3)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showResizeMode && app != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AppPreview(
                app = app,
                previewIconSize = previewIconSize,
                onPreviewSizeChange = { newSize ->
                    previewIconSize = newSize
                },
                onCancel = {
                    showResizeMode = false
                    previewIconSize = currentIconSize
                },
                onApply = {
                    onIconSizeChange(previewIconSize)
                    showResizeMode = false
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun AppPreview(
    app: AppInfo?,
    previewIconSize: Float,
    onPreviewSizeChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Icon Preview",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            app?.let {
                Image(
                    painter = rememberAsyncImagePainter(model = it.icon),
                    contentDescription = null,
                    modifier = Modifier.size(previewIconSize.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (previewIconSize > 32f) {
                        onPreviewSizeChange(previewIconSize - 8f)
                    }
                },
                enabled = previewIconSize > 32f
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease size",
                    tint = if (previewIconSize > 32f) ThemePrimaryColor else Color.Gray
                )
            }

            Text(
                text = "${previewIconSize.toInt()} dp",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = {
                    if (previewIconSize < 128f) {
                        onPreviewSizeChange(previewIconSize + 8f)
                    }
                },
                enabled = previewIconSize < 128f
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase size",
                    tint = if (previewIconSize < 128f) ThemePrimaryColor else Color.Gray
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimaryColor
                )
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun MenuOption(
    icon: ImageVector,
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    var isFocused by remember { mutableIntStateOf(0) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = when {
                    isFocused == 1 -> Color.White.copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = if (it.isFocused) 1 else 0
                onFocusChanged(it.isFocused)
            }
            .handleDPadNavigation(
                onNavigateUp = onNavigateUp,
                onNavigateDown = onNavigateDown,
                onEnterPress = onClick
            )
            .focusable(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            val color = if (isSelected) ThemePrimaryColor else Color.White
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                color = color,
                fontSize = 16.sp
            )
        }
    }
}