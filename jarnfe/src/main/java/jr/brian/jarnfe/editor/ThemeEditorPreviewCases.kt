package jr.brian.jarnfe.editor

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.MediaKind
import jr.brian.core.ui.theme.LocalJarnMotion
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnGameMetadata
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryFeed
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibraryState
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.themekit.LocalThemeHiddenMedia
import kotlin.random.Random

/** A preview's game count for the many-games case: past three digits' worth of scrolling. */
internal const val PREVIEW_MANY_GAMES = 999

private const val PREVIEW_ID_PREFIX = "editor-preview:"

/** What the canvas pretends about the library, so a layout can be checked against hard data. */
internal enum class ThemeEditorPreviewCase(@param:StringRes val labelRes: Int) {
    Library(R.string.jarnfe_editor_preview_library),
    NoVideo(R.string.jarnfe_editor_preview_no_video),
    NoCover(R.string.jarnfe_editor_preview_no_cover),
    NoInfo(R.string.jarnfe_editor_preview_no_info),
    LongTitle(R.string.jarnfe_editor_preview_long_title),
    EmptySystem(R.string.jarnfe_editor_preview_empty_system),
    OneGame(R.string.jarnfe_editor_preview_one_game),
    ManyGames(R.string.jarnfe_editor_preview_many_games),
    ;

    /** Media the focused game seems not to have in this case. */
    val hiddenMedia: Set<MediaKind>
        get() = when (this) {
            NoVideo -> setOf(MediaKind.Video)
            NoCover -> setOf(MediaKind.Cover)
            else -> emptySet()
        }
}

/** The preview's picks: which system and game it centers on, the case, and reduced motion. */
@Immutable
internal data class ThemeEditorPreviewSettings(
    val case: ThemeEditorPreviewCase = ThemeEditorPreviewCase.Library,
    val systemId: String? = null,
    val gameId: String? = null,
    val reducedMotion: Boolean = false,
)

/** Editor-only and never saved: closing the editor puts it back. */
internal object ThemeEditorPreview {
    var settings by mutableStateOf(ThemeEditorPreviewSettings())

    fun update(transform: ThemeEditorPreviewSettings.() -> ThemeEditorPreviewSettings) {
        settings = settings.transform()
    }

    fun reset() {
        settings = ThemeEditorPreviewSettings()
    }

    /** The next system or game in the library after the current pick, wrapping. */
    fun <T> next(items: List<T>, current: String?, id: (T) -> String): String? {
        if (items.isEmpty()) return null
        val index = items.indexOfFirst { id(it) == current }
        return id(items[(index + 1).mod(items.size)])
    }

    fun shuffle(state: JarnLibraryState, random: Random = Random.Default) = update {
        copy(
            systemId = state.systems.items.randomOrNull(random)?.id,
            gameId = state.allGames.items.randomOrNull(random)?.id,
        )
    }
}

/** Sample names for a library with nothing in it, and the long title case. */
internal data class ThemeEditorPreviewText(val system: String, val game: String, val longTitle: String)

/**
 * The library the canvas draws: [real]'s feeds with the picked system and game focused and the
 * case applied. Synthetic items get ids no real item has, so nothing reaches the library.
 */
internal fun previewLibraryState(
    real: JarnLibraryState,
    settings: ThemeEditorPreviewSettings,
    text: ThemeEditorPreviewText,
): JarnLibraryState {
    val case = settings.case
    val baseSystem = real.systems.items.firstOrNull { it.id == settings.systemId } ?: real.focusedSystem
        ?: JarnLibrarySystem(id = PREVIEW_ID_PREFIX + "system", title = text.system, gameCount = 1)
    val baseGame = real.allGames.items.firstOrNull { it.id == settings.gameId } ?: real.focusedAllGame
        ?: real.recent.items.firstOrNull()
        ?: JarnLibraryGame(id = PREVIEW_ID_PREFIX + "game", systemId = baseSystem.id, title = text.game)
    val system = baseSystem.forCase(case, text.longTitle)
    val game = baseGame.forCase(case, text.longTitle)
    val games = when (case) {
        ThemeEditorPreviewCase.EmptySystem -> emptyList()
        ThemeEditorPreviewCase.OneGame -> listOf(game)
        ThemeEditorPreviewCase.ManyGames -> manyGames(game, real.allGames.items)
        else -> real.allGames.items.replacing(game)
    }
    return real.copy(
        systems = JarnLibraryFeed.ready(real.systems.items.replacing(system), sort = real.systems.sort),
        allGames = JarnLibraryFeed.ready(games, sort = real.allGames.sort),
        focusedSystemId = system.id,
        focusedAllGameId = game.id.takeIf { games.isNotEmpty() },
    )
}

private fun JarnLibrarySystem.forCase(case: ThemeEditorPreviewCase, longTitle: String): JarnLibrarySystem = when (case) {
    ThemeEditorPreviewCase.EmptySystem ->
        copy(gameCount = 0, favoriteCount = 0, playtimeMinutes = 0, artPath = null, logoPath = null, nowBackgroundPath = null)
    ThemeEditorPreviewCase.OneGame -> copy(gameCount = 1)
    ThemeEditorPreviewCase.ManyGames -> copy(gameCount = PREVIEW_MANY_GAMES)
    ThemeEditorPreviewCase.LongTitle -> copy(title = longTitle)
    else -> this
}

private fun JarnLibraryGame.forCase(case: ThemeEditorPreviewCase, longTitle: String): JarnLibraryGame = when (case) {
    ThemeEditorPreviewCase.NoCover -> copy(artPath = null, deckArtPath = null, defaultArtPath = null)
    ThemeEditorPreviewCase.NoInfo -> copy(metadata = JarnGameMetadata())
    ThemeEditorPreviewCase.LongTitle -> copy(title = longTitle)
    else -> this
}

/** [game] first, then the library's games over and over with fresh ids, to [PREVIEW_MANY_GAMES]. */
private fun manyGames(game: JarnLibraryGame, library: List<JarnLibraryGame>): List<JarnLibraryGame> {
    val pool = library.ifEmpty { listOf(game) }
    return listOf(game) + (1 until PREVIEW_MANY_GAMES).map { index ->
        pool[index % pool.size].copy(id = "$PREVIEW_ID_PREFIX$index")
    }
}

/** The list with [item] in place of the one with its id, or [item] alone when the list is empty. */
private fun <T> List<T>.replacing(item: T, id: (T) -> String): List<T> =
    if (isEmpty()) listOf(item) else map { if (id(it) == id(item)) item else it }

@JvmName("replacingSystem")
private fun List<JarnLibrarySystem>.replacing(item: JarnLibrarySystem) = replacing(item, JarnLibrarySystem::id)

@JvmName("replacingGame")
private fun List<JarnLibraryGame>.replacing(item: JarnLibraryGame) = replacing(item, JarnLibraryGame::id)

/**
 * A library store of its own for the canvas, rebuilt from the real library and the preview
 * settings whenever either changes. It never observes a source, so nothing it shows is written back.
 */
@Composable
internal fun rememberEditorPreviewLibrary(): JarnLibraryStore {
    val text = ThemeEditorPreviewText(
        system = stringResource(R.string.jarnfe_editor_preview_sample_system),
        game = stringResource(R.string.jarnfe_editor_preview_sample_game),
        longTitle = stringResource(R.string.jarnfe_editor_preview_long_title_sample),
    )
    val real = JarnLibrary.state
    val settings = ThemeEditorPreview.settings
    LaunchedEffect(Unit) { JarnLibrary.wantAllGames() }
    return remember(real, settings, text) {
        val shown = previewLibraryState(real, settings, text)
        JarnLibraryStore().apply { update { shown } }
    }
}

/** The preview's hidden media and motion around the canvas, and a fresh preview when the editor closes. */
@Composable
internal fun ThemeEditorPreviewScope(content: @Composable () -> Unit) {
    val settings = ThemeEditorPreview.settings
    DisposableEffect(Unit) { onDispose { ThemeEditorPreview.reset() } }
    val motion = LocalJarnMotion.current
    CompositionLocalProvider(
        LocalThemeHiddenMedia provides settings.case.hiddenMedia,
        LocalJarnMotion provides if (settings.reducedMotion) motion.copy(reduced = true, ambientReduced = true) else motion,
        content = content,
    )
}
