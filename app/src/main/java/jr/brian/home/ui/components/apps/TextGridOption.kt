package jr.brian.home.ui.components.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun TextGridOption(
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
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
