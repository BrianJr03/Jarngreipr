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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.JoystickThemeManager
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalJoystickThemeManager
import jr.brian.home.ui.theme.managers.LocalThemeManager

@Composable
fun JoystickThemeSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {}
) {
    val joystickManager = LocalJoystickThemeManager.current
    val themeManager = LocalThemeManager.current
    val isThemeEnabled by joystickManager.isThemeEnabled.collectAsStateWithLifecycle()
    val isFlashingEnabled by joystickManager.isFlashingEnabled.collectAsStateWithLifecycle()
    val flashSpeed by joystickManager.flashSpeed.collectAsStateWithLifecycle()
    
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            mainCardFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isThemeEnabled, isFlashingEnabled, flashSpeed, themeManager.currentTheme) {
        if (isThemeEnabled) {
            joystickManager.applyThemeColors(
                themeManager.currentTheme.primaryColor,
                themeManager.currentTheme.secondaryColor
            )
        }
    }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
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

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier = Modifier
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
                        brush = borderBrush(
                            isFocused = isFocused,
                            colors = listOf(
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
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = stringResource(R.string.settings_joystick_theme_icon_description),
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_joystick_theme_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_joystick_theme_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }

                    Text(
                        text = if (isThemeEnabled) {
                            stringResource(R.string.settings_toggle_on)
                        } else {
                            stringResource(R.string.settings_toggle_off)
                        },
                        color = if (isThemeEnabled) Color.Green else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
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
                JoystickThemeToggle(
                    enabled = isThemeEnabled,
                    onToggle = { 
                        joystickManager.setThemeEnabled(!isThemeEnabled)
                    }
                )

                if (isThemeEnabled) {
                    JoystickFlashingToggle(
                        enabled = isFlashingEnabled,
                        onToggle = {
                            joystickManager.setFlashingEnabled(!isFlashingEnabled)
                        }
                    )

                    if (isFlashingEnabled) {
                        JoystickSpeedSlider(
                            currentSpeed = flashSpeed,
                            onSpeedChange = { speed ->
                                joystickManager.setFlashSpeed(speed)
                            }
                        )
                    }
                }

                JoystickControlButton(
                    text = stringResource(R.string.settings_grid_done),
                    onClick = { onExpandChanged(false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun JoystickThemeToggle(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )
        } else {
            listOf(
                OledCardLightColor,
                OledCardColor,
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .focusable()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_joystick_theme_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            
            Box(
                modifier = Modifier
                    .size(50.dp, 28.dp)
                    .background(
                        color = if (enabled) ThemePrimaryColor else Color.Gray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun JoystickFlashingToggle(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )
        } else {
            listOf(
                OledCardLightColor,
                OledCardColor,
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .focusable()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_joystick_flashing_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_joystick_flashing_description),
                    color = Color.Gray,
                    fontSize = 13.sp,
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(
                modifier = Modifier
                    .size(50.dp, 28.dp)
                    .background(
                        color = if (enabled) ThemePrimaryColor else Color.Gray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun JoystickSpeedSlider(
    currentSpeed: JoystickThemeManager.FlashSpeed,
    onSpeedChange: (JoystickThemeManager.FlashSpeed) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var sliderValue by remember(currentSpeed) { 
        mutableFloatStateOf(currentSpeed.value.toFloat()) 
    }

    val gradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )
        } else {
            listOf(
                OledCardLightColor,
                OledCardColor,
            )
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .focusable()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_joystick_speed_label),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Slider(
            value = sliderValue,
            onValueChange = { 
                sliderValue = it
            },
            onValueChangeFinished = {
                val speed = when (sliderValue.toInt()) {
                    0 -> JoystickThemeManager.FlashSpeed.SLOW
                    1 -> JoystickThemeManager.FlashSpeed.MEDIUM
                    else -> JoystickThemeManager.FlashSpeed.FAST
                }
                onSpeedChange(speed)
            },
            valueRange = 0f..2f,
            steps = 1,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SpeedLabel(
                text = stringResource(R.string.settings_joystick_speed_slow),
                isSelected = sliderValue.toInt() == 0
            )
            SpeedLabel(
                text = stringResource(R.string.settings_joystick_speed_medium),
                isSelected = sliderValue.toInt() == 1
            )
            SpeedLabel(
                text = stringResource(R.string.settings_joystick_speed_fast),
                isSelected = sliderValue.toInt() == 2
            )
        }
    }
}

@Composable
private fun SpeedLabel(text: String, isSelected: Boolean) {
    Text(
        text = text,
        color = if (isSelected) ThemePrimaryColor else Color.Gray,
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun JoystickControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.4f),
            )
        }
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .scale(if (isFocused) 1.05f else 1f)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
