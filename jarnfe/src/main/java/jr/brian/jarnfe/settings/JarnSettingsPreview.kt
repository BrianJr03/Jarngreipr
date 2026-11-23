package jr.brian.jarnfe.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnGameTile
import jr.brian.core.ui.library.JarnSystemTile
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.GameCellMinSize
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibraryCoverArt
import jr.brian.jarnfe.library.LibrarySystemArt
import jr.brian.jarnfe.library.SystemCellMinSize
import jr.brian.jarnfe.library.gameCountText
import jr.brian.jarnfe.look.rememberJarnDisplay

private const val PREVIEW_TILE_COUNT = 8

/** Real tiles at the saved sizes. They can't take focus, so the preview never traps the controller. */
@Composable
internal fun JarnSettingsPreview(modifier: Modifier = Modifier.fillMaxSize()) {
    val display = rememberJarnDisplay()
    val state = JarnLibrary.state
    val games = (state.recent.items + state.favorites.items).distinctBy { it.id }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState(), enabled = false)
            .padding(horizontal = JarnTheme.space.stageMargin, vertical = JarnTheme.space.focusEdge),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        PreviewHeading()
        PreviewSystemRow(state.systems.items, SystemCellMinSize * display.systemTileScale)
        PreviewGameRow(games, GameCellMinSize * display.gameTileScale)
    }
}

@Composable
private fun PreviewHeading() {
    Column {
        JarnText(
            text = stringResource(R.string.jarnfe_settings_preview_title),
            style = JarnTheme.type.labelStrong,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
        )
        JarnText(
            text = stringResource(R.string.jarnfe_settings_preview_hint),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
    }
}

@Composable
private fun PreviewRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState(), enabled = false),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) { content() }
}

@Composable
private fun PreviewSystemRow(systems: List<JarnLibrarySystem>, tileWidth: Dp) {
    val tileModifier = Modifier.width(tileWidth).focusProperties { canFocus = false }
    PreviewRow {
        if (systems.isEmpty()) {
            repeat(PREVIEW_TILE_COUNT) {
                JarnSystemTile(
                    title = stringResource(R.string.jarnfe_settings_preview_system),
                    subtitle = null,
                    onClick = {},
                    modifier = tileModifier,
                    art = {},
                )
            }
        } else {
            systems.take(PREVIEW_TILE_COUNT).forEach { system ->
                JarnSystemTile(
                    title = system.title,
                    subtitle = gameCountText(system.gameCount),
                    onClick = {},
                    modifier = tileModifier,
                ) { LibrarySystemArt(system) }
            }
        }
    }
}

@Composable
private fun PreviewGameRow(games: List<JarnLibraryGame>, tileWidth: Dp) {
    val tileModifier = Modifier.width(tileWidth).focusProperties { canFocus = false }
    PreviewRow {
        if (games.isEmpty()) {
            repeat(PREVIEW_TILE_COUNT) {
                JarnGameTile(
                    title = stringResource(R.string.jarnfe_settings_preview_game),
                    onClick = {},
                    modifier = tileModifier,
                    art = {},
                )
            }
        } else {
            games.take(PREVIEW_TILE_COUNT).forEach { game ->
                JarnGameTile(
                    title = game.title,
                    onClick = {},
                    modifier = tileModifier,
                ) { LibraryCoverArt(game) }
            }
        }
    }
}
