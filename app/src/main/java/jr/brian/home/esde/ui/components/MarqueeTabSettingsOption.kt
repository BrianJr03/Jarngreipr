package jr.brian.home.esde.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun MarqueeTabSettingsOption(
    pageIndex: Int,
    isVisible: Boolean,
    isOverlayEnabled: Boolean,
    onVisibilityToggle: () -> Unit,
    onOverlayToggle: () -> Unit,
    showOverlayOption: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    var isOverlayFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = OledBackgroundColor,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(animatedFocusedScale(isFocused))
                .onFocusChanged { isFocused = it.isFocused }
                .background(
                    color = if (isFocused) ThemePrimaryColor.copy(alpha = 0.1f) else Color.Transparent
                )
                .clickable { onVisibilityToggle() }
                .focusable()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.esde_settings_marquee_tab_number, pageIndex + 1),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.esde_settings_marquee_show_on_tab),
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = isVisible,
                    onCheckedChange = { onVisibilityToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ThemePrimaryColor,
                        checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.3f)
                    )
                )
            }
        }

        if (isVisible && showOverlayOption) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(animatedFocusedScale(isOverlayFocused))
                    .onFocusChanged { isOverlayFocused = it.isFocused }
                    .background(
                        color = if (isOverlayFocused) ThemePrimaryColor.copy(alpha = 0.1f)
                        else Color.DarkGray.copy(alpha = 0.15f)
                    )
                    .clickable { onOverlayToggle() }
                    .focusable()
                    .padding(start = 28.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.esde_settings_marquee_overlay_on_tab),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = stringResource(R.string.esde_settings_marquee_overlay_on_tab_description),
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isOverlayEnabled,
                        onCheckedChange = { onOverlayToggle() },
                        modifier = Modifier.scale(0.85f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThemePrimaryColor,
                            checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}
