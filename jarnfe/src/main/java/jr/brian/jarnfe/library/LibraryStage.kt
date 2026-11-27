package jr.brian.jarnfe.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.components.JarnKeyCap
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnGameTile
import jr.brian.core.ui.library.JarnHeroPanel
import jr.brian.core.ui.library.JarnSystemTile
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.launch.JarnGameFileStatus
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.transition.JarnSystemSlide

internal val SystemCellMinSize = 148.dp
internal val GameCellMinSize = 112.dp

/** Taller than a focused tile's border can reach upward, so it never touches the row above. */
internal val LibraryGridRowSpacing = 24.dp
private val HeroHeight = 160.dp
private const val SYSTEM_SKELETON_RATIO = 16f / 10f
private const val GAME_SKELETON_RATIO = 3f / 4f

@Composable
internal fun LibraryHome(
    section: LibrarySection,
    library: JarnLibraryStore,
    onOpenSystem: (String) -> Unit,
    emptyActions: @Composable () -> Unit,
    onOpenCollection: (Long) -> Unit = {},
) {
    ResumeMissingDialog()
    LibraryTabReveal(section) {
        when (section) {
            LibrarySection.Systems -> LibrarySystems(library, onOpenSystem, emptyActions)
            LibrarySection.AllGames -> LibraryAllGames(library, emptyActions)
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
            LibrarySection.Collections -> LibraryCollections(library, onOpenCollection, emptyActions)
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
    val display = rememberJarnDisplay()
    val cellMinSize = SystemCellMinSize * display.systemTileScale
    val onFocused: (String) -> Unit = { id -> library.update { focusSystem(id) } }
    LibraryFeedContent(
        feed = state.systems,
        cellMinSize = cellMinSize,
        tileRatio = SYSTEM_SKELETON_RATIO,
        onRetry = library::retry,
        empty = { LibraryEmptyMessage(systemsEmptyCopy(state.systems.hiddenCount), emptyActions) },
    ) {
        LibraryBrowseSwitch(
            view = display.systemView,
            grid = { LibrarySystemGrid(library, cellMinSize, onFocused, onOpenSystem) },
            shelf = { LibrarySystemShelf(library, onFocused, onOpenSystem) },
        )
    }
}

/**
 * The Continue playing card stays above the shelf, as it does above the grid. Up from the shelf
 * reaches it and Down returns to the centered system. There is no card until something is played.
 */
@Composable
internal fun LibrarySystemShelf(
    library: JarnLibraryStore,
    onFocused: (String) -> Unit,
    onOpenSystem: (String) -> Unit,
) {
    val state = library.state
    Column(Modifier.fillMaxSize()) {
        (state.hero as? LibraryHero.ContinuePlaying)?.let { hero ->
            Box(
                Modifier.padding(
                    start = JarnTheme.space.stageMargin,
                    end = JarnTheme.space.stageMargin,
                    top = JarnTheme.space.focusEdge,
                ),
            ) {
                LibraryHeroBanner(
                    hero = hero,
                    onPlay = library::play.takeIf { library.canPlay },
                    onFocusChange = { library.heroFocused = it },
                )
            }
        }
        LibraryShelf(
            items = state.systems.items,
            kind = LibraryShelfKind.Systems,
            content = systemShelfContent(onOpenSystem, rememberJarnDisplay().systemNamesVisible),
            focusedKey = state.focusedSystem?.id,
            onFocused = onFocused,
            modifier = Modifier.weight(1f),
            letterJumpEnabled = false,
        )
    }
}

@Composable
private fun LibrarySystemGrid(
    library: JarnLibraryStore,
    cellMinSize: Dp,
    onFocused: (String) -> Unit,
    onOpenSystem: (String) -> Unit,
) {
    val state = library.state
    val namesVisible = rememberJarnDisplay().systemNamesVisible
    LibraryFocusGrid(
        items = state.systems.items,
        itemKey = JarnLibrarySystem::id,
        focusedKey = state.focusedSystem?.id,
        onFocused = onFocused,
        geometry = LibraryGridGeometry(cellMinSize, rowSpacing = LibraryGridRowSpacing),
        header = state.hero?.let { hero ->
            {
                LibraryHeroBanner(
                    hero = hero,
                    onPlay = library::play.takeIf { library.canPlay },
                    onFocusChange = { library.heroFocused = it },
                )
            }
        },
    ) { system, modifier ->
        val tileModifier = modifier.librarySystemAnchor(system.id).fillMaxWidth()
        if (namesVisible) {
            JarnSystemTile(
                title = system.title,
                subtitle = gameCountText(system.gameCount),
                onClick = { onOpenSystem(system.id) },
                modifier = tileModifier,
                focusBorder = true,
            ) { LibrarySystemArt(system) }
        } else {
            LibrarySystemCard(system, onClick = { onOpenSystem(system.id) }, modifier = tileModifier)
        }
    }
}

private fun systemShelfContent(onOpenSystem: (String) -> Unit, namesVisible: Boolean) = LibraryShelfContent<JarnLibrarySystem>(
    itemKey = JarnLibrarySystem::id,
    itemTitle = JarnLibrarySystem::title,
    onOpen = { onOpenSystem(it.id) },
    itemModifier = { Modifier.librarySystemAnchor(it.id) },
    showsCaption = namesVisible,
    headline = { stringResource(R.string.jarnfe_library_system_line, it.title, gameCountText(it.gameCount)) },
    art = { LibrarySystemArt(it, logoAlignment = Alignment.Center) },
)

private fun gameShelfContent(library: JarnLibraryStore) = LibraryShelfContent<JarnLibraryGame>(
    itemKey = JarnLibraryGame::id,
    itemTitle = JarnLibraryGame::title,
    onOpen = library::play,
    caption = { playSummary(it.metadata) },
    art = { LibraryCoverArt(it) },
    card = { game, slot ->
        LibraryGameTile(
            game = game,
            onClick = slot.onClick,
            modifier = slot.modifier.fillMaxWidth(),
            slotAspectRatio = slot.aspectRatio,
            artAlpha = slot.artAlpha,
        )
    },
)

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
private fun LibraryHeroBanner(
    hero: LibraryHero,
    onPlay: ((JarnLibraryGame) -> Unit)?,
    onFocusChange: (Boolean) -> Unit,
) {
    DisposableEffect(Unit) { onDispose { onFocusChange(false) } }
    when (hero) {
        is LibraryHero.ContinuePlaying -> ContinuePlayingBanner(hero.game, onPlay != null, onFocusChange)
        is LibraryHero.FocusedSystem -> JarnHeroPanel(
            eyebrow = null,
            title = hero.system.title,
            body = gameCountText(hero.system.gameCount),
            modifier = Modifier.fillMaxWidth().height(HeroHeight),
            titleStyle = JarnTheme.baseType.display,
            art = { LibrarySystemArt(hero.system) },
        )
    }
}

/** A missing file dims the art and says so, and A opens the notice instead of launching. */
@Composable
private fun ContinuePlayingBanner(game: JarnLibraryGame, canPlay: Boolean, onFocusChange: (Boolean) -> Unit) {
    val status = rememberGameFileStatus(game)
    val screen = LocalJarnScreen.current
    val missing = status == JarnGameFileStatus.Missing
    val focus = remember { FocusRequester() }
    var coverWidth by remember(game.artPath) { mutableStateOf(HeroHeight) }
    LaunchedEffect(JarnLibrary.heroFocusPending) {
        if (!JarnLibrary.heroFocusPending) return@LaunchedEffect
        withFrameNanos { }
        runCatching { focus.requestFocus() }
        JarnLibrary.consumeHeroFocus()
    }
    JarnHeroPanel(
        eyebrow = stringResource(R.string.jarnfe_library_continue_playing),
        title = heroTitle(game.title),
        body = if (missing) stringResource(R.string.jarnfe_resume_file_missing) else null,
        bodyStats = if (missing) null else playStats(game.metadata),
        bodySeparator = PLAY_SUMMARY_SEPARATOR,
        modifier = Modifier
            .focusRequester(focus)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .fillMaxWidth()
            .height(HeroHeight),
        onClick = { resumeGame(game, status, screen) }.takeIf { canPlay },
        artEndWidth = coverWidth,
        bodyStyle = resumeStatsStyle(),
        art = {
            Box(Modifier.matchParentSize().alpha(missingArtAlpha(status))) {
                LibraryCoverArt(game, Alignment.CenterEnd, onAspectRatio = { coverWidth = heroCoverWidth(it) })
            }
        },
    )
}

/** How much of the card's right edge a cover of [aspectRatio] takes at the card's height. */
private fun heroCoverWidth(aspectRatio: Float?): Dp = aspectRatio?.let { HeroHeight * it } ?: 0.dp

/** ROM names carry tags like "(USA) (Rev 1)" that don't belong in a headline. */
internal fun heroTitle(title: String): String =
    title.substringBefore(" (").substringBefore(" [").trim().ifEmpty { title }

@Composable
internal fun LibraryGames(
    feed: JarnLibraryFeed<JarnLibraryGame>,
    focusedKey: String?,
    onFocused: (String) -> Unit,
    library: JarnLibraryStore,
    empty: @Composable () -> Unit,
    jump: LibraryJump? = null,
) {
    val display = rememberJarnDisplay()
    val cellMinSize = GameCellMinSize * display.gameTileScale
    val letterJumpEnabled = feed.letterJumpEnabled()
    val letterJumpKey = feed.letterJumpKey()
    LibraryFeedContent(
        feed = feed,
        cellMinSize = cellMinSize,
        tileRatio = GAME_SKELETON_RATIO,
        onRetry = library::retry,
        empty = empty,
    ) {
        LibraryBrowseSwitch(
            view = display.gameView,
            grid = {
                LibraryFocusGrid(
                    items = feed.items,
                    itemKey = JarnLibraryGame::id,
                    focusedKey = focusedKey,
                    onFocused = onFocused,
                    geometry = LibraryGridGeometry(cellMinSize, rowSpacing = LibraryGridRowSpacing),
                    refocusSignal = library.stageRefocus,
                    jump = jump,
                    onLanded = library::landJump,
                    itemTitle = JarnLibraryGame::title,
                    letterJumpEnabled = letterJumpEnabled,
                    titleKey = letterJumpKey,
                ) { game, modifier ->
                    LibraryGameTile(
                        game = game,
                        onClick = { library.play(game) },
                        modifier = modifier.fillMaxWidth(),
                    )
                }
            },
            shelf = {
                LibraryShelf(
                    items = feed.items,
                    kind = LibraryShelfKind.Games,
                    content = gameShelfContent(library),
                    focusedKey = focusedKey,
                    onFocused = onFocused,
                    refocusSignal = library.stageRefocus,
                    jump = jump,
                    onLanded = library::landJump,
                    letterJumpEnabled = letterJumpEnabled,
                    titleKey = letterJumpKey,
                )
            },
        )
    }
}

@Composable
internal fun LibraryGameList(library: JarnLibraryStore) {
    JarnSystemSlide(library.state.openSystemId) { systemId ->
        LibrarySystemGames(library, systemId)
    }
}

/** Reads [systemId]'s own games, so a list sliding out during a system step keeps its contents. */
@Composable
private fun LibrarySystemGames(library: JarnLibraryStore, systemId: String?) {
    val state = library.state
    val feed = systemId?.let(state.gamesBySystem::get) ?: JarnLibraryFeed()
    val focusedId = systemId?.let(state.focusedGameBySystem::get)
    LibraryGames(
        feed = feed,
        focusedKey = feed.items.firstOrNull { it.id == focusedId }?.id ?: feed.items.firstOrNull()?.id,
        onFocused = { id -> if (library.state.openSystemId == systemId) library.update { focusOpenGame(id) } },
        library = library,
        empty = { LibraryEmptyMessage(gamesEmptyCopy(feed.hiddenCount)) },
        jump = library.jump?.takeIf { it.systemId == systemId && library.state.openSystemId == systemId },
    )
}

@Composable
internal fun LibraryGameListTitle(state: JarnLibraryState) {
    LibraryListTitle(state.openSystem?.title.orEmpty(), state.openGames.items, state.focusedOpenGame?.id)
}

/** A game list's name between its L1 and R1 caps, with where focus sits in it. */
@Composable
internal fun LibraryListTitle(title: String, games: List<JarnLibraryGame>, focusedId: String?) {
    val colors = JarnTheme.colors
    val position = games.indexOfFirst { it.id == focusedId }
    Row(
        modifier = Modifier.padding(vertical = JarnTheme.space.lg),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnKeyCap(JarnKey.L1)
        JarnText(title, JarnTheme.baseType.labelStrong, colors.textHigh, maxLines = 1)
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
