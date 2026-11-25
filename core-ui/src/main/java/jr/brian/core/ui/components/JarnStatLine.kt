package jr.brian.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.rememberThemedStyle

/** The separator between stats that share one line. */
const val JARN_STAT_SEPARATOR = "  ·  "

/**
 * Whether [count] stats should stack, one per line: only when there is more than one and their
 * joined line, [joinedWidth] wide, runs past [maxWidth]. An unbounded width always fits.
 */
fun jarnStatsStack(count: Int, joinedWidth: Int, maxWidth: Int): Boolean =
    count > 1 && maxWidth != Constraints.Infinity && joinedWidth > maxWidth

/**
 * Short stats such as play time and last played, joined by [separator] on one line while they fit
 * and stacked one per line once they don't, so a wide font never cuts a stat in half. With
 * [fallback], [style] goes through [rememberThemedStyle] for the joined text, so a look's font is
 * only used when it covers every stat.
 */
@Composable
fun JarnStatLine(
    stats: List<String>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    separator: String = JARN_STAT_SEPARATOR,
    fallback: TextStyle? = null,
) {
    if (stats.isEmpty()) return
    val joined = stats.joinToString(separator)
    val resolved = fallback?.let { rememberThemedStyle(joined, style, it) } ?: style
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val joinedWidth = remember(joined, resolved, measurer) {
            measurer.measure(joined, resolved, maxLines = 1, softWrap = false).size.width
        }
        if (jarnStatsStack(stats.size, joinedWidth, constraints.maxWidth)) {
            JarnStackedStats(stats, resolved, color)
        } else {
            JarnText(joined, resolved, color, maxLines = 1)
        }
    }
}

@Composable
private fun JarnStackedStats(stats: List<String>, style: TextStyle, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        stats.forEach { JarnText(it, style, color, maxLines = 1) }
    }
}
