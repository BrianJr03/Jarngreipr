package jr.brian.jarnfe.canvas.tour

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnChip
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

private val HeldBorder = 2.dp

@Composable
internal fun CanvasTourBoard(run: CanvasTourState, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        CanvasTourPageChips(run.game.page)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(JarnTheme.radius.smShape)
                .background(JarnTheme.colors.raised),
        ) {
            CanvasTourTiles(run)
            CanvasTourMenu(run.mode, Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun CanvasTourPageChips(page: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        repeat(CANVAS_TOUR_PAGES) { index ->
            JarnChip(
                stringResource(R.string.jarnfe_canvas_page_of, index + 1, CANVAS_TOUR_PAGES),
                selected = index == page,
            )
        }
    }
}

@Composable
private fun CanvasTourTiles(run: CanvasTourState) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cellWidth = maxWidth / CANVAS_TOUR_COLUMNS
        val cellHeight = maxHeight / CANVAS_TOUR_ROWS
        CanvasTourTile.entries
            .filter { run.placeOf(it).page == run.game.page }
            .forEach { tile -> CanvasTourTileView(tile, run, cellWidth, cellHeight) }
    }
}

@Composable
private fun CanvasTourTileView(tile: CanvasTourTile, run: CanvasTourState, cellWidth: Dp, cellHeight: Dp) {
    val rect = run.placeOf(tile).rect
    val motion = JarnTheme.motion.small
    val x by animateDpAsState(cellWidth * rect.col, motion.spec(), label = "tourTileX")
    val y by animateDpAsState(cellHeight * rect.row, motion.spec(), label = "tourTileY")
    val width by animateDpAsState(cellWidth * rect.colSpan, motion.spec(), label = "tourTileWidth")
    val height by animateDpAsState(cellHeight * rect.rowSpan, motion.spec(), label = "tourTileHeight")
    val colors = JarnTheme.colors
    val isGame = tile == CanvasTourTile.Game
    val edge = if (isGame && run.mode == CanvasTourMode.Holding) colors.accent else colors.textMid
    Box(
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .padding(JarnTheme.space.xs)
            .clip(JarnTheme.radius.smShape)
            .background(colors.bg)
            .then(if (isGame) Modifier.border(HeldBorder, edge, JarnTheme.radius.smShape) else Modifier)
            .padding(JarnTheme.space.xs),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(stringResource(tile.labelRes), JarnTheme.type.caption, colors.textHigh, maxLines = 2)
    }
}

@get:StringRes
private val CanvasTourTile.labelRes: Int
    get() = when (this) {
        CanvasTourTile.Game -> R.string.jarnfe_canvas_tour_tile_game
        CanvasTourTile.Music -> R.string.jarnfe_canvas_stage_music
        CanvasTourTile.Photo -> R.string.jarnfe_canvas_tile_photo
    }

@Composable
private fun CanvasTourMenu(mode: CanvasTourMode, modifier: Modifier) {
    when (mode) {
        CanvasTourMode.TileMenu -> CanvasTourMenuCard(
            R.string.jarnfe_canvas_tile_menu_title,
            R.string.jarnfe_canvas_action_move,
            modifier,
        )
        CanvasTourMode.Options -> CanvasTourMenuCard(
            R.string.jarnfe_canvas_options_title,
            R.string.jarnfe_canvas_option_edit,
            modifier,
        )
        else -> Unit
    }
}

@Composable
private fun CanvasTourMenuCard(@StringRes titleRes: Int, @StringRes rowRes: Int, modifier: Modifier) {
    val colors = JarnTheme.colors
    JarnSurface(modifier.padding(JarnTheme.space.sm)) {
        Column(
            modifier = Modifier.padding(JarnTheme.space.md),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            JarnText(stringResource(titleRes), JarnTheme.type.caption, colors.textMid, maxLines = 1)
            JarnText(stringResource(rowRes), JarnTheme.type.labelStrong, colors.accent, maxLines = 1)
        }
    }
}
