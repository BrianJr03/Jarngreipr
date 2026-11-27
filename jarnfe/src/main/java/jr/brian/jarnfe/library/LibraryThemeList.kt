package jr.brian.jarnfe.library

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.themekit.FocusedItem
import jr.brian.jarnfe.themekit.asString
import jr.brian.jarnfe.themekit.resolveThemeText
import jr.brian.jarnfe.themekit.textAlign

/** A row never gets shorter than its lines need, so a theme asking for many rows on a short box shows fewer. */
internal val LibraryListOneLineMin = 32.dp
internal val LibraryListTwoLineMin = 44.dp

/**
 * How a theme's list lays out: [rows] rows fill the box, their text sits at [align], and
 * [secondary] adds a second line read from each row's own item.
 */
internal data class LibraryListGeometry(
    val rows: Int,
    val align: ThemeAlign = ThemeAlign.Start,
    val secondary: ThemeText? = null,
)

internal fun libraryListGeometry(primary: ThemePrimary.List) =
    LibraryListGeometry(rows = primary.rows.coerceAtLeast(1), align = primary.align, secondary = primary.secondary)

/** [boxHeight] split into the theme's rows, after the list's own top and bottom padding. */
internal fun libraryListRowHeight(boxHeight: Dp, padding: Dp, spacing: Dp, geometry: LibraryListGeometry): Dp {
    val min = if (geometry.secondary != null) LibraryListTwoLineMin else LibraryListOneLineMin
    val usable = boxHeight - padding * 2 - spacing * (geometry.rows - 1)
    return maxOf(usable / geometry.rows, min)
}

/**
 * What a theme list shows for one item. [itemFocus] is the item as a theme binding sees it, so the
 * secondary line reads the same fields the decor layers do.
 */
internal class LibraryListContent<T>(
    val itemKey: (T) -> String,
    val itemTitle: (T) -> String,
    val itemFocus: (T) -> FocusedItem,
    val onOpen: (T) -> Unit,
    val itemModifier: (T) -> Modifier = { Modifier },
)

/**
 * A theme's text list: one item per row, on [LibraryFocusGrid] with a single column, so D-pad
 * movement, focus restore, jumps and the letter jump work exactly as they do in the grid.
 */
@Composable
internal fun <T : Any> LibraryThemeList(
    items: List<T>,
    content: LibraryListContent<T>,
    geometry: LibraryListGeometry,
    focusedKey: String?,
    onFocused: (String) -> Unit,
    modifier: Modifier = Modifier,
    refocusSignal: Int = 0,
    header: (@Composable () -> Unit)? = null,
    jump: LibraryJump? = null,
    onLanded: (String) -> Unit = {},
    letterJumpEnabled: Boolean = true,
    titleKey: (String) -> String = { it },
) {
    val spacing = JarnTheme.space.xs
    BoxWithConstraints(modifier) {
        val rowHeight = libraryListRowHeight(maxHeight, JarnTheme.space.focusEdge, spacing, geometry)
        LibraryFocusGrid(
            items = items,
            itemKey = content.itemKey,
            focusedKey = focusedKey,
            onFocused = onFocused,
            geometry = LibraryGridGeometry(cellMinSize = maxWidth, rowSpacing = spacing, columns = 1),
            refocusSignal = refocusSignal,
            header = header,
            jump = jump,
            onLanded = onLanded,
            itemTitle = content.itemTitle,
            letterJumpEnabled = letterJumpEnabled,
            titleKey = titleKey,
        ) { item, itemModifier ->
            LibraryThemeListRow(
                title = content.itemTitle(item),
                secondary = geometry.secondary?.let { resolveThemeText(it, content.itemFocus(item)) }?.asString(),
                align = geometry.align,
                onClick = { content.onOpen(item) },
                modifier = itemModifier.then(content.itemModifier(item)).height(rowHeight),
            )
        }
    }
}

/** One list row: the item's name and an optional second line, aligned as the theme asks. */
@Composable
internal fun LibraryThemeListRow(
    title: String,
    secondary: String?,
    align: ThemeAlign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val colors = JarnTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .jarnInteractive(JarnTheme.radius.smShape, source, focusStyle = JarnFocusStyle.Ring, onClick = onClick)
            .padding(horizontal = JarnTheme.space.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = align.horizontal,
    ) {
        JarnText(title, JarnTheme.type.labelStrong, colors.textHigh, maxLines = 1, textAlign = align.textAlign)
        secondary?.let { JarnText(it, JarnTheme.type.caption, colors.textMid, maxLines = 1, textAlign = align.textAlign) }
    }
}

private val ThemeAlign.horizontal: Alignment.Horizontal
    get() = when (this) {
        ThemeAlign.Start -> Alignment.Start
        ThemeAlign.Center -> Alignment.CenterHorizontally
        ThemeAlign.End -> Alignment.End
    }
