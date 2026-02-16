package jr.brian.home.ui.components.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun SearchAppOptionsMenuContent(
    currentDisplayPreference: DisplayPreference,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    hasExternalDisplay: Boolean,
    focusRequesters: List<FocusRequester>,
    onFocusedIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit
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

        if (hasExternalDisplay) {
            add(
                GridItem.TextItem(
                    text = stringResource(R.string.app_options_launch_primary_descr),
                    onClick = {
                        onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY)
                        onDismiss()
                    },
                    isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
                    index = 1
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
                    index = 2
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
                        is GridItem.IconItem -> SearchIconGridOption(
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
                                // No bottom row navigation needed
                            },
                            onFocusChanged = { focused ->
                                if (focused) onFocusedIndexChange(item.index)
                            }
                        )

                        is GridItem.TextItem -> SearchTextGridOption(
                            text = item.text,
                            onClick = item.onClick,
                            isSelected = item.isSelected,
                            focusRequester = focusRequesters[item.index],
                            onNavigateLeft = {
                                if (item.index > 0) {
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
                                // Stay on top row
                            },
                            onNavigateDown = {
                                // No bottom row navigation needed
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
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .offset(y = pressOffsetY)
            .scale(pressScale)
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
            .pressWithHaptic(
                onClick,
                haptic = haptic,
                onPressChange = { isPressed = it }
            )
            .clickable { onClick() }
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
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .offset(y = pressOffsetY)
            .scale(pressScale)
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
            .pressWithHaptic(
                onClick,
                haptic = haptic,
                onPressChange = { isPressed = it }
            )
            .clickable { onClick() }
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

@Composable
fun rememberSearchAppOptionsFocusRequesters(
    hasExternalDisplay: Boolean
): List<FocusRequester> {
    return remember(hasExternalDisplay) {
        val count = if (hasExternalDisplay) 3 else 1
        List(count) { FocusRequester() }
    }
}