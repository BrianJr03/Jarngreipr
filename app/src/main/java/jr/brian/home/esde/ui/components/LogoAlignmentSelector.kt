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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.preferences.LogoAlignment
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun LogoAlignmentSelector(
    selectedAlignment: LogoAlignment,
    onAlignmentSelected: (LogoAlignment) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .background(
                brush = Brush.linearGradient(
                    colors = if (isFocused) {
                        listOf(
                            ThemePrimaryColor.copy(alpha = 0.3f),
                            ThemeSecondaryColor.copy(alpha = 0.2f)
                        )
                    } else {
                        listOf(OledCardLightColor, OledCardColor)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.esde_settings_logo_alignment),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogoAlignment.entries.forEach { alignment ->
                LogoAlignmentChip(
                    alignment = alignment,
                    isSelected = alignment == selectedAlignment,
                    onClick = { onAlignmentSelected(alignment) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LogoAlignmentChip(
    alignment: LogoAlignment,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val alignmentName = when (alignment) {
        LogoAlignment.Top -> stringResource(R.string.esde_settings_logo_alignment_top)
        LogoAlignment.Center -> stringResource(R.string.esde_settings_logo_alignment_center)
        LogoAlignment.Bottom -> stringResource(R.string.esde_settings_logo_alignment_bottom)
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
            text = alignmentName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
