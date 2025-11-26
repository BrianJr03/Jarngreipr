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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.LocalPowerSettingsManager
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun ThorSettingsItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {}
) {
    val powerSettingsManager = LocalPowerSettingsManager.current
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
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
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.settings_thor_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_thor_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_thor_description),
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThorSettingToggleButton(
                    text = stringResource(id = R.string.settings_thor_power_button),
                    isChecked = isPowerButtonVisible,
                    onClick = {
                        powerSettingsManager.setPowerButtonVisibility(!isPowerButtonVisible)
                        onExpandChanged(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThorSettingToggleButton(
    text: String,
    isChecked: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                } else {
                    listOf(
                        OledCardLightColor,
                        OledCardColor,
                    )
                },
        )

    val borderColor = when {
        isChecked -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isChecked || isFocused) 2.dp else 0.dp

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(animatedFocusedScale(isFocused))
                .onFocusChanged {
                    isFocused = it.isFocused
                }
                .background(
                    brush = gradient,
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .focusable()
                .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
            )

            Text(
                text = if (isChecked) stringResource(R.string.settings_toggle_on) else stringResource(
                    R.string.settings_toggle_off
                ),
                color = if (isChecked) Color.Green else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
