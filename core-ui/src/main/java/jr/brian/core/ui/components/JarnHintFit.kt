package jr.brian.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints

/**
 * Which hints still fit in [available] px, as indexes into [widths]. Hints never get cut: when
 * the row is too long, whole hints drop in [jarnHintDropRank] order, the later one first among
 * equals, until the rest fit with [gap] between them.
 */
internal fun jarnHintsThatFit(keys: List<JarnKey>, widths: List<Int>, available: Int, gap: Int): Set<Int> {
    val kept = keys.indices.toMutableSet()
    fun total() = kept.sumOf { widths[it] } + gap * (kept.size - 1).coerceAtLeast(0)
    val dropOrder = keys.indices.sortedWith(compareByDescending<Int> { jarnHintDropRank(keys[it]) }.thenByDescending { it })
    for (index in dropOrder) {
        if (total() <= available) break
        kept -= index
    }
    return kept
}

/**
 * Higher drops first. Tabs also step with the shoulders the player already holds, Select's
 * options and Search have their own screens, and the action keys and screen switch go last.
 */
internal fun jarnHintDropRank(key: JarnKey): Int = when (key) {
    JarnKey.L1R1, JarnKey.L1, JarnKey.R1, JarnKey.L2R2, JarnKey.L2, JarnKey.R2 -> 3
    JarnKey.Select -> 2
    JarnKey.Y -> 1
    else -> 0
}

/**
 * Lays out [leading] hints from the start and [trailing] ones at the end, dropping whole hints
 * that don't fit (see [jarnHintsThatFit]) instead of letting the row run off the screen.
 */
@Composable
internal fun JarnFittedHints(
    leading: List<JarnHint>,
    trailing: List<JarnHint>,
    gap: Dp,
    modifier: Modifier = Modifier,
    item: @Composable (JarnHint) -> Unit,
) {
    val all = leading + trailing
    Layout(
        content = { all.forEach { item(it) } },
        modifier = modifier,
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        val kept = jarnHintsThatFit(all.map { it.key }, placeables.map { it.width }, width, gapPx)
        val height = placeables.maxOfOrNull { it.height }?.coerceIn(constraints.minHeight, constraints.maxHeight)
            ?: constraints.minHeight
        layout(width, height) {
            var x = 0
            leading.indices.filter { it in kept }.forEach { index ->
                val placeable = placeables[index]
                placeable.place(x, (height - placeable.height) / 2)
                x += placeable.width + gapPx
            }
            var end = width
            trailing.indices.reversed().map { leading.size + it }.filter { it in kept }.forEach { index ->
                val placeable = placeables[index]
                end -= placeable.width
                placeable.place(end, (height - placeable.height) / 2)
                end -= gapPx
            }
        }
    }
}
