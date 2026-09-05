package jr.brian.home.ui.components.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.GridItem
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.util.rememberAutoFocus

@Composable
fun SearchAppOptionsMenuContent(
    currentDisplayPreference: DisplayPreference,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    onRenameClick: () -> Unit = {},
    hasExternalDisplay: Boolean,
    onDismiss: () -> Unit,
    promptForDisplayOnLaunch: Boolean = false,
    onPromptForDisplayOnLaunchChange: ((Boolean) -> Unit)? = null
) {
    val mainItems: List<GridItem> = buildList {
        add(GridItem.IconItem(
            icon = Icons.Default.Info,
            label = stringResource(R.string.app_options_info),
            onClick = { onAppInfoClick(); onDismiss() }
        ))
        add(GridItem.IconItem(
            icon = Icons.Default.Edit,
            label = stringResource(R.string.app_options_rename),
            onClick = onRenameClick
        ))
    }

    val launchItems: List<GridItem> = buildList {
        if (!hasExternalDisplay) return@buildList
        add(GridItem.IconItem(
            icon = Icons.AutoMirrored.Filled.Launch,
            label = stringResource(R.string.app_options_launch_primary_short),
            isSelected = !promptForDisplayOnLaunch &&
                currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
            onClick = {
                onPromptForDisplayOnLaunchChange?.invoke(false)
                onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                onDismiss()
            }
        ))
        add(GridItem.IconItem(
            icon = Icons.AutoMirrored.Filled.Launch,
            label = stringResource(R.string.app_options_launch_external_short),
            isSelected = !promptForDisplayOnLaunch &&
                currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
            onClick = {
                onPromptForDisplayOnLaunchChange?.invoke(false)
                onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY)
                onDismiss()
            }
        ))
        if (onPromptForDisplayOnLaunchChange != null) {
            add(GridItem.IconItem(
                icon = Icons.AutoMirrored.Filled.Launch,
                label = stringResource(R.string.app_options_launch_ask_short),
                isSelected = promptForDisplayOnLaunch,
                onClick = {
                    onPromptForDisplayOnLaunchChange(true)
                    onDismiss()
                }
            ))
        }
    }

    val rows: List<List<GridItem>> = buildList {
        if (launchItems.isNotEmpty()) add(launchItems)
        addAll(mainItems.chunked(3))
    }
    val flatItems: List<GridItem> = rows.flatten()
    val rowStartOffsets: List<Int> = rows.runningFold(0) { acc, row -> acc + row.size }

    val firstFocusRequester = rememberAutoFocus()
    val focusRequesters = remember(flatItems.size, firstFocusRequester) {
        List(flatItems.size) { i -> if (i == 0) firstFocusRequester else FocusRequester() }
    }

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
                            is GridItem.IconItem -> SearchIconGridOption(
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
                            is GridItem.TextItem -> SearchTextGridOption(
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

@Composable
private fun SearchIconGridOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    onNavigateLeft: () -> Unit,
    onNavigateRight: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    var isFocused by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val contentColor = if (isSelected) ThemePrimaryColor else Color.White

    Box(
        modifier = modifier
            .aspectRatio(1f)
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
            .clickWithHaptic(haptic) { onClick() }
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
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchTextGridOption(
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
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
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
            .clickWithHaptic(haptic) { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) ThemePrimaryColor else Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
