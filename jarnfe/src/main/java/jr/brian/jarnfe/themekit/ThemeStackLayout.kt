package jr.brian.jarnfe.themekit

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import jr.brian.core.model.theme.stack
import kotlin.math.max
import kotlin.math.min

/** True inside a stack, where a layer takes only the height its content needs. */
internal val LocalThemeStacked = compositionLocalOf { false }

/** What one slot of a placed view draws: a layer on its own, or a stack of layers as one column. */
internal sealed interface ThemeSlotItem {
    val key: String
    val bounds: ThemePxRect

    data class Single(val placed: PlacedThemeLayer) : ThemeSlotItem {
        override val key: String get() = placed.layer.id
        override val bounds: ThemePxRect get() = placed.bounds
    }

    data class Stack(val name: String, val members: List<PlacedThemeLayer>) : ThemeSlotItem {
        override val key: String get() = STACK_KEY_PREFIX + name
        override val bounds: ThemePxRect get() = members.map { it.bounds }.reduce(ThemePxRect::union)
    }
}

/** Layers in draw order, each stack gathered where its first member draws. */
internal fun List<PlacedThemeLayer>.slotItems(): List<ThemeSlotItem> {
    val stacks = filter { it.layer.stack != null }.groupBy { it.layer.stack!! }
    val seen = HashSet<String>()
    return mapNotNull { placed ->
        val name = placed.layer.stack ?: return@mapNotNull ThemeSlotItem.Single(placed)
        if (seen.add(name)) ThemeSlotItem.Stack(name, stacks.getValue(name)) else null
    }
}

/**
 * Where each row of a stack lands, as tops relative to the stack. Rows are members sharing a top,
 * in top order; a row whose members measured no height is skipped, and the rest keep the gap
 * they had above them in the theme.
 */
internal fun stackRowTops(rows: List<StackRow>): List<Int?> {
    var cursor = 0
    var placedAny = false
    var previousBottom: Int? = null
    return rows.map { row ->
        val gap = previousBottom?.let { max(0, row.top - it) } ?: 0
        previousBottom = row.top + row.extent
        if (row.measured == 0) return@map null
        if (placedAny) cursor += gap
        val top = cursor
        cursor += row.measured
        placedAny = true
        top
    }
}

/** A stack row: its [top] and [extent] in the theme, and the height its members [measured]. */
internal data class StackRow(val top: Int, val extent: Int, val measured: Int)

/** Draws [stack]'s members as a column in its bounds, so empty or short members leave no holes. */
@Composable
internal fun ThemeStack(stack: ThemeSlotItem.Stack, layer: @Composable (PlacedThemeLayer) -> Unit) {
    val origin = stack.bounds
    CompositionLocalProvider(LocalThemeStacked provides true) {
        Layout(
            content = { stack.members.forEach { key(it.layer.id) { Box { layer(it) } } } },
        ) { measurables, constraints ->
            val placeables = measurables.zip(stack.members) { measurable, member ->
                val b = member.bounds
                measurable.measure(Constraints(minWidth = b.width, maxWidth = b.width, maxHeight = b.height)) to member
            }
            val rows = placeables.groupBy { it.second.bounds.top }.toSortedMap()
            val tops = stackRowTops(
                rows.map { (top, members) ->
                    StackRow(top, members.maxOf { it.second.bounds.height }, members.maxOf { it.first.height })
                },
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                rows.values.zip(tops).forEach { (members, top) ->
                    if (top == null) return@forEach
                    members.forEach { (placeable, member) -> placeable.place(member.bounds.left - origin.left, min(top, constraints.maxHeight)) }
                }
            }
        }
    }
}

private const val STACK_KEY_PREFIX = "jarn-theme-stack:"

internal fun ThemePxRect.union(other: ThemePxRect): ThemePxRect {
    val l = min(left, other.left)
    val t = min(top, other.top)
    return ThemePxRect(l, t, max(right, other.right) - l, max(bottom, other.bottom) - t)
}
