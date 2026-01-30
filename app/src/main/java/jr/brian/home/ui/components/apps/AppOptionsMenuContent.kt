package jr.brian.home.ui.components.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.GridItem
import jr.brian.home.model.app.AppInfo

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
    onToggleVisibility: () -> Unit = {},
    onCustomIconClick: () -> Unit = {},
    isInDock: Boolean = false,
    onRemoveFromDock: () -> Unit = {}
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
            val gridItems = buildList {
                add(
                    GridItem.IconItem(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.app_options_info),
                        onClick = {
                            onAppInfoClick()
                            onDismiss()
                        },
                        index = 0
                    )
                )
                if (!isInDock){
                    add(
                        GridItem.IconItem(
                            icon = Icons.Default.VisibilityOff,
                            label = stringResource(R.string.app_options_hide),
                            onClick = {
                                onToggleVisibility()
                                onDismiss()
                            },
                            index = 1
                        )
                    )
                }
                add(
                    GridItem.IconItem(
                        icon = Icons.Default.Image,
                        label = stringResource(R.string.app_options_icon),
                        onClick = onCustomIconClick,
                        index = 2
                    )
                )
                
                if (isInDock) {
                    add(
                        GridItem.IconItem(
                            icon = Icons.Default.RemoveCircleOutline,
                            label = stringResource(R.string.dock_remove_app),
                            onClick = {
                                onRemoveFromDock()
                                onDismiss()
                            },
                            index = 3
                        )
                    )
                }

                if (app != null) {
                    val resizeIndex = if (isInDock) 4 else 3
                    if (!isInDock) {
                        add(
                            GridItem.IconItem(
                                icon = Icons.Default.OpenInFull,
                                label = stringResource(R.string.app_options_resize),
                                onClick = {
                                    showResizeMode = true
                                    previewIconSize = currentIconSize
                                },
                                index = resizeIndex
                            )
                        )
                    }

                    if (hasExternalDisplay) {
                        val displayIndexOffset = if (isInDock) 1 else 0
                        add(
                            GridItem.TextItem(
                                text = stringResource(R.string.app_options_launch_primary_descr),
                                onClick = {
                                    onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                                    onDismiss()
                                },
                                isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                                index = 4 + displayIndexOffset
                            )
                        )
                        add(
                            GridItem.TextItem(
                                text = stringResource(R.string.app_options_launch_external_descr),
                                onClick = {
                                    onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                                    onDismiss()
                                },
                                isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                                index = 5 + displayIndexOffset
                            )
                        )
                    }
                } else if (hasExternalDisplay) {
                    val displayIndexOffset = if (isInDock) 1 else 0
                    add(
                        GridItem.TextItem(
                            text = stringResource(R.string.app_options_launch_primary_descr),
                            onClick = {
                                onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                                onDismiss()
                            },
                            isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                            index = 3 + displayIndexOffset
                        )
                    )
                    add(
                        GridItem.TextItem(
                            text = stringResource(R.string.app_options_launch_external_descr),
                            onClick = {
                                onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                                onDismiss()
                            },
                            isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                            index = 4 + displayIndexOffset
                        )
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems.take(3).forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            when (item) {
                                is GridItem.IconItem -> IconGridOption(
                                    icon = item.icon,
                                    label = item.label,
                                    onClick = item.onClick,
                                    focusRequester = focusRequesters[item.index],
                                    onNavigateLeft = {
                                        if (item.index > 0) {
                                            focusRequesters[item.index - 1].requestFocus()
                                            onFocusedIndexChange(item.index - 1)
                                        }
                                    },
                                    onNavigateRight = {
                                        if (item.index < 2 && gridItems.size > item.index + 1) {
                                            focusRequesters[item.index + 1].requestFocus()
                                            onFocusedIndexChange(item.index + 1)
                                        }
                                    },
                                    onNavigateUp = {
                                        // Stay on top row
                                    },
                                    onNavigateDown = {
                                        val bottomRowIndex = item.index + 3
                                        if (gridItems.size > bottomRowIndex) {
                                            focusRequesters[bottomRowIndex].requestFocus()
                                            onFocusedIndexChange(bottomRowIndex)
                                        }
                                    },
                                    onFocusChanged = { focused ->
                                        if (focused) onFocusedIndexChange(item.index)
                                    }
                                )

                                is GridItem.TextItem -> {}
                            }
                        }
                    }
                }

                if (gridItems.size > 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        gridItems.drop(3).take(3).forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                when (item) {
                                    is GridItem.IconItem -> IconGridOption(
                                        icon = item.icon,
                                        label = item.label,
                                        onClick = item.onClick,
                                        focusRequester = focusRequesters[item.index],
                                        onNavigateLeft = {
                                            if (item.index > 3) {
                                                focusRequesters[item.index - 1].requestFocus()
                                                onFocusedIndexChange(item.index - 1)
                                            }
                                        },
                                        onNavigateRight = {
                                            if (item.index < gridItems.size - 1) {
                                                focusRequesters[item.index + 1].requestFocus()
                                                onFocusedIndexChange(item.index + 1)
                                            }
                                        },
                                        onNavigateUp = {
                                            val topRowIndex = item.index - 3
                                            if (topRowIndex >= 0) {
                                                focusRequesters[topRowIndex].requestFocus()
                                                onFocusedIndexChange(topRowIndex)
                                            }
                                        },
                                        onNavigateDown = {
                                            // Stay on bottom row
                                        },
                                        onFocusChanged = { focused ->
                                            if (focused) onFocusedIndexChange(item.index)
                                        }
                                    )

                                    is GridItem.TextItem -> TextGridOption(
                                        text = item.text,
                                        onClick = item.onClick,
                                        isSelected = item.isSelected,
                                        focusRequester = focusRequesters[item.index],
                                        onNavigateLeft = {
                                            if (item.index > 3) {
                                                focusRequesters[item.index - 1].requestFocus()
                                                onFocusedIndexChange(item.index - 1)
                                            }
                                        },
                                        onNavigateRight = {
                                            if (item.index < gridItems.size - 1) {
                                                focusRequesters[item.index + 1].requestFocus()
                                                onFocusedIndexChange(item.index + 1)
                                            }
                                        },
                                        onNavigateUp = {
                                            val topRowIndex = item.index - 3
                                            if (topRowIndex >= 0) {
                                                focusRequesters[topRowIndex].requestFocus()
                                                onFocusedIndexChange(topRowIndex)
                                            }
                                        },
                                        onNavigateDown = {
                                            // Stay on bottom row
                                        },
                                        onFocusChanged = { focused ->
                                            if (focused) onFocusedIndexChange(item.index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showResizeMode && app != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AppResizePreview(
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