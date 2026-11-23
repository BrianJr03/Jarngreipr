package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.LibraryGame
import jr.brian.frontend.library.art.VisibleList
import jr.brian.jarnfe.R
import jr.brian.jarnfe.launch.JarnGameOptions
import jr.brian.jarnfe.launch.jarnGameOptionsOrNull
import jr.brian.jarnfe.launch.toGameRef
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.search.allowedSystemIds
import jr.brian.jarnfe.search.normalizeForSearch
import jr.brian.jarnfe.search.parseJarnSearchQuery
import jr.brian.jarnfe.search.searchGames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

internal const val CANVAS_BROWSE_LIMIT = 200

/** The same key Classic gives a pinned game, so a game is never pinned twice. */
internal fun canvasRomKey(systemId: String, path: String): String = "$systemId/$path"

/** [game] is set for a library game, whose art and location are resolved only once it is added. */
internal data class CanvasRomPick(val rom: JarnCanvasRom, val game: LibraryGame? = null)

internal fun LibraryGame.toCanvasRomPick(): CanvasRomPick {
    val path = absolutePath ?: relativePath
    return CanvasRomPick(
        rom = JarnCanvasRom(
            key = canvasRomKey(systemId, path),
            title = title,
            systemName = systemId,
            path = path,
            emulatorPackage = emulatorPackage,
            launchCommand = launchCommand,
        ),
        game = this,
    )
}

/** Keeps one result past [limit], so the caller can tell a capped list from a complete one. */
internal fun <T> canvasBrowseRows(rows: List<T>, limit: Int = CANVAS_BROWSE_LIMIT): Pair<List<T>, Boolean> =
    rows.take(limit) to (rows.size > limit)

internal data class CanvasRomBrowse(val rows: List<CanvasRomPick>, val capped: Boolean)

/** Reads titles only. Art is resolved for the games actually added, never the whole library. */
internal class CanvasRomCatalog(
    private val library: JarnLibraryData,
    private val options: JarnGameOptions?,
) {
    suspend fun browse(raw: String): CanvasRomBrowse = withContext(Dispatchers.Default) {
        val games = library.allGames.first().items
        val query = parseJarnSearchQuery(raw)
        val matches = if (query.isEmpty) {
            games.sortedBy { normalizeForSearch(it.title) }.take(CANVAS_BROWSE_LIMIT + 1)
        } else {
            val systems = library.systems.first().items.map { JarnLibrarySystem(it.id, it.displayName, it.gameCount) }
            val allowed = allowedSystemIds(query, systems, scopeSystemId = null)
            searchGames(games, query, allowed, CANVAS_BROWSE_LIMIT + 1, LibraryGame::systemId, LibraryGame::title)
        }
        val (rows, capped) = canvasBrowseRows(matches)
        CanvasRomBrowse(rows.map { it.toCanvasRomPick() }, capped)
    }

    suspend fun tiles(picks: List<CanvasRomPick>): List<JarnCanvasNewTile> {
        val games = picks.mapNotNull { it.game }
        val art = if (games.isEmpty()) {
            emptyMap()
        } else {
            library.withArt(flowOf(VisibleList(games))).first().items.associateBy { it.game.key }
        }
        return picks.map { pick ->
            val game = pick.game ?: return@map JarnCanvasNewTile.Rom(pick.rom.key)
            val artPath = art[game.key]?.defaultArtPath
            val located = options?.let { runCatching { it.locate(game.toGameRef(artPath), game.key) }.getOrNull() }
            val path = located?.absolutePath ?: pick.rom.path
            JarnCanvasNewTile.LibraryRom(
                pick.rom.copy(key = canvasRomKey(game.systemId, path), path = path, artPath = artPath),
            )
        }
    }
}

@Composable
internal fun rememberCanvasRomCatalog(): CanvasRomCatalog? {
    val context = LocalContext.current
    return remember(context) {
        context.jarnLibraryDataOrNull()?.let { CanvasRomCatalog(it, context.jarnGameOptionsOrNull()) }
    }
}

/** Pinned games first, with Browse a layer below. Picks carry across both, and B steps back one. */
@Composable
internal fun CanvasRomPickerFlow(
    data: JarnCanvasData,
    catalog: CanvasRomCatalog?,
    capacity: Int,
    placedKeys: Set<String>,
    onPicked: (List<CanvasRomPick>) -> Unit,
    onClose: () -> Unit,
) {
    var selection by remember { mutableStateOf(CanvasPickSelection<CanvasRomPick>(capacity = capacity)) }
    var browsing by remember { mutableStateOf(false) }
    var pinnedQuery by remember { mutableStateOf("") }
    var browseQuery by remember { mutableStateOf("") }
    val confirmLabel = stringResource(R.string.jarnfe_canvas_pick_add, selection.count)
    val onPage = stringResource(R.string.jarnfe_canvas_pick_on_page)
    val detail: (CanvasRomPick) -> String = { if (it.rom.key in placedKeys) onPage else it.rom.systemName }
    if (browsing && catalog != null) {
        val browse by produceState<CanvasRomBrowse?>(null, browseQuery) {
            value = null
            value = runCatching { catalog.browse(browseQuery) }.getOrDefault(CanvasRomBrowse(emptyList(), false))
        }
        CanvasMultiPicker(
            title = stringResource(R.string.jarnfe_canvas_pick_browse_title),
            rows = browse?.rows,
            rowKey = { it.rom.key },
            rowLabel = { it.rom.title },
            rowDetail = detail,
            isLocked = { it.rom.key in placedKeys },
            selection = selection,
            onSelection = { selection = it },
            query = browseQuery,
            onQuery = { browseQuery = it },
            confirmLabel = confirmLabel,
            onConfirm = onPicked,
            onClose = { browsing = false },
            note = stringResource(R.string.jarnfe_canvas_pick_browse_capped, CANVAS_BROWSE_LIMIT)
                .takeIf { browse?.capped == true },
        )
        return
    }
    var pinned by remember { mutableStateOf<List<CanvasRomPick>?>(null) }
    LaunchedEffect(data) {
        pinned = runCatching { data.pinnedRoms().map { CanvasRomPick(it) } }.getOrDefault(emptyList())
    }
    val rows = remember(pinned, pinnedQuery) {
        pinned?.let { list -> canvasPickerMatches(list, pinnedQuery) { it.rom.title } }
    }
    val browseLabel = stringResource(R.string.jarnfe_canvas_pick_browse)
    val browseDetail = stringResource(R.string.jarnfe_canvas_pick_browse_detail)
    CanvasMultiPicker(
        title = stringResource(R.string.jarnfe_canvas_pick_rom_title),
        rows = rows,
        rowKey = { it.rom.key },
        rowLabel = { it.rom.title },
        rowDetail = detail,
        isLocked = { it.rom.key in placedKeys },
        selection = selection,
        onSelection = { selection = it },
        query = pinnedQuery,
        onQuery = { pinnedQuery = it },
        confirmLabel = confirmLabel,
        onConfirm = onPicked,
        onClose = onClose,
        header = {
            if (catalog != null) {
                item(key = CANVAS_BROWSE_ROW_KEY) {
                    JarnNavigateRow(
                        label = browseLabel,
                        supporting = browseDetail,
                        onClick = { browsing = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}

private const val CANVAS_BROWSE_ROW_KEY = "canvas-picker-browse"
