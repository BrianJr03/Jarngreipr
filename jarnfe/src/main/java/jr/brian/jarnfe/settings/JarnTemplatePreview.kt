package jr.brian.jarnfe.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibraryCoverArt
import jr.brian.jarnfe.library.LibraryGameTile
import jr.brian.jarnfe.library.LibraryShelfKind
import jr.brian.jarnfe.library.LibraryShelfPreview
import jr.brian.jarnfe.library.LibrarySystemArt
import jr.brian.jarnfe.library.LibrarySystemCard
import jr.brian.jarnfe.library.libraryShelfGeometry
import jr.brian.jarnfe.themekit.FocusedItem
import jr.brian.jarnfe.themekit.JarnThemeDecorView
import jr.brian.jarnfe.themekit.ThemeInsets
import jr.brian.jarnfe.themes.JarnThemeEntry

/** Roughly the top bar over the browse stage, so the frame keeps the stage's proportions. */
private val StageTopBar = 80.dp
private const val PREVIEW_GRID_ROWS = 3

/** What a template preview draws: the first system, or the first games with their systems. */
internal class JarnTemplatePreviewItems(val systems: List<JarnLibrarySystem>, val games: List<JarnLibraryGame>) {
    fun focused(view: ThemeView): FocusedItem = when (view) {
        ThemeView.Systems -> systems.firstOrNull()?.let(FocusedItem::System) ?: FocusedItem.None
        ThemeView.Games -> games.firstOrNull()
            ?.let { game -> FocusedItem.Game(game, systems.firstOrNull { it.id == game.systemId }) }
            ?: FocusedItem.None
    }
}

/**
 * [entry]'s [view] drawn as the browse stage would, scaled down into a frame at most
 * [maxFrameHeight] tall. Its decor layers follow the first item, and nothing in it takes focus.
 */
@Composable
internal fun JarnTemplatePreview(
    entry: JarnThemeEntry,
    view: ThemeView,
    items: JarnTemplatePreviewItems,
    maxFrameHeight: Dp,
) {
    val spec = entry.viewSpec(view) ?: return
    val configuration = LocalConfiguration.current
    val stageWidth = configuration.screenWidthDp.dp
    val stageHeight = configuration.screenHeightDp.dp - StageTopBar - JarnTheme.space.hintBarHeight
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth / stageWidth, maxFrameHeight / stageHeight)
        Box(
            Modifier
                .size(stageWidth * scale, stageHeight * scale)
                .clip(JarnTheme.radius.smShape)
                .background(JarnTheme.colors.bg)
                .focusProperties { canFocus = false },
        ) {
            Box(
                Modifier
                    .requiredSize(stageWidth, stageHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                JarnThemeDecorView(
                    view = spec,
                    focused = items.focused(view),
                    primary = { PreviewPrimary(spec.primary, entry, view, items) },
                    themeDir = entry.themeDir,
                    insets = ThemeInsets(),
                )
            }
        }
    }
}

@Composable
private fun PreviewPrimary(primary: ThemePrimary, entry: JarnThemeEntry, view: ThemeView, items: JarnTemplatePreviewItems) {
    when (primary) {
        is ThemePrimary.Shelf -> PreviewTemplateShelf(primary, entry, view, items)
        is ThemePrimary.Grid -> PreviewTemplateGrid(primary, view, items)
        is ThemePrimary.List -> PreviewTemplateList(primary, view, items)
    }
}

@Composable
private fun PreviewTemplateShelf(primary: ThemePrimary.Shelf, entry: JarnThemeEntry, view: ThemeView, items: JarnTemplatePreviewItems) {
    val kind = if (view == ThemeView.Systems) LibraryShelfKind.Systems else LibraryShelfKind.Games
    val geometry = libraryShelfGeometry(primary, kind, entry.theme.pinsFor(view).shelf)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (view == ThemeView.Systems) {
            val placeholder = stringResource(R.string.jarnfe_settings_preview_system)
            LibraryShelfPreview(items.systems, kind, { it?.title ?: placeholder }, maxHeight, geometry) { system ->
                system?.let { LibrarySystemArt(it) }
            }
        } else {
            val placeholder = stringResource(R.string.jarnfe_settings_preview_game)
            LibraryShelfPreview(items.games, kind, { it?.title ?: placeholder }, maxHeight, geometry) { game ->
                game?.let { LibraryCoverArt(it) }
            }
        }
    }
}

@Composable
private fun PreviewTemplateGrid(primary: ThemePrimary.Grid, view: ThemeView, items: JarnTemplatePreviewItems) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gap = maxWidth * primary.spacing
        val columns = primary.columns.coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(PREVIEW_GRID_ROWS) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(columns) { column ->
                        val cell = Modifier.weight(1f).aspectRatio(primary.itemAspect)
                        PreviewGridCell(view, items, row * columns + column, cell)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewGridCell(view: ThemeView, items: JarnTemplatePreviewItems, index: Int, modifier: Modifier) {
    val noFocus = modifier.focusProperties { canFocus = false }
    when (view) {
        ThemeView.Systems -> items.systems.getOrNull(index)
            ?.let { LibrarySystemCard(it, onClick = {}, modifier = noFocus) }
            ?: Box(noFocus.background(JarnTheme.colors.surface, JarnTheme.tileShape))
        ThemeView.Games -> items.games.getOrNull(index)
            ?.let { LibraryGameTile(it, onClick = {}, modifier = noFocus) }
            ?: Box(noFocus.background(JarnTheme.colors.surface, JarnTheme.tileShape))
    }
}

@Composable
private fun PreviewTemplateList(primary: ThemePrimary.List, view: ThemeView, items: JarnTemplatePreviewItems) {
    val titles = when (view) {
        ThemeView.Systems -> items.systems.map { it.title }
        ThemeView.Games -> items.games.map { it.title }
    }.take(primary.rows)
    Column(Modifier.fillMaxSize()) {
        titles.forEachIndexed { index, title ->
            val focused = index == 0
            val rowModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (focused) Modifier.background(JarnTheme.colors.raised, JarnTheme.radius.smShape) else Modifier)
                .padding(horizontal = JarnTheme.space.md)
            Box(rowModifier, contentAlignment = Alignment.CenterStart) {
                JarnText(title, JarnTheme.type.label, if (focused) JarnTheme.colors.textHigh else JarnTheme.colors.textMid, maxLines = 1)
            }
        }
        repeat(primary.rows - titles.size) { Box(Modifier.weight(1f)) }
    }
}
