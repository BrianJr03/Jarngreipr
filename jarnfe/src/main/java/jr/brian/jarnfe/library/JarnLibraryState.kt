package jr.brian.jarnfe.library

import androidx.annotation.StringRes
import jr.brian.jarnfe.R
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute

sealed interface LibrarySelection {
    data object None : LibrarySelection
    data class System(val system: JarnLibrarySystem) : LibrarySelection
    /** [resume] is the Continue playing card, shown on Now as a Resume poster. */
    data class Game(val game: JarnLibraryGame, val resume: Boolean = false) : LibrarySelection
}

sealed interface LibraryHero {
    data class ContinuePlaying(val game: JarnLibraryGame) : LibraryHero
    data class FocusedSystem(val system: JarnLibrarySystem) : LibraryHero
}

internal const val ALSO_RECENT_COUNT = 4

/** What A does right now, so a hint is only declared when the press has an effect. */
enum class LibraryConfirm { Open, Play, Generic, None }

data class JarnLibraryState(
    val systems: JarnLibraryFeed<JarnLibrarySystem> = JarnLibraryFeed(),
    val favorites: JarnLibraryFeed<JarnLibraryGame> = JarnLibraryFeed(),
    val recent: JarnLibraryFeed<JarnLibraryGame> = JarnLibraryFeed(),
    val gamesBySystem: Map<String, JarnLibraryFeed<JarnLibraryGame>> = emptyMap(),
    val openSystemId: String? = null,
    val focusedSystemId: String? = null,
    val focusedFavoriteId: String? = null,
    val focusedRecentId: String? = null,
    val focusedGameBySystem: Map<String, String> = emptyMap(),
    val searchSelection: LibrarySelection = LibrarySelection.None,
) {
    val focusedSystem: JarnLibrarySystem?
        get() = systems.items.firstOrNull { it.id == focusedSystemId } ?: systems.items.firstOrNull()

    val openSystem: JarnLibrarySystem?
        get() = systems.items.firstOrNull { it.id == openSystemId }

    val openGames: JarnLibraryFeed<JarnLibraryGame>
        get() = openSystemId?.let(gamesBySystem::get) ?: JarnLibraryFeed()

    val focusedOpenGame: JarnLibraryGame?
        get() {
            val games = openGames.items
            val focusedId = openSystemId?.let(focusedGameBySystem::get)
            return games.firstOrNull { it.id == focusedId } ?: games.firstOrNull()
        }

    val focusedFavorite: JarnLibraryGame?
        get() = favorites.items.firstOrNull { it.id == focusedFavoriteId } ?: favorites.items.firstOrNull()

    val focusedRecent: JarnLibraryGame?
        get() = recent.items.firstOrNull { it.id == focusedRecentId } ?: recent.items.firstOrNull()

    val hero: LibraryHero?
        get() = recent.items.firstOrNull()?.let(LibraryHero::ContinuePlaying)
            ?: focusedSystem?.let(LibraryHero::FocusedSystem)

    /** The Continue playing game while its banner has focus on the Library's Systems tab. */
    fun resumeSelection(route: StageRoute, section: LibrarySection, heroFocused: Boolean): LibrarySelection? {
        if (!heroFocused || route != StageRoute.Library || section != LibrarySection.Systems) return null
        val game = (hero as? LibraryHero.ContinuePlaying)?.game ?: return null
        return LibrarySelection.Game(game, resume = true)
    }

    /** The next recent games after the Continue playing one, newest first, without [missing] ones. */
    fun alsoRecent(missing: Set<String> = emptySet(), limit: Int = ALSO_RECENT_COUNT): List<JarnLibraryGame> =
        recent.items.drop(1).filterNot { it.id in missing }.take(limit)

    fun selection(route: StageRoute, section: LibrarySection): LibrarySelection = when (route) {
        StageRoute.Library -> when (section) {
            LibrarySection.Systems -> focusedSystem.asSelection()
            LibrarySection.Favorites -> focusedFavorite.asSelection()
            LibrarySection.Recent -> focusedRecent.asSelection()
        }
        StageRoute.GameList -> focusedOpenGame?.let(LibrarySelection::Game) ?: openSystem.asSelection()
        StageRoute.Settings, StageRoute.Systems -> LibrarySelection.None
        StageRoute.Search -> searchSelection
    }

    fun confirmFor(route: StageRoute, section: LibrarySection, canPlay: Boolean): LibraryConfirm {
        val play = if (canPlay) LibraryConfirm.Play else LibraryConfirm.None
        return when (route) {
            StageRoute.Settings -> LibraryConfirm.Generic
            StageRoute.Search -> when (searchSelection) {
                LibrarySelection.None -> LibraryConfirm.None
                is LibrarySelection.System -> LibraryConfirm.Open
                is LibrarySelection.Game -> play
            }
            StageRoute.Systems -> LibraryConfirm.None
            StageRoute.GameList -> when {
                openGames.items.isNotEmpty() -> play
                openGames.status == JarnLibraryStatus.Unavailable -> LibraryConfirm.Generic
                else -> LibraryConfirm.None
            }
            StageRoute.Library -> when (section) {
                LibrarySection.Systems -> systems.confirm(LibraryConfirm.Open)
                LibrarySection.Favorites -> favorites.confirm(play)
                LibrarySection.Recent -> recent.confirm(play)
            }
        }
    }

    /** True when the game list has lost its system, for example after a restore or a rescan. */
    fun gameListIsOrphaned(route: StageRoute): Boolean {
        if (route != StageRoute.GameList) return false
        if (openSystemId == null) return true
        return systems.status == JarnLibraryStatus.Ready && openSystem == null
    }

    fun openSystem(systemId: String): JarnLibraryState =
        copy(openSystemId = systemId, focusedSystemId = systemId)

    /** Stops at the first and last system, matching how every other shoulder tab behaves. */
    fun stepOpenSystem(delta: Int): JarnLibraryState {
        val items = systems.items
        val current = items.indexOfFirst { it.id == openSystemId }
        if (current < 0) return this
        return openSystem(items[(current + delta).coerceIn(items.indices)].id)
    }

    fun focusSystem(systemId: String): JarnLibraryState = copy(focusedSystemId = systemId)

    fun focusFavorite(gameId: String): JarnLibraryState = copy(focusedFavoriteId = gameId)

    fun focusRecent(gameId: String): JarnLibraryState = copy(focusedRecentId = gameId)

    fun focusSearchResult(selection: LibrarySelection): JarnLibraryState =
        copy(searchSelection = selection)

    fun focusOpenGame(gameId: String): JarnLibraryState {
        val systemId = openSystemId ?: return this
        return copy(focusedGameBySystem = focusedGameBySystem + (systemId to gameId))
    }

    private fun JarnLibraryFeed<*>.confirm(whenPopulated: LibraryConfirm): LibraryConfirm = when {
        items.isNotEmpty() -> whenPopulated
        status is JarnLibraryStatus.Loading -> LibraryConfirm.None
        else -> LibraryConfirm.Generic
    }

    private fun JarnLibrarySystem?.asSelection(): LibrarySelection =
        this?.let(LibrarySelection::System) ?: LibrarySelection.None

    private fun JarnLibraryGame?.asSelection(): LibrarySelection =
        this?.let(LibrarySelection::Game) ?: LibrarySelection.None
}

internal data class LibraryMetadataField(@param:StringRes val labelRes: Int, val value: String)

/** Only fields that hold a real value, so the Deck never shows an empty row. */
internal fun JarnGameMetadata.textFields(): List<LibraryMetadataField> = listOf(
    R.string.jarnfe_library_meta_developer to developer,
    R.string.jarnfe_library_meta_publisher to publisher,
    R.string.jarnfe_library_meta_genre to genre,
    R.string.jarnfe_library_meta_released to releaseDate,
    R.string.jarnfe_library_meta_players to players,
    R.string.jarnfe_library_meta_rating to rating,
).mapNotNull { (labelRes, value) ->
    value?.trim()?.takeIf { it.isNotEmpty() }?.let { LibraryMetadataField(labelRes, it) }
}
