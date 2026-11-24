package jr.brian.jarnfe.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnGameTile
import jr.brian.core.ui.library.JarnSystemTile
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppIcon
import jr.brian.jarnfe.apps.JarnAppNameLabel
import jr.brian.jarnfe.apps.jarnAppIconSize
import jr.brian.jarnfe.apps.jarnAppSections
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.SettingsSection
import jr.brian.jarnfe.library.GameCellMinSize
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibraryBrowseSwitch
import jr.brian.jarnfe.library.LibraryCoverArt
import jr.brian.jarnfe.library.LibraryShelfKind
import jr.brian.jarnfe.library.LibraryShelfPreview
import jr.brian.jarnfe.library.LibrarySystemArt
import jr.brian.jarnfe.library.SystemCellMinSize
import jr.brian.jarnfe.library.gameCountText
import jr.brian.jarnfe.look.rememberJarnDisplay

private const val PREVIEW_TILE_COUNT = 8
private val PreviewAppTileWidth = 88.dp
private val PreviewAppIconSize = 48.dp

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
        if (JarnSession.state.settingsSection == SettingsSection.Apps) {
            PreviewAppRow()
        } else {
            LibraryBrowseSwitch(
                view = display.systemView,
                grid = { PreviewSystemRow(state.systems.items, SystemCellMinSize * display.systemTileScale) },
                shelf = { PreviewSystemShelf(state.systems.items) },
            )
            LibraryBrowseSwitch(
                view = display.gameView,
                grid = { PreviewGameRow(games, GameCellMinSize * display.gameTileScale) },
                shelf = { PreviewGameShelf(games) },
            )
        }
    }
}

@Composable
private fun PreviewAppRow() {
    val context = LocalContext.current
    val data = remember(context) { context.jarnAppsDataOrNull() } ?: return
    val snapshot by remember(data) { data.snapshot }.collectAsState(initial = null)
    val labelSize = rememberJarnLauncherPrefs().appLabelSize
    val namesVisible = rememberJarnDisplay().appNamesVisible
    val apps = remember(snapshot) {
        snapshot?.let { jarnAppSections(it) }?.let { it.pinned + it.all }.orEmpty().take(PREVIEW_TILE_COUNT)
    }
    JarnText(
        text = stringResource(R.string.jarnfe_settings_preview_apps),
        style = JarnTheme.type.caption,
        color = JarnTheme.colors.textMid,
        maxLines = 1,
    )
    PreviewRow {
        apps.forEach { app -> PreviewAppTile(app, labelSize, namesVisible) }
    }
}

@Composable
private fun PreviewAppTile(app: JarnDockApp, labelSize: Int, nameVisible: Boolean) {
    Column(
        modifier = Modifier.width(PreviewAppTileWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        val iconSize = jarnAppIconSize(PreviewAppIconSize, nameVisible)
        JarnAppIcon(app.icon, app.iconPath, app.iconVersion, Modifier.size(iconSize))
        if (nameVisible) JarnAppNameLabel(app.label, labelSize)
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
private fun PreviewSystemShelf(systems: List<JarnLibrarySystem>) {
    val placeholder = stringResource(R.string.jarnfe_settings_preview_system)
    LibraryShelfPreview(
        items = systems,
        kind = LibraryShelfKind.Systems,
        description = { it?.title ?: placeholder },
    ) { system -> system?.let { LibrarySystemArt(it) } }
}

@Composable
private fun PreviewGameShelf(games: List<JarnLibraryGame>) {
    val placeholder = stringResource(R.string.jarnfe_settings_preview_game)
    LibraryShelfPreview(
        items = games,
        kind = LibraryShelfKind.Games,
        description = { it?.title ?: placeholder },
    ) { game -> game?.let { LibraryCoverArt(it) } }
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
