package jr.brian.home.esde.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun Modifier.focusableSettingCard(
    isFocused: Boolean,
    cornerRadius: Dp = 16.dp
): Modifier = this
    .scale(animatedFocusedScale(isFocused))
    .background(
        brush = subtleCardGradient(isFocused),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = if (isFocused) 2.dp else 0.dp,
        color = if (isFocused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
        shape = RoundedCornerShape(cornerRadius)
    )

@Composable
fun CollapsibleSection(
    title: String,
    showBorder: Boolean = false,
    initiallyExpanded: Boolean = false,
    onHeaderTap: (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    var isFocused by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    val border = if (showBorder) Modifier.border(
        width = if (isFocused || isExpanded) 2.dp else 1.dp,
        brush = borderBrush(
            isFocused = true,
            colors = if (isFocused || isExpanded) {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.8f),
                    ThemeSecondaryColor.copy(alpha = 0.6f),
                )
            } else {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.4f),
                    ThemeSecondaryColor.copy(alpha = 0.3f),
                )
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) else Modifier

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(animatedFocusedScale(isFocused))
                .background(
                    brush = subtleCardGradient(isFocused || isExpanded),
                    shape = RoundedCornerShape(16.dp)
                )
                .then(border)
                .clip(RoundedCornerShape(16.dp))
                .clickWithHaptic(haptic) {
                    onHeaderTap?.invoke()
                    isExpanded = !isExpanded
                }
                .focusable()
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                badge?.invoke()
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotationAngle }
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    description: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusableSettingCard(isFocused)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = valueText,
                color = if (enabled) ThemePrimaryColor else ThemePrimaryColor.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = ThemePrimaryColor,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                disabledThumbColor = ThemePrimaryColor.copy(alpha = 0.4f),
                disabledActiveTrackColor = ThemePrimaryColor.copy(alpha = 0.4f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun ToggleSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    showToggle: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val clickAction = { onClick?.invoke() ?: onCheckedChange(!checked) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .background(
                brush = subtleCardGradient(isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { clickAction() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        if (showToggle) {
            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemePrimaryColor,
                    checkedTrackColor = ThemeSecondaryColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
fun ToggleSetting(
    title: String,
    description: AnnotatedString,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    showToggle: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val clickAction = { onClick?.invoke() ?: onCheckedChange(!checked) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .background(
                brush = subtleCardGradient(isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { clickAction() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        if (showToggle) {
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemePrimaryColor,
                    checkedTrackColor = ThemeSecondaryColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
fun PathSetting(
    title: String,
    description: String,
    currentPath: String?,
    defaultText: String,
    onSelectPath: () -> Unit,
    onClearPath: () -> Unit,
    fallbackPath: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .background(
                brush = subtleCardGradient(isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { onSelectPath() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            IconButton(onClick = onSelectPath) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Select folder",
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (currentPath != null) {
                IconButton(onClick = onClearPath) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear path",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val displayPath = currentPath ?: fallbackPath ?: defaultText
        val pathColor = when {
            currentPath != null -> ThemePrimaryColor
            fallbackPath != null -> Color.Gray.copy(alpha = 0.8f)
            else -> Color.Gray
        }

        Text(
            text = displayPath,
            color = pathColor,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun MarqueeSizeSetting(
    title: String,
    description: String,
    width: Int,
    height: Int,
    widthLabel: String,
    heightLabel: String,
    resetLabel: String,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onReset: () -> Unit,
    minWidth: Int = 40,
    maxWidth: Int = 600,
    minHeight: Int = 40,
    maxHeight: Int = 600,
    step: Int = 10
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusableSettingCard(isFocused)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = resetLabel,
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size((width / 3).dp, (height / 3).dp)
                    .background(
                        brush = subtleCardGradient(isFocused = true),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = ThemePrimaryColor,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = widthLabel,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.width(60.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { if (width > minWidth) onWidthChange(width - step) },
                    enabled = width > minWidth,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (width > minWidth) ThemePrimaryColor.copy(alpha = 0.2f) else Color.Gray.copy(
                                alpha = 0.1f
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease width",
                        tint = if (width > minWidth) ThemePrimaryColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${width}dp",
                    color = ThemePrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { if (width < maxWidth) onWidthChange(width + step) },
                    enabled = width < maxWidth,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (width < maxWidth) ThemePrimaryColor.copy(alpha = 0.2f) else Color.Gray.copy(
                                alpha = 0.1f
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase width",
                        tint = if (width < maxWidth) ThemePrimaryColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = heightLabel,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.width(60.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { if (height > minHeight) onHeightChange(height - step) },
                    enabled = height > minHeight,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (height > minHeight) ThemePrimaryColor.copy(alpha = 0.2f) else Color.Gray.copy(
                                alpha = 0.1f
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease height",
                        tint = if (height > minHeight) ThemePrimaryColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${height}dp",
                    color = ThemePrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { if (height < maxHeight) onHeightChange(height + step) },
                    enabled = height < maxHeight,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (height < maxHeight) ThemePrimaryColor.copy(alpha = 0.2f) else Color.Gray.copy(
                                alpha = 0.1f
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase height",
                        tint = if (height < maxHeight) ThemePrimaryColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SyncLogoPositionLock(
    esdePrefsState: ESDEPrefsState,
    esdePrefsManager: ESDEPreferencesManager
) {
    val isLogoFreePosEnabled = esdePrefsState.isLogoFreePosEnabled()
    LaunchedEffect(isLogoFreePosEnabled) {
        if (!isLogoFreePosEnabled) {
            esdePrefsManager.setLogoPositionLocked(true)
        }
    }
}
