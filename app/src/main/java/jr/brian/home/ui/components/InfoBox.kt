package jr.brian.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as GraphicsColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun InfoBox(
    label: String,
    content: String,
    isPrimary: Boolean = false,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isPrimary -> Brush.linearGradient(
            colors = listOf(
                ThemePrimaryColor.copy(alpha = 0.15f),
                ThemeSecondaryColor.copy(alpha = 0.15f)
            )
        )

        isWarning -> Brush.linearGradient(
            colors = listOf(
                GraphicsColor(0xFFFF9800).copy(alpha = 0.2f),
                GraphicsColor(0xFFFFC107).copy(alpha = 0.2f)
            )
        )

        else -> Brush.linearGradient(
            colors = listOf(
                GraphicsColor.White.copy(alpha = 0.1f),
                GraphicsColor.White.copy(alpha = 0.05f)
            )
        )
    }

    val borderBrush = when {
        isPrimary -> Brush.linearGradient(
            colors = listOf(
                ThemePrimaryColor.copy(alpha = 0.5f),
                ThemeSecondaryColor.copy(alpha = 0.5f)
            )
        )

        isWarning -> Brush.linearGradient(
            colors = listOf(
                GraphicsColor(0xFFFF9800).copy(alpha = 0.8f),
                GraphicsColor(0xFFFFC107).copy(alpha = 0.8f)
            )
        )

        else -> Brush.linearGradient(
            colors = listOf(
                GraphicsColor.White.copy(alpha = 0.2f),
                GraphicsColor.White.copy(alpha = 0.2f)
            )
        )
    }

    val labelColor = when {
        isPrimary -> ThemePrimaryColor
        isWarning -> GraphicsColor(0xFFFF9800)
        else -> GraphicsColor.White.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = GraphicsColor.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Start,
                lineHeight = 26.sp
            )
        }
    }
}
