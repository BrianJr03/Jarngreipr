package jr.brian.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.AppCardLight
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun WallpaperOptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        isSelected -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isSelected || isFocused) 2.dp else 0.dp

    Box(
        modifier =
            modifier
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
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
