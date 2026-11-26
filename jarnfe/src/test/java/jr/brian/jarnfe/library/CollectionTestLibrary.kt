package jr.brian.jarnfe.library

import kotlinx.coroutines.flow.flowOf

/** A library with nothing in it but [collections], for collection screens under test. */
internal class CollectionTestLibrary(override val collections: JarnCollectionSource) : JarnLibrarySource {
    override fun observeSystems() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibrarySystem>()))
    override fun observeGames(systemId: String) = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
    override fun observeFavorites() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
    override fun observeRecent() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
    override fun refresh() = Unit
}

internal fun collectionGame(id: String, playtimeMinutes: Long? = null) = JarnLibraryGame(
    id = id,
    systemId = "n64",
    title = id,
    metadata = JarnGameMetadata(playtimeMinutes = playtimeMinutes),
)
