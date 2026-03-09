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
import jr.brian.home.ui.util.rememberAutoFocus

@Composable
fun AppOptionsMenuContent(
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
    onRemoveFromDock: () -> Unit = {}
) {
    var showResizeMode by remember { mutableStateOf(false) }
    var previewIconSize by remember(currentIconSize) { mutableFloatStateOf(currentIconSize) }

    val items: List<GridItem> = buildList {
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
        if (hasExternalDisplay) {
            add(
                GridItem.TextItem(
                text = stringResource(R.string.app_options_launch_primary_descr),
                isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                onClick = {
                    onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                    onDismiss()
                }
            ))
            add(
                GridItem.TextItem(
                text = stringResource(R.string.app_options_launch_external_descr),
                isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
                onClick = {
                    onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                    onDismiss()
                }
            ))
        }
    }

    val firstFocusRequester = rememberAutoFocus()
    val focusRequesters = remember(items.size, firstFocusRequester) {
        List(items.size) { i -> if (i == 0) firstFocusRequester else FocusRequester() }
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
                items.chunked(3).forEachIndexed { rowIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { colIdx, item ->
                            val listIdx = rowIdx * 3 + colIdx
                            Box(modifier = Modifier.weight(1f)) {
                                when (item) {
                                    is GridItem.IconItem -> IconGridOption(
                                        icon = item.icon,
                                        label = item.label,
                                        onClick = item.onClick,
                                        focusRequester = focusRequesters[listIdx],
                                        onNavigateLeft = {
                                            if (colIdx > 0) focusRequesters[listIdx - 1].requestFocus()
                                        },
                                        onNavigateRight = {
                                            if (colIdx < rowItems.size - 1) focusRequesters[listIdx + 1].requestFocus()
                                        },
                                        onNavigateUp = {
                                            val upIdx = listIdx - 3
                                            if (upIdx >= 0) focusRequesters[upIdx].requestFocus()
                                        },
                                        onNavigateDown = {
                                            val downIdx = listIdx + 3
                                            if (downIdx < items.size) focusRequesters[downIdx].requestFocus()
                                        },
                                        onFocusChanged = {}
                                    )

                                    is GridItem.TextItem -> TextGridOption(
                                        text = item.text,
                                        onClick = item.onClick,
                                        isSelected = item.isSelected,
                                        focusRequester = focusRequesters[listIdx],
                                        onNavigateLeft = {
                                            if (colIdx > 0) focusRequesters[listIdx - 1].requestFocus()
                                        },
                                        onNavigateRight = {
                                            if (colIdx < rowItems.size - 1) focusRequesters[listIdx + 1].requestFocus()
                                        },
                                        onNavigateUp = {
                                            val upIdx = listIdx - 3
                                            if (upIdx >= 0) focusRequesters[upIdx].requestFocus()
                                        },
                                        onNavigateDown = {
                                            val downIdx = listIdx + 3
                                            if (downIdx < items.size) focusRequesters[downIdx].requestFocus()
                                        },
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