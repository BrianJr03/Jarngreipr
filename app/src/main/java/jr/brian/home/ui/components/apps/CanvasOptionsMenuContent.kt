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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Tune
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
import jr.brian.home.ui.util.rememberAutoFocus

@Composable
fun CanvasOptionsMenuContent(
    appLabel: String,
    currentDisplayPreference: DisplayPreference,
    hasExternalDisplay: Boolean,
    app: AppInfo? = null,
    currentIconSize: Float = 64f,
    isInDock: Boolean = false,
    onDismiss: () -> Unit,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    onIconSizeChange: (Float) -> Unit = {},
    onToggleVisibility: (() -> Unit)? = null,
    onCustomIconClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onRemoveFromDock: () -> Unit = {},
    onEditCanvas: (() -> Unit)? = null,
    promptForDisplayOnLaunch: Boolean = false,
    onPromptForDisplayOnLaunchChange: ((Boolean) -> Unit)? = null
) {
    var showResizeMode by remember { mutableStateOf(false) }
    var previewIconSize by remember(currentIconSize) { mutableFloatStateOf(currentIconSize) }

    val mainItems: List<GridItem> = buildList {
        if (onEditCanvas != null) {
            add(
                GridItem.IconItem(
                    icon = Icons.Default.Tune,
                    label = stringResource(R.string.canvas_edit_canvas_option),
                    onClick = {
                        onDismiss()
                        onEditCanvas()
                    }
                )
            )
        }
        add(
            GridItem.IconItem(
            icon = Icons.Default.Info,
            label = stringResource(R.string.app_options_info),
            onClick = { onAppInfoClick(); onDismiss() }
        ))
        if (!isInDock && onToggleVisibility != null) {
            add(
                GridItem.IconItem(
                icon = Icons.Default.VisibilityOff,
                label = stringResource(R.string.app_options_hide),
                onClick = {
                    onToggleVisibility.invoke()
                    onDismiss()
                }
            ))
        }
        add(
            GridItem.IconItem(
                icon = Icons.Default.Image,
                label = stringResource(R.string.app_options_icon),
                onClick = onCustomIconClick
            )
        )
        add(
            GridItem.IconItem(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.app_options_rename),
                onClick = onRenameClick
            )
        )
        if (isInDock) {
            add(
                GridItem.IconItem(
                icon = Icons.Default.RemoveCircleOutline,
                label = stringResource(R.string.dock_remove_app),
                onClick = { onRemoveFromDock(); onDismiss() }
            ))
        }
        if (app != null && !isInDock) {
            add(
                GridItem.IconItem(
                icon = Icons.Default.OpenInFull,
                label = stringResource(R.string.app_options_resize),
                onClick = {
                    showResizeMode = true
                    previewIconSize = currentIconSize
                }
            ))
        }
    }

    val launchItems: List<GridItem> = buildList {
        if (!hasExternalDisplay) return@buildList
        add(
            GridItem.IconItem(
                icon = Icons.AutoMirrored.Filled.Launch,
                label = stringResource(R.string.app_options_launch_primary_short),
                isSelected = !promptForDisplayOnLaunch &&
                    currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                onClick = {
                    onPromptForDisplayOnLaunchChange?.invoke(false)
                    onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                    onDismiss()
                }
            )
        )
        add(
            GridItem.IconItem(
                icon = Icons.AutoMirrored.Filled.Launch,
                label = stringResource(R.string.app_options_launch_external_short),
                isSelected = !promptForDisplayOnLaunch &&
                    currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                onClick = {
                    onPromptForDisplayOnLaunchChange?.invoke(false)
                    onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                    onDismiss()
                }
            )
        )
        if (onPromptForDisplayOnLaunchChange != null) {
            add(
                GridItem.IconItem(
                    icon = Icons.AutoMirrored.Filled.Launch,
                    label = stringResource(R.string.app_options_launch_ask_short),
                    isSelected = promptForDisplayOnLaunch,
                    onClick = {
                        onPromptForDisplayOnLaunchChange(true)
                        onDismiss()
                    }
                )
            )
        }
    }

    val rows: List<List<GridItem>> = buildList {
        addAll(mainItems.chunked(3))
        if (launchItems.isNotEmpty()) add(launchItems)
    }
    val flatItems: List<GridItem> = rows.flatten()
    val rowStartOffsets: List<Int> = rows.runningFold(0) { acc, row -> acc + row.size }

    val firstFocusRequester = rememberAutoFocus()
    val focusRequesters = remember(flatItems.size, firstFocusRequester) {
        List(flatItems.size) { i -> if (i == 0) firstFocusRequester else FocusRequester() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEachIndexed { rowIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { colIdx, item ->
                            val listIdx = rowStartOffsets[rowIdx] + colIdx
                            val onNavigateLeft = {
                                if (colIdx > 0) focusRequesters[listIdx - 1].requestFocus()
                            }
                            val onNavigateRight = {
                                if (colIdx < rowItems.size - 1) focusRequesters[listIdx + 1].requestFocus()
                            }
                            val onNavigateUp = {
                                if (rowIdx > 0) {
                                    val prev = rows[rowIdx - 1]
                                    val safeCol = colIdx.coerceAtMost(prev.size - 1)
                                    focusRequesters[rowStartOffsets[rowIdx - 1] + safeCol].requestFocus()
                                }
                            }
                            val onNavigateDown = {
                                if (rowIdx < rows.size - 1) {
                                    val next = rows[rowIdx + 1]
                                    val safeCol = colIdx.coerceAtMost(next.size - 1)
                                    focusRequesters[rowStartOffsets[rowIdx + 1] + safeCol].requestFocus()
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                when (item) {
                                    is GridItem.IconItem -> IconGridOption(
                                        icon = item.icon,
                                        label = item.label,
                                        onClick = item.onClick,
                                        focusRequester = focusRequesters[listIdx],
                                        onNavigateLeft = onNavigateLeft,
                                        onNavigateRight = onNavigateRight,
                                        onNavigateUp = onNavigateUp,
                                        onNavigateDown = onNavigateDown,
                                        onFocusChanged = {},
                                        isSelected = item.isSelected
                                    )

                                    is GridItem.TextItem -> TextGridOption(
                                        text = item.text,
                                        onClick = item.onClick,
                                        isSelected = item.isSelected,
                                        focusRequester = focusRequesters[listIdx],
                                        onNavigateLeft = onNavigateLeft,
                                        onNavigateRight = onNavigateRight,
                                        onNavigateUp = onNavigateUp,
                                        onNavigateDown = onNavigateDown,
                                        onFocusChanged = {}
                                    )
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
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