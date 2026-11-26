package jr.brian.jarnfe.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.library.JarnSystemTile
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.transition.JarnSystemSlide
import jr.brian.jarnfe.transition.jarnDepthAnchor
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal const val COLLECTION_MOSAIC_SIZE = 4
internal const val COLLECTION_MOSAIC_TAG = "collection_mosaic"
internal const val COLLECTION_ICON_TAG = "collection_icon"
private const val COLLECTION_SKELETON_RATIO = 16f / 10f
private const val MOSAIC_COLUMNS = 2
private val CollectionIconSize = 40.dp

/** A depth push into a collection grows from its tile, kept apart from system ids. */
internal fun collectionAnchorKey(collectionId: Long?): String? = collectionId?.let { "collection:$it" }

private fun JarnCollection.key(): String = id.toString()

/**
 * A collection's games, A to Z, so the mosaic never reshuffles. Null until known. Each tile reads
 * its own collection, which stays cheap for the few collections a player keeps.
 */
@Composable
internal fun rememberCollectionGames(
    source: JarnCollectionSource,
    collectionId: Long,
    limit: Int = Int.MAX_VALUE,
): List<JarnLibraryGame>? {
    val games by produceState<List<JarnLibraryGame>?>(null, source, collectionId, limit) {
        source.observeCollectionGames(collectionId)
            .map { it.items.take(limit) }
            .catch { emit(emptyList()) }
            .collect { value = it }
    }
    return games
}

/** Four covers as a 2 by 2 mosaic, or the collection icon when there are fewer. */
@Composable
internal fun BoxScope.LibraryCollectionArt(covers: List<JarnLibraryGame>?) {
    if (covers != null && covers.size >= COLLECTION_MOSAIC_SIZE) {
        LibraryCollectionMosaic(covers)
    } else {
        LibraryCollectionIcon()
    }
}

@Composable
private fun BoxScope.LibraryCollectionMosaic(covers: List<JarnLibraryGame>) {
    Column(Modifier.matchParentSize().testTag(COLLECTION_MOSAIC_TAG)) {
        covers.take(COLLECTION_MOSAIC_SIZE).chunked(MOSAIC_COLUMNS).forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { game ->
                    Box(Modifier.weight(1f).fillMaxSize()) { LibraryArt(game.artPath, game.title) }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LibraryCollectionIcon() {
    val colors = JarnTheme.colors
    Box(
        modifier = Modifier.matchParentSize().background(colors.raised).testTag(COLLECTION_ICON_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_jarn_collection),
            contentDescription = null,
            modifier = Modifier.size(CollectionIconSize),
            colorFilter = ColorFilter.tint(colors.accent),
        )
    }
}

/** The Collections tab: a tile per collection, in the user's order. Select on a tile manages it. */
@Composable
internal fun LibraryCollections(
    library: JarnLibraryStore,
    onOpenCollection: (Long) -> Unit,
    emptyActions: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        LibraryCollectionGrid(library, onOpenCollection, emptyActions)
        LibraryCollectionManageHost(library)
    }
}

@Composable
private fun LibraryCollectionGrid(
    library: JarnLibraryStore,
    onOpenCollection: (Long) -> Unit,
    emptyActions: @Composable () -> Unit,
) {
    val state = library.state
    val cellMinSize = SystemCellMinSize * rememberJarnDisplay().systemTileScale
    LibraryFeedContent(
        feed = state.collections,
        cellMinSize = cellMinSize,
        tileRatio = COLLECTION_SKELETON_RATIO,
        onRetry = library::retry,
        empty = {
            LibraryEmptyMessage(
                LibraryEmptyCopy(R.string.jarnfe_collections_empty_title, R.string.jarnfe_collections_empty_message),
                emptyActions,
            )
        },
    ) {
        LibraryFocusGrid(
            items = state.collections.items,
            itemKey = { it.key() },
            focusedKey = state.focusedCollection?.key(),
            onFocused = { key -> key.toLongOrNull()?.let { id -> library.update { focusCollection(id) } } },
            cellMinSize = cellMinSize,
            refocusSignal = library.stageRefocus,
            rowSpacing = LibraryGridRowSpacing,
            letterJumpEnabled = false,
        ) { collection, modifier ->
            LibraryCollectionTile(
                collection = collection,
                source = library.source.collections,
                onClick = { onOpenCollection(collection.id) },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun LibraryCollectionTile(
    collection: JarnCollection,
    source: JarnCollectionSource,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val covers = rememberCollectionGames(source, collection.id, COLLECTION_MOSAIC_SIZE)
    JarnSystemTile(
        title = collection.name,
        subtitle = gameCountText(collection.gameCount),
        onClick = onClick,
        modifier = modifier.jarnDepthAnchor(collectionAnchorKey(collection.id).orEmpty()).fillMaxWidth(),
        focusBorder = true,
    ) { LibraryCollectionArt(covers) }
}

/** One collection's games, laid out like a system's. L1 and R1 step to the next collection. */
@Composable
internal fun LibraryCollectionList(library: JarnLibraryStore) {
    JarnSystemSlide(
        systemId = library.state.openCollectionId?.toString(),
        orderOf = { key -> library.state.collections.items.indexOfFirst { it.key() == key } },
    ) { key -> LibraryCollectionGames(library, key?.toLongOrNull()) }
}

@Composable
private fun LibraryCollectionGames(library: JarnLibraryStore, collectionId: Long?) {
    val state = library.state
    val feed = collectionId?.let(state.gamesByCollection::get) ?: JarnLibraryFeed()
    val focusedId = collectionId?.let(state.focusedGameByCollection::get)
    LibraryGames(
        feed = feed,
        focusedKey = feed.items.firstOrNull { it.id == focusedId }?.id ?: feed.items.firstOrNull()?.id,
        onFocused = { id -> if (library.state.openCollectionId == collectionId) library.update { focusCollectionGame(id) } },
        library = library,
        empty = {
            LibraryEmptyMessage(LibraryEmptyCopy(R.string.jarnfe_collection_empty_title, R.string.jarnfe_collection_empty_message))
        },
    )
}

@Composable
internal fun LibraryCollectionListTitle(state: JarnLibraryState) {
    LibraryListTitle(
        title = state.openCollection?.name.orEmpty(),
        games = state.openCollectionGames.items,
        focusedId = state.focusedCollectionGame?.id,
    )
}
