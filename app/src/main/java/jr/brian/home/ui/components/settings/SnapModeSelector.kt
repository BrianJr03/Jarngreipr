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
import jr.brian.home.data.SnapMode
import jr.brian.home.esde.ui.components.focusableSettingCard
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun SnapModeSelector(
    selectedMode: SnapMode,
    onModeSelected: (SnapMode) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val choices = listOf(SnapMode.ICON, SnapMode.GRID)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusableSettingCard(isFocused)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_layout_snap_mode),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.settings_layout_snap_mode_description),
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.forEach { mode ->
                SnapModeChip(
                    mode = mode,
                    isSelected = mode == selectedMode,
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SnapModeChip(
    mode: SnapMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val label = when (mode) {
        SnapMode.ICON -> stringResource(R.string.settings_layout_snap_mode_icon)
        SnapMode.GRID -> stringResource(R.string.settings_layout_snap_mode_grid)
        SnapMode.OFF -> ""
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
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
