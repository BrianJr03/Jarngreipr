package jr.brian.home.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.GridSettingsManager
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun GridColumnSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {},
    totalAppsCount: Int = 0
) {
    val gridSettingsManager = LocalGridSettingsManager.current
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }
    val columnsMinusFocusRequester = remember { FocusRequester() }

    LaunchedEffect(totalAppsCount) {
        gridSettingsManager.setTotalAppsCount(totalAppsCount)
        gridSettingsManager.initializeDefaultRows(totalAppsCount)
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            columnsMinusFocusRequester.requestFocus()
        } else {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                    )
                } else {
                    listOf(
                        OledCardLightColor,
                        OledCardColor,
                    )
                },
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester ?: mainCardFocusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .background(
                            brush = cardGradient,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            brush =
                                borderBrush(
                                    isFocused = isFocused,
                                    colors =
                                        listOf(
                                            ThemePrimaryColor.copy(alpha = 0.8f),
                                            ThemeSecondaryColor.copy(alpha = 0.6f),
                                        ),
                                ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onExpandChanged(true)
                        }
                        .focusable()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = stringResource(R.string.settings_grid_columns_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_grid_columns_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_grid_columns_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val unlimitedMode = gridSettingsManager.unlimitedMode
                val gridCapacity = gridSettingsManager.columnCount * gridSettingsManager.rowCount
                val hiddenAppsCount = (totalAppsCount - gridCapacity).coerceAtLeast(0)

                GridLayoutLabel(
                    gridCapacity = gridCapacity,
                    hiddenAppsCount = hiddenAppsCount,
                    totalAppsCount = totalAppsCount,
                    unlimitedMode = unlimitedMode
                )

                GridControlButton(
                    text = stringResource(R.string.settings_grid_reset),
                    onClick = {
                        gridSettingsManager.resetToDefault(totalAppsCount)
                    },
                    isPrimary = false,
                    isSpecial = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                GridDimensionSelector(
                    label = stringResource(R.string.settings_grid_columns_label),
                    value = gridSettingsManager.columnCount,
                    minValue = GridSettingsManager.MIN_COLUMNS,
                    maxValue = GridSettingsManager.MAX_COLUMNS,
                    onValueChange = { newValue ->
                        gridSettingsManager.updateColumnCount(newValue)
                    },
                    minusFocusRequester = columnsMinusFocusRequester,
                )

                GridDimensionSelector(
                    label = stringResource(R.string.settings_grid_rows_label),
                    value = gridSettingsManager.rowCount,
                    minValue = GridSettingsManager.MIN_ROWS,
                    maxValue = gridSettingsManager.getMaxRows(),
                    onValueChange = { newValue ->
                        gridSettingsManager.updateRowCount(newValue)
                    },
                )

                GridControlButton(
                    text = stringResource(R.string.settings_grid_done),
                    onClick = { onExpandChanged(false) },
                    isPrimary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun GridDimensionSelector(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    minusFocusRequester: FocusRequester? = null,
    plusFocusRequester: FocusRequester? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GridControlButton(
                text = stringResource(R.string.settings_grid_minus),
                onClick = {
                    if (value > minValue) {
                        onValueChange(value - 1)
                    }
                },
                enabled = value > minValue,
                modifier = Modifier.weight(1f),
                focusRequester = minusFocusRequester
            )

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = OledCardColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = ThemePrimaryColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            GridControlButton(
                text = stringResource(R.string.settings_grid_plus),
                onClick = {
                    if (value < maxValue) {
                        onValueChange(value + 1)
                    }
                },
                enabled = value < maxValue,
                modifier = Modifier.weight(1f),
                focusRequester = plusFocusRequester
            )
        }
    }
}

@Composable
private fun GridLayoutLabel(
    gridCapacity: Int,
    hiddenAppsCount: Int,
    totalAppsCount: Int,
    unlimitedMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (unlimitedMode || hiddenAppsCount == 0) {
                    ThemePrimaryColor.copy(alpha = 0.15f)
                } else {
                    ThemeSecondaryColor.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (unlimitedMode || hiddenAppsCount == 0) {
                    ThemePrimaryColor.copy(alpha = 0.5f)
                } else {
                    ThemeSecondaryColor.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = if (unlimitedMode) {
                if (totalAppsCount == 1) {
                    stringResource(
                        R.string.settings_grid_unlimited_mode_singular,
                        totalAppsCount
                    )
                } else {
                    stringResource(
                        R.string.settings_grid_unlimited_mode_plural,
                        totalAppsCount
                    )
                }
            } else if (hiddenAppsCount > 0) {
                if (gridCapacity == 1 && hiddenAppsCount == 1) {
                    stringResource(
                        R.string.settings_grid_apps_hidden_singular,
                        gridCapacity,
                        hiddenAppsCount
                    )
                } else {
                    stringResource(
                        R.string.settings_grid_apps_hidden_plural,
                        gridCapacity,
                        hiddenAppsCount
                    )
                }
            } else {
                if (gridCapacity == 1) {
                    stringResource(
                        R.string.settings_grid_all_apps_visible_singular,
                        gridCapacity
                    )
                } else {
                    stringResource(
                        R.string.settings_grid_all_apps_visible_plural,
                        gridCapacity
                    )
                }
            },
            color = if (unlimitedMode || hiddenAppsCount == 0) {
                ThemePrimaryColor
            } else {
                ThemeSecondaryColor
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GridControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    isSpecial: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = when {
            !enabled -> listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
            isFocused && isSpecial -> listOf(
                ThemePrimaryColor.copy(alpha = 1f),
                ThemeSecondaryColor.copy(alpha = 0.9f),
            )

            isFocused -> listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )

            isSpecial -> listOf(
                ThemePrimaryColor.copy(alpha = 0.5f),
                ThemeSecondaryColor.copy(alpha = 0.3f),
            )
            isPrimary -> listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.4f),
            )

            else -> listOf(
                OledCardLightColor,
                OledCardColor,
            )
        }
    )

    val borderColor = when {
        !enabled -> Color.Gray.copy(alpha = 0.3f)
        isFocused -> Color.White
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .scale(if (isFocused) 1.05f else 1f)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = when {
                isPrimary -> 18.sp
                isSpecial -> 16.sp
                else -> 24.sp
            },
            fontWeight = FontWeight.Bold,
        )
    }
}
