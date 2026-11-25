package jr.brian.jarnfe.library

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Immutable
data class JarnLibrarySystem(
    val id: String,
    val title: String,
    val gameCount: Int,
    val artPath: String? = null,
    val lastPlayedAtMs: Long? = null,
    val logoPath: String? = null,
    val favoriteCount: Int = 0,
    val playtimeMinutes: Long = 0,
    /** The image picked for this system's Now page. Wins over its art when it can be read. */
    val nowBackgroundPath: String? = null,
)

/** Every field is optional because scraped metadata is often partial. Blank values are not shown. */
@Immutable
data class JarnGameMetadata(
    val description: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val players: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val playtimeMinutes: Long? = null,
    val lastPlayedAtMs: Long? = null,
    val playCount: Int? = null,
)

/** [id] must be unique across the whole library and stable between refreshes. */
@Immutable
data class JarnLibraryGame(
    val id: String,
    val systemId: String,
    val title: String,
    val artPath: String? = null,
    val metadata: JarnGameMetadata = JarnGameMetadata(),
    val deckArtPath: String? = null,
    /** The art this game has with no art choices applied. Launch records keep this one. */
    val defaultArtPath: String? = artPath,
)

enum class JarnShelfGroup { Recent, MostPlayed, InSystem }

@Immutable
data class JarnShelfGame(val game: JarnLibraryGame, val group: JarnShelfGroup)

/** A system's capped Now data. [isLoaded] tells an empty shelf apart from one still loading. */
@Immutable
data class JarnSystemNow(
    val systemId: String,
    val shelf: List<JarnShelfGame> = emptyList(),
    val sceneArtPath: String? = null,
    val isLoaded: Boolean = false,
)

sealed interface JarnLibraryStatus {
    /** [loaded] and [total] are null when scan progress is unknown. */
    data class Loading(val loaded: Int? = null, val total: Int? = null) : JarnLibraryStatus
    data object Ready : JarnLibraryStatus
    data object Unavailable : JarnLibraryStatus
}

/** [items] may hold cached content during [JarnLibraryStatus.Loading] or Unavailable. */
@Immutable
data class JarnLibraryFeed<out T>(
    val items: List<T> = emptyList(),
    val status: JarnLibraryStatus = JarnLibraryStatus.Loading(),
    val hiddenCount: Int = 0,
    /** The order [items] are in. Null for lists the user can't sort, like Favorites and Recent. */
    val sort: JarnGameSort? = null,
) {
    companion object {
        fun <T> ready(items: List<T>, hiddenCount: Int = 0, sort: JarnGameSort? = null) =
            JarnLibraryFeed(items, JarnLibraryStatus.Ready, hiddenCount, sort)
    }
}

/**
 * What the Library reads. Flows should emit promptly and keep emitting on change. A thrown flow
 * becomes Unavailable with its last items kept. [observeRecent] is ordered most recent first.
 */
interface JarnLibrarySource {
    fun observeSystems(): Flow<JarnLibraryFeed<JarnLibrarySystem>>
    fun observeGames(systemId: String): Flow<JarnLibraryFeed<JarnLibraryGame>>
    fun observeFavorites(): Flow<JarnLibraryFeed<JarnLibraryGame>>
    fun observeRecent(): Flow<JarnLibraryFeed<JarnLibraryGame>>

    /** Every visible game across systems, in name order, so letter jump works there too. */
    fun observeAllGames(): Flow<JarnLibraryFeed<JarnLibraryGame>> =
        flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>(), sort = JarnGameSort.Name))
    fun observeSystemNow(systemId: String): Flow<JarnSystemNow> =
        flowOf(JarnSystemNow(systemId, isLoaded = true))
    fun observeWideArt(gameId: String): Flow<String?> = flowOf(null)
    fun refresh()
}

/** The production default until a real adapter is connected, so the app never shows fake games. */
object EmptyJarnLibrarySource : JarnLibrarySource {
    override fun observeSystems() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibrarySystem>()))
    override fun observeGames(systemId: String) = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
    override fun observeFavorites() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
    override fun observeRecent() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
    override fun refresh() = Unit
}
