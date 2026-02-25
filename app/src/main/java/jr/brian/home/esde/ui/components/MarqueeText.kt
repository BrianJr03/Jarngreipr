package jr.brian.home.esde.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A continuously scrolling text component that auto-scrolls horizontally
 * and loops back to the start when reaching the end of the text.
 *
 * @param text The text to display and scroll
 * @param modifier Modifier to apply to this layout
 * @param scrollSpeed Speed of scrolling in dp per second
 * @param textStyle Style for the text
 * @param backgroundColor Background color of the scrolling box
 * @param pauseDurationMs Duration to pause at the start before scrolling begins (and after looping)
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    scrollSpeed: Float = 50f,
    textStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    pauseDurationMs: Int = 2000
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var textWidth by remember { mutableFloatStateOf(0f) }
    var shouldScroll by remember { mutableStateOf(false) }
    
    // Measure text width
    LaunchedEffect(text, textStyle) {
        val textLayoutResult = textMeasurer.measure(text, textStyle)
        textWidth = textLayoutResult.size.width.toFloat()
    }
    
    // Determine if scrolling is needed
    LaunchedEffect(textWidth, containerWidth) {
        shouldScroll = textWidth > containerWidth && containerWidth > 0
    }
    
    // Calculate scroll distance and duration
    val scrollDistance = if (shouldScroll) textWidth + containerWidth else 0f
    val scrollDurationMs = if (shouldScroll && scrollSpeed > 0) {
        ((scrollDistance / scrollSpeed) * 1000).toInt()
    } else {
        1000
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (shouldScroll) -scrollDistance else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = scrollDurationMs + pauseDurationMs,
                easing = LinearEasing,
                delayMillis = pauseDurationMs
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "marqueeOffset"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (shouldScroll) "$text          $text" else text,
            style = textStyle,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset {
                IntOffset(
                    x = if (shouldScroll) offsetX.toInt() else 0,
                    y = 0
                )
            }
        )
    }
}

/**
 * A vertically scrolling description box that continuously scrolls
 * and loops back when reaching the end.
 */
@Composable
fun ScrollingDescriptionBox(
    description: String,
    modifier: Modifier = Modifier,
    scrollSpeed: Float = 30f,
    textStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    pauseDurationMs: Int = 3000
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var textHeight by remember { mutableFloatStateOf(0f) }
    var shouldScroll by remember { mutableStateOf(false) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    
    // Measure text height based on container width
    LaunchedEffect(description, textStyle, containerWidth) {
        if (containerWidth > 0) {
            val textLayoutResult = textMeasurer.measure(
                text = description,
                style = textStyle,
                constraints = androidx.compose.ui.unit.Constraints(
                    maxWidth = containerWidth.toInt()
                )
            )
            textHeight = textLayoutResult.size.height.toFloat()
        }
    }
    
    // Determine if scrolling is needed
    LaunchedEffect(textHeight, containerHeight) {
        shouldScroll = textHeight > containerHeight && containerHeight > 0
    }
    
    // Calculate scroll distance and duration
    val scrollDistance = if (shouldScroll) textHeight + containerHeight else 0f
    val scrollDurationMs = if (shouldScroll && scrollSpeed > 0) {
        ((scrollDistance / scrollSpeed) * 1000).toInt()
    } else {
        1000
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "descriptionMarquee")
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (shouldScroll) -scrollDistance else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = scrollDurationMs + pauseDurationMs,
                easing = LinearEasing,
                delayMillis = pauseDurationMs
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "descriptionOffset"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp)
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                containerHeight = coordinates.size.height.toFloat()
                containerWidth = coordinates.size.width.toFloat()
            },
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = if (shouldScroll) "$description\n\n\n\n$description" else description,
            style = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        x = 0,
                        y = if (shouldScroll) offsetY.toInt() else 0
                    )
                }
        )
    }
}
