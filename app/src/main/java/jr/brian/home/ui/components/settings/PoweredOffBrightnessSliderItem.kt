package jr.brian.home.ui.components.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.PowerSettingsManager
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager

@Composable
fun PoweredOffBrightnessSliderItem() {
    val powerSettingsManager = LocalPowerSettingsManager.current
    val currentBrightness by powerSettingsManager.poweredOffBrightness.collectAsStateWithLifecycle()
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .background(
                    brush = subtleCardGradient(isFocused),
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
                .focusable()
                .padding(16.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = stringResource(R.string.settings_powered_off_brightness_title),
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_powered_off_brightness_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_powered_off_brightness_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }

                    Text(
                        text = "$currentBrightness%",
                        color = ThemePrimaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = currentBrightness.toFloat(),
                    onValueChange = { powerSettingsManager.setPoweredOffBrightness(it.toInt()) },
                    valueRange = PowerSettingsManager.MIN_POWERED_OFF_BRIGHTNESS.toFloat()..PowerSettingsManager.MAX_POWERED_OFF_BRIGHTNESS.toFloat(),
                    steps = (PowerSettingsManager.MAX_POWERED_OFF_BRIGHTNESS - PowerSettingsManager.MIN_POWERED_OFF_BRIGHTNESS) / 5 - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = ThemePrimaryColor,
                        activeTrackColor = ThemePrimaryColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}
