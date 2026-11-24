package jr.brian.jarnfe.library

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Immutable
import jr.brian.core.ui.theme.JarnMotionToken
import kotlin.math.abs
import kotlin.random.Random

/** Rows a spin always travels, so a nearby pick still reads as a spin. */
internal const val LIBRARY_SPIN_MIN_ROWS = 12

/** Cards a carousel spin always passes. */
internal const val LIBRARY_SPIN_MIN_STEPS = 14

private const val EASING_SEARCH_STEPS = 24

/**
 * A request for the open game list to move to [gameId] on its own and focus it there. [spin] runs
 * a long eased spin instead of a plain scroll. [id] tells two requests for one game apart.
 */
@Immutable
internal data class LibraryJump(val systemId: String, val gameId: String, val spin: Boolean, val id: Long)

/** A visible game other than [avoid] when there is more than one to choose from. */
internal fun libraryRandomGame(ids: List<String>, avoid: String?, random: Random): String? {
    val choices = if (ids.size > 1) ids.filterNot { it == avoid } else ids
    return choices.takeIf { it.isNotEmpty() }?.let { it[random.nextInt(it.size)] }
}

/** The system's most recently played game, from the Recent group of its Now shelf. */
internal fun libraryLastPlayed(shelf: List<JarnShelfGame>): JarnLibraryGame? =
    shelf.firstOrNull { it.group == JarnShelfGroup.Recent }?.game

/**
 * The row a grid spin starts from. A target already [LIBRARY_SPIN_MIN_ROWS] away keeps the
 * current row. A closer one starts that far before it, or after it near the top of the list.
 */
internal fun librarySpinStartRow(currentRow: Int, targetRow: Int, lastRow: Int, minRows: Int = LIBRARY_SPIN_MIN_ROWS): Int =
    when {
        abs(targetRow - currentRow) >= minRows -> currentRow
        targetRow - minRows >= 0 -> targetRow - minRows
        else -> minOf(lastRow, targetRow + minRows)
    }

/**
 * How many steps a carousel spin takes forward from [center] to land on [target]. With wrapping
 * (four or more items) it loops round until it has passed at least [minSteps] cards. Without it,
 * it moves straight there, negative meaning backward.
 */
internal fun libraryShelfSpinSteps(center: Int, target: Int, size: Int, minSteps: Int = LIBRARY_SPIN_MIN_STEPS): Int {
    if (size < SHELF_WRAP_MIN_ITEMS) return target - center
    var steps = (target - center).mod(size)
    while (steps < minSteps) steps += size
    return steps
}

/**
 * When each carousel step fires, in milliseconds from the start, so the steps together follow one
 * [easing] curve over [totalMillis]: close together at first, spreading out into the landing.
 */
internal fun libraryShelfSpinTimes(steps: Int, totalMillis: Int, easing: Easing): List<Long> =
    (1..steps).map { step ->
        val goal = step.toFloat() / steps
        var low = 0f
        var high = 1f
        repeat(EASING_SEARCH_STEPS) {
            val mid = (low + high) / 2
            if (easing.transform(mid) < goal) low = mid else high = mid
        }
        (high * totalMillis).toLong()
    }

/**
 * Scrolls by rows toward [index] following [token], with the target's row ending near the middle.
 * A [spin] that would travel only a little starts from further away. Instant when [token] is.
 */
internal suspend fun LazyGridState.scrollToJump(index: Int, token: JarnMotionToken, spin: Boolean) {
    val first = layoutInfo.visibleItemsInfo.firstOrNull()
    if (token.durationMillis == 0 || first == null) {
        scrollToItem(index)
        return
    }
    val columns = layoutInfo.visibleItemsInfo.groupingBy { it.row }.eachCount().values.max()
    val rowOf = { item: Int -> first.row + (item - first.index).floorDiv(columns) }
    if (spin) {
        val start = librarySpinStartRow(first.row, rowOf(index), rowOf(layoutInfo.totalItemsCount - 1))
        if (start != first.row) scrollToItem((first.index + (start - first.row) * columns).coerceAtLeast(0))
    }
    val now = layoutInfo.visibleItemsInfo.firstOrNull() ?: return scrollToItem(index)
    val pitch = rowPitch()
    val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val rows = rowOf(index) - now.row
    val distance = rows * pitch - firstVisibleItemScrollOffset - (viewport - pitch) / 2f
    animateScrollBy(distance, tween(token.durationMillis, easing = token.easing))
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) scrollToItem(index)
}

private fun LazyGridState.rowPitch(): Float {
    val rows = layoutInfo.visibleItemsInfo.groupBy { it.row }.toSortedMap().values.map { it.first() }
    val second = rows.getOrNull(2) ?: rows.getOrNull(1)
    val before = second?.let { rows[rows.indexOf(it) - 1] }
    return if (second != null && before != null) {
        (second.offset.y - before.offset.y).toFloat()
    } else {
        rows.first().size.height.toFloat()
    }
}
