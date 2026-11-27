package jr.brian.jarnfe.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.JarnThemeSketchKind
import jr.brian.jarnfe.themes.toReadableJarnColors

private const val GRID_COLUMNS = 4
private const val GRID_ROWS = 2
private const val COVER_GRID_COLUMNS = 3
private const val LIST_ROWS = 4
private const val RAIL_LINES = 3
private const val SHELF_NEIGHBOR_WEIGHT = 0.6f
private const val SHELF_NEIGHBOR_HEIGHT = 0.7f
private const val BIG_ART_WEIGHT = 2.2f
private const val SIDE_PANEL_WEIGHT = 0.8f
private const val LIST_WEIGHT = 1.2f
private const val RAIL_LINE_WIDTH = 0.7f

/**
 * A small picture of [entry]'s own layout, ignoring any per-view override. It uses the colors the
 * layout would show, so one that follows the Look recolors with it.
 */
@Composable
internal fun JarnThemeSketch(entry: JarnThemeEntry, modifier: Modifier = Modifier) {
    val colors = entry.theme.spec.tokens.colors.takeIf { entry.ownColors }?.toReadableJarnColors() ?: JarnTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(JarnTheme.radius.smShape)
            .background(colors.bg)
            .padding(JarnTheme.space.sm),
    ) {
        when (entry.sketchKind) {
            JarnThemeSketchKind.Grid -> GridSketch(colors, GRID_COLUMNS, Modifier.fillMaxSize())
            JarnThemeSketchKind.Carousel -> ShelfSketch(colors, Modifier.fillMaxSize())
            JarnThemeSketchKind.List -> ListSketch(colors, Modifier.fillMaxSize())
            JarnThemeSketchKind.BigShelf -> BigShelfSketch(colors)
            JarnThemeSketchKind.CoverGrid -> CoverGridSketch(colors)
            JarnThemeSketchKind.ListWithArt -> ListWithArtSketch(colors)
        }
    }
}

@Composable
private fun BigShelfSketch(colors: JarnColors) {
    SketchColumn(Modifier.fillMaxSize()) {
        SketchPanel(colors, Modifier.fillMaxWidth().weight(BIG_ART_WEIGHT))
        SketchRow(Modifier.fillMaxWidth().weight(1f)) {
            repeat(GRID_COLUMNS + 1) { index ->
                SketchItem(colors, focused = index == 1, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CoverGridSketch(colors: JarnColors) {
    SketchRow(Modifier.fillMaxSize()) {
        GridSketch(colors, COVER_GRID_COLUMNS, Modifier.weight(1f).fillMaxHeight())
        RailSketch(colors, Modifier.weight(SIDE_PANEL_WEIGHT).fillMaxHeight())
    }
}

@Composable
private fun ListWithArtSketch(colors: JarnColors) {
    SketchRow(Modifier.fillMaxSize()) {
        ListSketch(colors, Modifier.weight(LIST_WEIGHT).fillMaxHeight())
        SketchPanel(colors, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun GridSketch(colors: JarnColors, columns: Int, modifier: Modifier) {
    SketchColumn(modifier) {
        repeat(GRID_ROWS) { row ->
            SketchRow(Modifier.fillMaxWidth().weight(1f)) {
                repeat(columns) { column ->
                    SketchItem(colors, focused = row == 0 && column == 0, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun ShelfSketch(colors: JarnColors, modifier: Modifier) {
    SketchRow(modifier) {
        SketchItem(colors, focused = false, Modifier.weight(SHELF_NEIGHBOR_WEIGHT).fillMaxHeight(SHELF_NEIGHBOR_HEIGHT))
        SketchItem(colors, focused = true, Modifier.weight(1f).fillMaxHeight())
        SketchItem(colors, focused = false, Modifier.weight(SHELF_NEIGHBOR_WEIGHT).fillMaxHeight(SHELF_NEIGHBOR_HEIGHT))
    }
}

@Composable
private fun ListSketch(colors: JarnColors, modifier: Modifier) {
    SketchColumn(modifier) {
        repeat(LIST_ROWS) { index ->
            SketchItem(colors, focused = index == 0, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** A details rail: a panel with a few text lines. */
@Composable
private fun RailSketch(colors: JarnColors, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(colors.surface, JarnTheme.radius.smShape)
            .padding(JarnTheme.space.xs),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        repeat(RAIL_LINES) { index ->
            val width = if (index == 0) 1f else RAIL_LINE_WIDTH
            Box(Modifier.fillMaxWidth(width).weight(1f).background(colors.textLow, JarnTheme.radius.smShape))
        }
    }
}

@Composable
private fun SketchPanel(colors: JarnColors, modifier: Modifier) {
    Box(modifier.background(colors.raised, JarnTheme.radius.smShape))
}

@Composable
private fun SketchRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SketchColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs), content = content)
}

@Composable
private fun SketchItem(colors: JarnColors, focused: Boolean, modifier: Modifier) {
    Box(modifier.background(if (focused) colors.accent else colors.surface, JarnTheme.radius.smShape))
}
