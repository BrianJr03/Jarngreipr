package jr.brian.home.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun GridControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    isSpecial: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = when {
            !enabled -> listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
            isFocused && isSpecial -> listOf(
                ThemePrimaryColor.copy(alpha = 1f),
                ThemeSecondaryColor.copy(alpha = 0.9f),
            )

            isFocused -> listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )

            isSpecial -> listOf(
                ThemePrimaryColor.copy(alpha = 0.5f),
                ThemeSecondaryColor.copy(alpha = 0.3f),
            )
            isPrimary -> listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.4f),
            )

            else -> listOf(
                OledCardLightColor,
                OledCardColor,
            )
        }
    )

    val borderColor = when {
        !enabled -> Color.Gray.copy(alpha = 0.3f)
        isFocused -> Color.White
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .scale(if (isFocused) 1.05f else 1f)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = when {
                isPrimary -> 18.sp
                isSpecial -> 16.sp
                else -> 24.sp
            },
            fontWeight = FontWeight.Bold,
        )
    }
}
