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
    onDismiss: () -> Unit
) {
    val items: List<GridItem> = buildList {
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
        if (hasExternalDisplay) {
            add(GridItem.TextItem(
                text = stringResource(R.string.app_options_launch_primary_descr),
                isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                onClick = {
                    onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                    onDismiss()
                }
            ))
            add(GridItem.TextItem(
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
                            is GridItem.IconItem -> SearchIconGridOption(
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
                            is GridItem.TextItem -> SearchTextGridOption(
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
