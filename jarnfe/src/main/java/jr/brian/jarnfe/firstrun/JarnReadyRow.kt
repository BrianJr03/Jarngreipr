package jr.brian.jarnfe.firstrun

import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.ArtSource
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.jarnCanvasDataOrNull
import jr.brian.jarnfe.library.JarnFolderPicker
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.settings.labelRes
import kotlinx.coroutines.flow.map

internal const val READY_ROW_LIBRARY = "library"
internal const val READY_ROW_GAMES_FOLDER = "games-folder"
internal const val READY_ROW_ART = "art"
internal const val READY_ROW_FAVORITES = "favorites"
internal const val READY_ROW_CANVAS = "canvas"

/**
 * One line on the Ready card: a check when [isDone], otherwise a button that does [onAction].
 * A row with the same [key] as a built-in one replaces it. [leads] sorts first and takes first
 * focus while it still needs a tap. [linkLabel] is a small text link under the row.
 */
@Immutable
data class JarnReadyRow(
    val key: String,
    val text: String,
    val isDone: Boolean,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val linkLabel: String? = null,
    val onLink: (() -> Unit)? = null,
    val leads: Boolean = false,
    val order: Int = 0,
) {
    val needsTap: Boolean get() = !isDone && actionLabel != null && onAction != null
}

/** What a row can know about and do on the Ready card. */
class JarnReadyCard(val hasGames: Boolean, val skipToLook: () -> Unit)

/** Returns null to hide its row. Read on every composition, so state it reads keeps the row live. */
interface JarnReadyRowSource {
    @Composable
    fun row(card: JarnReadyCard): JarnReadyRow?
}

/** Rows other features add to the Ready card, by key. */
object JarnReadyRows {
    private val sources = mutableStateMapOf<String, JarnReadyRowSource>()

    fun register(key: String, source: JarnReadyRowSource) {
        sources[key] = source
    }

    fun unregister(key: String) {
        sources.remove(key)
    }

    internal val all: Map<String, JarnReadyRowSource> get() = sources
}

/** Built-in rows give way to added rows with the same key. Leading rows that need a tap come first. */
internal fun mergeReadyRows(builtIn: List<JarnReadyRow>, added: List<JarnReadyRow>): List<JarnReadyRow> {
    val addedKeys = added.mapTo(HashSet()) { it.key }
    return (builtIn.filterNot { it.key in addedKeys } + added)
        .sortedWith(compareBy({ !(it.leads && it.needsTap) }, { it.order }))
}

/** The folder row takes first focus only while there are no games and it still needs a tap. */
internal fun readyLeadRow(rows: List<JarnReadyRow>): JarnReadyRow? =
    rows.firstOrNull()?.takeIf { it.leads && it.needsTap }

@Composable
internal fun rememberReadyRows(card: JarnReadyCard): List<JarnReadyRow> {
    val builtIn = listOfNotNull(
        libraryRow(card),
        esdeImportReadyRow(card),
        artRow(),
        favoritesRow(),
        canvasRow(),
    )
    val added = JarnReadyRows.all.entries.mapNotNull { (rowKey, source) -> key(rowKey) { source.row(card) } }
    return mergeReadyRows(builtIn, added)
}

@Composable
private fun libraryRow(card: JarnReadyCard): JarnReadyRow {
    val systems = JarnLibrary.state.systems.items
    if (!card.hasGames) {
        return JarnReadyRow(
            key = READY_ROW_GAMES_FOLDER,
            text = stringResource(R.string.jarnfe_ready_no_games),
            isDone = false,
            actionLabel = stringResource(R.string.jarnfe_setup_add_folder),
            onAction = { JarnFolderPicker.open?.invoke(null) },
            linkLabel = stringResource(R.string.jarnfe_ready_skip_apps),
            onLink = card.skipToLook,
            leads = true,
        )
    }
    val games = systems.sumOf { it.gameCount }
    return JarnReadyRow(
        key = READY_ROW_LIBRARY,
        text = stringResource(
            R.string.jarnfe_first_run_existing_counts,
            pluralStringResource(R.plurals.jarnfe_first_run_existing_systems, systems.size, systems.size),
            pluralStringResource(R.plurals.jarnfe_first_run_existing_games, games, games),
        ),
        isDone = true,
        order = 0,
    )
}

@Composable
private fun artRow(): JarnReadyRow? {
    val context = LocalContext.current
    val data = remember(context) { context.jarnLibraryDataOrNull() } ?: return null
    return when (val summary = rememberFirstRunMedia(data)) {
        JarnMediaSummary.Checking -> null
        is JarnMediaSummary.Detected -> artFromRow(summary.source)
        is JarnMediaSummary.InUse -> artFromRow(summary.source)
        is JarnMediaSummary.Elsewhere -> artFromRow(summary.source)
        JarnMediaSummary.Missing -> JarnReadyRow(
            key = READY_ROW_ART,
            text = stringResource(R.string.jarnfe_ready_art_none),
            isDone = Environment.isExternalStorageManager(),
            actionLabel = stringResource(R.string.jarnfe_first_run_media_access),
            onAction = rememberFileAccessOpener(),
            order = 10,
        )
    }
}

@Composable
private fun artFromRow(source: ArtSource) = JarnReadyRow(
    key = READY_ROW_ART,
    text = stringResource(R.string.jarnfe_ready_art_from, stringResource(source.labelRes())),
    isDone = true,
    order = 10,
)

@Composable
private fun favoritesRow(): JarnReadyRow? {
    val count = JarnLibrary.state.favorites.items.size
    if (count == 0) return null
    return JarnReadyRow(
        key = READY_ROW_FAVORITES,
        text = pluralStringResource(R.plurals.jarnfe_ready_favorites, count, count),
        isDone = true,
        order = 20,
    )
}

@Composable
private fun canvasRow(): JarnReadyRow? {
    val context = LocalContext.current
    val data = remember(context) { context.jarnCanvasDataOrNull() } ?: return null
    val hasCanvas by produceState(false, data) {
        data.snapshot.map { snapshot -> snapshot.pages.any { it.layout.items.isNotEmpty() } }
            .collect { value = it }
    }
    if (!hasCanvas) return null
    return JarnReadyRow(
        key = READY_ROW_CANVAS,
        text = stringResource(R.string.jarnfe_ready_canvas),
        isDone = true,
        order = 30,
    )
}
