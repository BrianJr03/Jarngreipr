package jr.brian.jarnfe.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.themes.JarnActiveTheme
import jr.brian.jarnfe.themes.toReadableJarnColors

private const val GRID_SKETCH_CELLS = 4
private const val LIST_SKETCH_ROWS = 3
private const val SHELF_NEIGHBOR_WEIGHT = 0.6f
private const val SHELF_NEIGHBOR_HEIGHT = 0.7f

/**
 * A small drawing of [theme]: systems on top, games below, each as its list shape. It uses the
 * colors the theme would show, so a theme that follows the Look recolors with it.
 */
@Composable
internal fun JarnThemeSketch(theme: JarnActiveTheme, modifier: Modifier = Modifier) {
    val colors = theme.ownColors?.toReadableJarnColors() ?: JarnTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(JarnTheme.radius.smShape)
            .background(colors.bg)
            .padding(JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        ThemeView.entries.forEach { view ->
            PrimarySketch(theme.forView(view).viewSpec(view)?.primary, colors, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PrimarySketch(primary: ThemePrimary?, colors: JarnColors, modifier: Modifier) {
    when (primary) {
        is ThemePrimary.Shelf -> ShelfSketch(colors, modifier)
        is ThemePrimary.List -> ListSketch(colors, modifier)
        is ThemePrimary.Grid, null -> GridSketch(colors, modifier)
    }
}

@Composable
private fun GridSketch(colors: JarnColors, modifier: Modifier) {
    SketchRow(modifier) {
        repeat(GRID_SKETCH_CELLS) { index ->
            SketchItem(colors, focused = index == 0, Modifier.weight(1f).fillMaxHeight())
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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        repeat(LIST_SKETCH_ROWS) { index ->
            SketchItem(colors, focused = index == 0, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun SketchRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SketchItem(colors: JarnColors, focused: Boolean, modifier: Modifier) {
    val shape = JarnTheme.radius.smShape
    val outline = if (focused) Modifier.border(2.dp, colors.accent, shape) else Modifier
    Box(modifier.background(if (focused) colors.raised else colors.surface, shape).then(outline))
}
