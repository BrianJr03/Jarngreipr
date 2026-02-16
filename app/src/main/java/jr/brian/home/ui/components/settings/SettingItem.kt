package jr.brian.home.ui.components.settings

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.util.SettingsTag

private val shape = RoundedCornerShape(16.dp)

@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    trailing: @Composable (() -> Unit)? = null,
    tag: SettingsTag? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)
    val focusedColors =
        listOf(ThemePrimaryColor.copy(alpha = 0.8f), ThemeSecondaryColor.copy(alpha = 0.6f))
    val unfocusedColors = listOf(OledCardLightColor, OledCardColor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = pressOffsetY)
            .scale(pressScale * animatedFocusedScale(isFocused))
            .then(focusRequester?.let {
                Modifier.focusRequester(it)
            } ?: Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                Brush.linearGradient(if (isFocused) focusedColors else unfocusedColors),
                shape
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                brush = borderBrush(isFocused, focusedColors), shape
            )
            .clip(shape)
            .pressWithHaptic(
                onClick,
                haptic = haptic,
                onPressChange = { isPressed = it })
            .clickable { onClick() }
            .focusable()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.settings_coffee_icon_description),
                modifier =
                    Modifier
                        .size(32.dp)
                        .rotate(animatedRotation(isFocused)),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (isFocused) 18.sp else 16.sp,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    )
                    if (tag != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = ThemeAccentColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = ThemeAccentColor.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(id = tag.stringRes),
                                color = ThemeAccentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                    fontSize = 14.sp,
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.size(12.dp))
                trailing()
            }
        }
    }
}
