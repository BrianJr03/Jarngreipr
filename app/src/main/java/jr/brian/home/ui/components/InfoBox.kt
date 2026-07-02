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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.Color as GraphicsColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun InfoBox(
    label: String,
    content: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isWarning: Boolean = false,
    highlightedTerms: List<String> = emptyList(),
    contentTextColor: GraphicsColor = GraphicsColor.White.copy(alpha = 0.95f)
) {
    val backgroundColor = when {
        isPrimary -> subtleCardGradient(true)
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

    val infoBorderBrush = when {
        isPrimary -> borderBrush(true)
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

    val annotatedContent = remember(
        key1 = content,
        key2 = highlightedTerms,
        key3 = contentTextColor
    ) {
        if (highlightedTerms.isEmpty()) {
            AnnotatedString(content)
        } else {
            val ranges = highlightedTerms
                .flatMap { term ->
                    val matches = mutableListOf<IntRange>()
                    var start = content.indexOf(
                        string = term,
                        ignoreCase = true
                    )
                    while (start != -1) {
                        matches.add(start until start + term.length)
                        start = content.indexOf(
                            string = term,
                            startIndex = start + term.length,
                            ignoreCase = true
                        )
                    }
                    matches
                }.sortedBy { it.first }

            buildAnnotatedString {
                append(content)
                ranges.forEach { range ->
                    addStyle(
                        style = SpanStyle(color = contentTextColor),
                        start = range.first,
                        end = range.last + 1
                    )
                }
            }
        }
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
                brush = infoBorderBrush,
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
                text = annotatedContent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = GraphicsColor.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Start,
                lineHeight = 26.sp
            )
        }
    }
}
