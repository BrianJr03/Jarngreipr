package jr.brian.home.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun IconBox(
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) ThemeAccentColor.copy(alpha = 0.3f) else Color.Black.copy(
            alpha = 0.75f
        ),
        label = "iconBoxBackgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) ThemeAccentColor else Color.White.copy(alpha = 0.2f),
        label = "iconBoxBorderColor"
    )

    Box(
        modifier = modifier
            .offset(y = pressOffsetY)
            .scale(pressScale)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .then(
                if (onFocusChanged != null) {
                    Modifier.onFocusChanged { onFocusChanged(it.isFocused) }
                } else {
                    Modifier
                }
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .pressWithHaptic(
                            onClick,
                            haptic = haptic,
                            onPressChange = { isPressed = it }
                        )
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .focusable()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        content = { icon() }
    )
}