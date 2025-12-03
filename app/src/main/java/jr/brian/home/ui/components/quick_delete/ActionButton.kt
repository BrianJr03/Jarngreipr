package jr.brian.home.ui.components.quick_delete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isPrimary: Boolean = true,
    isCompact: Boolean = false
) {
    val backgroundBrush = if (isPrimary) {
        Brush.horizontalGradient(
            colors = listOf(
                ThemePrimaryColor.copy(alpha = if (enabled) 0.8f else 0.3f),
                ThemeSecondaryColor.copy(alpha = if (enabled) 0.6f else 0.3f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (enabled) 0.1f else 0.05f),
                Color.White.copy(alpha = if (enabled) 0.15f else 0.05f)
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = backgroundBrush,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isPrimary) {
                    ThemePrimaryColor.copy(alpha = if (enabled) 0.5f else 0.2f)
                } else {
                    Color.White.copy(alpha = if (enabled) 0.3f else 0.1f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(
                vertical = if (isCompact) 10.dp else 16.dp,
                horizontal = if (isCompact) 12.dp else 20.dp
            )
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
            )
        }

        Spacer(modifier = Modifier.size(if (isCompact) 8.dp else 12.dp))

        Text(
            text = text,
            color = Color.White,
            fontSize = if (isCompact) 13.sp else 16.sp,
            fontWeight = if (isCompact) FontWeight.SemiBold else FontWeight.Bold
        )
    }
}
