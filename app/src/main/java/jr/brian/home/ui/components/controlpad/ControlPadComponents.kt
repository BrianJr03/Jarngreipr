package jr.brian.home.ui.components.controlpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.model.ControlPadItem
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.StatusGreen
import jr.brian.home.ui.theme.StatusOrange
import jr.brian.home.ui.theme.StatusRed
import jr.brian.home.ui.theme.StatusYellow
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.util.shizuku.ShizukuInputManager

@Composable
fun GamePadCard(
    item: ControlPadItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed)

    val cardGradient = Brush.linearGradient(
        colors = if (isSelected || isPressed) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.9f)
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.4f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        }
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .offset(y = offsetY)
            .scale(pressScale)
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected || isPressed) 3.dp else 2.dp,
                brush = if (isSelected || isPressed) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.6f),
                            ThemeSecondaryColor.copy(alpha = 0.4f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when {
                            event.changes.any { it.pressed } && !isPressed -> {
                                isPressed = true
                                onPress()
                            }
                            event.changes.none { it.pressed } && isPressed -> {
                                isPressed = false
                                onRelease()
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.label,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isEditMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.control_pad_tap_to_map),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GamePadActionButton(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused || isHighlighted) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.9f)
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.4f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        }
    )

    Box(
        modifier = Modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused || isHighlighted) 3.dp else 2.dp,
                brush = if (isFocused || isHighlighted) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.6f),
                            ThemeSecondaryColor.copy(alpha = 0.4f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ShizukuStatusIndicator(
    isShizukuAvailable: Boolean,
    isPermissionGranted: Boolean,
    isServiceConnected: Boolean,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        isServiceConnected -> StatusGreen
        isPermissionGranted -> StatusYellow
        isShizukuAvailable -> StatusOrange
        else -> StatusRed
    }

    val statusText = when {
        isServiceConnected -> stringResource(R.string.control_pad_shizuku_ready)
        isPermissionGranted -> stringResource(R.string.control_pad_shizuku_connecting)
        isShizukuAvailable -> stringResource(R.string.control_pad_shizuku_permission_required)
        else -> stringResource(R.string.control_pad_shizuku_not_running)
    }

    val statusIcon = when {
        isServiceConnected -> Icons.Default.Check
        isPermissionGranted -> Icons.Default.Refresh
        else -> Icons.Default.Lock
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = OledCardColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = statusColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = isShizukuAvailable && !isPermissionGranted) {
                    ShizukuInputManager.requestPermission()
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )

            Text(
                text = statusText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            if (isShizukuAvailable && !isPermissionGranted) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .background(
                    color = OledCardColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable { onHelpClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.control_pad_help),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BottomActionButtons(
    isEditMode: Boolean,
    onResetClick: () -> Unit,
    onEditToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            GamePadActionButton(
                text = stringResource(R.string.control_pad_reset),
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.control_pad_reset),
                isHighlighted = false,
                onClick = onResetClick
            )
        }

        GamePadActionButton(
            text = if (isEditMode) stringResource(R.string.control_pad_done) else stringResource(R.string.control_pad_edit),
            icon = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
            contentDescription = if (isEditMode) stringResource(R.string.control_pad_done) else stringResource(R.string.control_pad_edit),
            isHighlighted = isEditMode,
            onClick = onEditToggle
        )
    }
}

@Composable
fun GamePadButtonColumn(
    gamePadItems: List<ControlPadItem>,
    indices: IntRange,
    isEditMode: Boolean,
    selectedCardIndex: Int,
    onCardPress: (Int) -> Unit,
    onCardRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in indices) {
            GamePadCard(
                item = gamePadItems[i],
                isEditMode = isEditMode,
                isSelected = selectedCardIndex == i,
                onPress = { onCardPress(i) },
                onRelease = { onCardRelease(i) }
            )
        }
    }
}
