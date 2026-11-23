package jr.brian.jarnfe.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.components.JarnKeyCap
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnGameTile
import jr.brian.core.ui.library.JarnHeroPanel
import jr.brian.core.ui.library.JarnSystemTile
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.session.LibrarySection

private val SystemCellMinSize = 148.dp
private val GameCellMinSize = 112.dp
private val HeroHeight = 160.dp
private const val SYSTEM_SKELETON_RATIO = 16f / 10f
private const val GAME_SKELETON_RATIO = 3f / 4f

@Composable
internal fun LibraryHome(
    section: LibrarySection,
    library: JarnLibraryStore,
    onOpenSystem: (String) -> Unit,
    emptyActions: @Composable () -> Unit,
) {
    key(section) {
        when (section) {
            LibrarySection.Systems -> LibrarySystems(library, onOpenSystem, emptyActions)
            LibrarySection.Favorites -> LibraryGames(
                feed = library.state.favorites,
                focusedKey = library.state.focusedFavorite?.id,
                onFocused = { id -> library.update { focusFavorite(id) } },
                library = library,
                empty = {
                    LibraryEmptyMessage(
                        LibraryEmptyCopy(R.string.jarnfe_empty_favorites_title, R.string.jarnfe_library_no_favorites_message),
                        emptyActions,
                    )
                },
            )
            LibrarySection.Recent -> LibraryGames(
                feed = library.state.recent,
                focusedKey = library.state.focusedRecent?.id,
                onFocused = { id -> library.update { focusRecent(id) } },
                library = library,
                empty = {
                    LibraryEmptyMessage(
                        LibraryEmptyCopy(R.string.jarnfe_empty_recent_title, R.string.jarnfe_library_no_recent_message),
                        emptyActions,
                    )
                },
            )
        }
    }
}

@Composable
private fun LibrarySystems(
    library: JarnLibraryStore,
    onOpenSystem: (String) -> Unit,
    emptyActions: @Composable () -> Unit,
) {
    val state = library.state
    LibraryFeedContent(
        feed = state.systems,
        cellMinSize = SystemCellMinSize,
        tileRatio = SYSTEM_SKELETON_RATIO,
        onRetry = library::retry,
        empty = { LibraryEmptyMessage(systemsEmptyCopy(state.systems.hiddenCount), emptyActions) },
    ) {
        LibraryFocusGrid(
            items = state.systems.items,
            itemKey = JarnLibrarySystem::id,
            focusedKey = state.focusedSystem?.id,
            onFocused = { id -> library.update { focusSystem(id) } },
            cellMinSize = SystemCellMinSize,
            header = state.hero?.let { hero -> { LibraryHeroBanner(hero) } },
        ) { system, modifier ->
            JarnSystemTile(
                title = system.title,
                subtitle = gameCountText(system.gameCount),
                onClick = { onOpenSystem(system.id) },
                modifier = modifier.fillMaxWidth(),
            ) { LibrarySystemArt(system) }
        }
    }
}

private fun systemsEmptyCopy(hiddenCount: Int) = if (hiddenCount > 0) {
    LibraryEmptyCopy(R.string.jarnfe_library_hidden_systems_title, R.string.jarnfe_library_hidden_systems_message)
} else {
    LibraryEmptyCopy(R.string.jarnfe_library_no_systems_title, R.string.jarnfe_library_no_systems_message)
}

private fun gamesEmptyCopy(hiddenCount: Int) = if (hiddenCount > 0) {
    LibraryEmptyCopy(R.string.jarnfe_library_hidden_games_title, R.string.jarnfe_library_hidden_games_message)
} else {
    LibraryEmptyCopy(R.string.jarnfe_library_no_games_title, R.string.jarnfe_library_no_games_message)
}

@Composable
private fun LibraryHeroBanner(hero: LibraryHero) {
    when (hero) {
        is LibraryHero.ContinuePlaying -> JarnHeroPanel(
            eyebrow = stringResource(R.string.jarnfe_library_continue_playing),
            title = heroTitle(hero.game.title),
            body = playSummary(hero.game.metadata),
            modifier = Modifier.fillMaxWidth().height(HeroHeight),
            art = { LibraryArt(hero.game.artPath, hero.game.title) },
        )
        is LibraryHero.FocusedSystem -> JarnHeroPanel(
            eyebrow = null,
            title = hero.system.title,
            body = gameCountText(hero.system.gameCount),
            modifier = Modifier.fillMaxWidth().height(HeroHeight),
            art = { LibrarySystemArt(hero.system) },
        )
    }
}

/** ROM names carry tags like "(USA) (Rev 1)" that don't belong in a headline. */
internal fun heroTitle(title: String): String =
    title.substringBefore(" (").substringBefore(" [").trim().ifEmpty { title }

@Composable
private fun LibraryGames(
    feed: JarnLibraryFeed<JarnLibraryGame>,
    focusedKey: String?,
    onFocused: (String) -> Unit,
    library: JarnLibraryStore,
    empty: @Composable () -> Unit,
) {
    LibraryFeedContent(
        feed = feed,
        cellMinSize = GameCellMinSize,
        tileRatio = GAME_SKELETON_RATIO,
        onRetry = library::retry,
        empty = empty,
    ) {
        LibraryFocusGrid(
            items = feed.items,
            itemKey = JarnLibraryGame::id,
            focusedKey = focusedKey,
            onFocused = onFocused,
            cellMinSize = GameCellMinSize,
            refocusSignal = library.stageRefocus,
        ) { game, modifier ->
            JarnGameTile(
                title = game.title,
                onClick = { library.play(game) },
                modifier = modifier.fillMaxWidth(),
            ) { LibraryArt(game.artPath, game.title) }
        }
    }
}

@Composable
internal fun LibraryGameList(library: JarnLibraryStore) {
    val state = library.state
    key(state.openSystemId) {
        LibraryGames(
            feed = state.openGames,
            focusedKey = state.focusedOpenGame?.id,
            onFocused = { id -> library.update { focusOpenGame(id) } },
            library = library,
            empty = { LibraryEmptyMessage(gamesEmptyCopy(state.openGames.hiddenCount)) },
        )
    }
}

@Composable
internal fun LibraryGameListTitle(state: JarnLibraryState) {
    val colors = JarnTheme.colors
    val games = state.openGames.items
    val position = games.indexOfFirst { it.id == state.focusedOpenGame?.id }
    Row(
        modifier = Modifier.padding(vertical = JarnTheme.space.lg),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnKeyCap(JarnKey.L1)
        JarnText(state.openSystem?.title.orEmpty(), JarnTheme.type.labelStrong, colors.textHigh, maxLines = 1)
        JarnKeyCap(JarnKey.R1)
        if (position >= 0) {
            JarnText(
                text = stringResource(R.string.jarnfe_library_game_position, position + 1, games.size),
                style = JarnTheme.type.caption,
                color = colors.textMid,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun gameCountText(count: Int): String =
    pluralStringResource(R.plurals.jarnfe_library_game_count, count, count)

/** Keeps the session and the library in step when a restored game list no longer has a system. */
@Composable
internal fun LibraryOrphanGuard(orphaned: Boolean, onOrphaned: () -> Unit) {
    LaunchedEffect(orphaned) { if (orphaned) onOrphaned() }
}
