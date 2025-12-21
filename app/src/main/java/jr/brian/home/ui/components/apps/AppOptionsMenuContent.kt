package jr.brian.home.ui.components.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.extensions.handleFullNavigation
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
    onToggleVisibility: () -> Unit = {},
    onCustomIconClick: () -> Unit = {}
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
            // Create grid items based on available options
            val gridItems = buildList {
                // Row 1
                add(
                    GridItem.IconItem(
                        icon = Icons.Default.Info,
                        label = "Info",
                        onClick = {
                            onAppInfoClick()
                            onDismiss()
                        },
                        index = 0
                    )
                )
                add(
                    GridItem.IconItem(
                        icon = Icons.Default.VisibilityOff,
                        label = "Hide",
                        onClick = {
                            onToggleVisibility()
                            onDismiss()
                        },
                        index = 1
                    )
                )
                add(
                    GridItem.IconItem(
                        icon = Icons.Default.Image,
                        label = "Icon",
                        onClick = onCustomIconClick,
                        index = 2
                    )
                )

                // Row 2
                if (app != null) {
                    add(
                        GridItem.IconItem(
                            icon = Icons.Default.OpenInFull,
                            label = "Resize",
                            onClick = {
                                showResizeMode = true
                                previewIconSize = currentIconSize
                            },
                            index = 3
                        )
                    )

                    if (hasExternalDisplay) {
                        add(
                            GridItem.TextItem(
                                text = "Top\nDisplay",
                                onClick = {
                                    onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                                    onDismiss()
                                },
                                isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                                index = 4
                            )
                        )
                        add(
                            GridItem.TextItem(
                                text = "Bottom\nDisplay",
                                onClick = {
                                    onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                                    onDismiss()
                                },
                                isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                                index = 5
                            )
                        )
                    }
                } else if (hasExternalDisplay) {
                    add(
                        GridItem.TextItem(
                            text = "Top\nDisplay",
                            onClick = {
                                onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                                onDismiss()
                            },
                            isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                            index = 3
                        )
                    )
                    add(
                        GridItem.TextItem(
                            text = "Bottom\nDisplay",
                            onClick = {
                                onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                                onDismiss()
                            },
                            isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                            index = 4
                        )
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // First row (3 items)
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

                // Second row (up to 3 items)
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

private sealed class GridItem {
    data class IconItem(
        val icon: ImageVector,
        val label: String,
        val onClick: () -> Unit,
        val index: Int
    ) : GridItem()

    data class TextItem(
        val text: String,
        val onClick: () -> Unit,
        val isSelected: Boolean,
        val index: Int
    ) : GridItem()
}

@Composable
private fun IconGridOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    onNavigateLeft: () -> Unit,
    onNavigateRight: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .background(
                color = when {
                    isFocused == 1 -> ThemePrimaryColor.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused == 1) 2.dp else 0.dp,
                color = if (isFocused == 1) ThemePrimaryColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = if (it.isFocused) 1 else 0
                onFocusChanged(it.isFocused)
            }
            .handleFullNavigation(
                onNavigateUp = onNavigateUp,
                onNavigateDown = onNavigateDown,
                onNavigateLeft = onNavigateLeft,
                onNavigateRight = onNavigateRight,
                onEnterPress = onClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TextGridOption(
    text: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onNavigateLeft: () -> Unit,
    onNavigateRight: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .background(
                color = when {
                    isFocused == 1 -> ThemePrimaryColor.copy(alpha = 0.3f)
                    isSelected -> ThemePrimaryColor.copy(alpha = 0.2f)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused == 1) 2.dp else 0.dp,
                color = if (isFocused == 1) ThemePrimaryColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = if (it.isFocused) 1 else 0
                onFocusChanged(it.isFocused)
            }
            .handleFullNavigation(
                onNavigateUp = onNavigateUp,
                onNavigateDown = onNavigateDown,
                onNavigateLeft = onNavigateLeft,
                onNavigateRight = onNavigateRight,
                onEnterPress = onClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) ThemePrimaryColor else Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge
        )
    }
}