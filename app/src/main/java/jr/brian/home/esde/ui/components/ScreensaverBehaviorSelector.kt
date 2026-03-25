package jr.brian.home.esde.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.model.ScreensaverBehavior
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun ScreensaverBehaviorSelector(
    selectedBehavior: ScreensaverBehavior,
    onBehaviorSelected: (ScreensaverBehavior) -> Unit
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
        Text(
            text = stringResource(R.string.esde_settings_screensaver_behavior),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = stringResource(R.string.esde_settings_screensaver_behavior_description),
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScreensaverBehavior.entries.forEach { behavior ->
                ScreensaverBehaviorChip(
                    behavior = behavior,
                    isSelected = behavior == selectedBehavior,
                    onClick = { onBehaviorSelected(behavior) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScreensaverBehaviorChip(
    behavior: ScreensaverBehavior,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val behaviorName = when (behavior) {
        ScreensaverBehavior.ShowContent -> stringResource(R.string.esde_settings_screensaver_show_companion)
        ScreensaverBehavior.PowerOff -> stringResource(R.string.esde_settings_screensaver_power_off)
        ScreensaverBehavior.Floaty -> stringResource(R.string.esde_settings_screensaver_floaty)
        ScreensaverBehavior.ShowAll -> stringResource(R.string.esde_settings_screensaver_show_all)
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .scale(animatedFocusedScale(isFocused))
            .background(
                color = when {
                    isSelected -> ThemePrimaryColor.copy(alpha = 0.7f)
                    isFocused -> ThemePrimaryColor.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected || isFocused) 1.dp else 0.dp,
                color = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = behaviorName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
