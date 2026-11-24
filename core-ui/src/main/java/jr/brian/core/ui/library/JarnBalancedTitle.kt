package jr.brian.core.ui.library

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import jr.brian.core.ui.components.JarnText
import kotlin.math.abs
import kotlin.math.max

private const val TITLE_MAX_LINES = 2
private const val TITLE_MIN_FONT_SP = 24f
private const val TITLE_FONT_STEP_SP = 2f

internal data class JarnTitleFit(val lines: List<String>, val fontSizeSp: Float)

/** Splits [title] between words into the two lines with the narrowest, most even shape. */
internal fun jarnBalancedLines(title: String, widthOf: (String) -> Float): List<String> {
    val words = title.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size < TITLE_MAX_LINES) return listOf(title.trim())
    return (1 until words.size)
        .map { split ->
            listOf(words.take(split).joinToString(" "), words.drop(split).joinToString(" "))
        }
        .minWith(
            compareBy(
                { (first, second) -> max(widthOf(first), widthOf(second)) },
                { (first, second) -> abs(widthOf(first) - widthOf(second)) },
            ),
        )
}

/** The largest size where [title] fits on one line, or on two balanced lines. */
internal fun jarnFitTitle(
    title: String,
    maxFontSp: Float,
    maxWidth: Float,
    maxHeight: Float,
    widthAt: (String, Float) -> Float,
    lineHeightAt: (Float) -> Float,
): JarnTitleFit {
    val minFont = minOf(TITLE_MIN_FONT_SP, maxFontSp)
    var font = maxFontSp
    while (font >= minFont) {
        val size = font
        val width: (String) -> Float = { widthAt(it, size) }
        if (width(title) <= maxWidth && lineHeightAt(size) <= maxHeight) {
            return JarnTitleFit(listOf(title.trim()), size)
        }
        val lines = jarnBalancedLines(title, width)
        val fitsWidth = lines.all { width(it) <= maxWidth }
        if (lines.size == TITLE_MAX_LINES && fitsWidth && lineHeightAt(size) * TITLE_MAX_LINES <= maxHeight) {
            return JarnTitleFit(lines, size)
        }
        font -= TITLE_FONT_STEP_SP
    }
    return JarnTitleFit(jarnBalancedLines(title) { widthAt(it, minFont) }, minFont)
}

@Composable
internal fun JarnBalancedTitle(
    title: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseFont = style.fontSize.value
    fun styleAt(font: Float) = style.copy(
        fontSize = font.sp,
        lineHeight = style.lineHeight * (font / baseFont),
    )
    BoxWithConstraints(modifier) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val fit = remember(title, style, maxWidthPx, maxHeightPx, density) {
            jarnFitTitle(
                title = title,
                maxFontSp = baseFont,
                maxWidth = maxWidthPx,
                maxHeight = maxHeightPx,
                widthAt = { text, font ->
                    measurer.measure(text, styleAt(font), maxLines = 1, softWrap = false).size.width.toFloat()
                },
                lineHeightAt = { font -> with(density) { styleAt(font).lineHeight.toPx() } },
            )
        }
        JarnText(
            text = fit.lines.joinToString("\n"),
            style = styleAt(fit.fontSizeSp),
            color = color,
            modifier = Modifier.semantics {
                heading()
                contentDescription = title
            },
            maxLines = TITLE_MAX_LINES,
        )
    }
}
