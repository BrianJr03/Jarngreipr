package jr.brian.jarnfe.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasModalSheet
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.launch.CollectionNameEntry
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.utilities.UtilityConfirmDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal sealed interface CollectionSheet {
    val collectionId: Long

    data class Menu(override val collectionId: Long) : CollectionSheet
    data class Rename(override val collectionId: Long) : CollectionSheet
    data class ConfirmDelete(override val collectionId: Long) : CollectionSheet
}

/** Select on a collection tile opens its Manage sheet, the same button that manages a system. */
internal fun JarnBindingsBuilder.manageCollectionBinding(state: JarnSessionState, library: JarnLibraryStore) {
    if (state.stageRoute != StageRoute.Library || state.librarySection != LibrarySection.Collections) return
    val selection = library.state.selection(state.stageRoute, state.librarySection)
    if (selection !is LibrarySelection.Collection) return
    on(JarnAction.Context, R.string.jarnfe_collection_manage) {
        library.collectionSheet = CollectionSheet.Menu(selection.collection.id)
    }
}

/** What the sheets do. Every way out puts focus back on a tile. */
internal class CollectionSheetActions(
    private val library: JarnLibraryStore,
    private val scope: CoroutineScope,
) {
    private val source: JarnCollectionSource get() = library.source.collections
    private val collections: List<JarnCollection> get() = library.state.collections.items

    fun show(sheet: CollectionSheet) {
        library.collectionSheet = sheet
    }

    /** Closes the sheet and focuses [collectionId], or the tile that was focused. */
    fun close(collectionId: Long? = null) {
        library.collectionSheet = null
        collectionId?.let { id -> library.update { focusCollection(id) } }
        library.takeCollectionFocus()
    }

    fun indexOf(collectionId: Long): Int = collections.indexOfFirst { it.id == collectionId }

    val lastIndex: Int get() = collections.lastIndex

    fun move(collectionId: Long, delta: Int) {
        library.update { focusCollection(collectionId) }
        scope.launch { source.moveCollection(collectionId, delta) }
    }

    fun rename(collectionId: Long, name: String, onProblem: (CollectionNameResult) -> Unit) {
        scope.launch {
            val result = source.renameCollection(collectionId, name)
            if (result is CollectionNameResult.Saved) close(collectionId) else onProblem(result)
        }
    }

    /** Focus goes to the next tile, or the one before when it was the last. */
    fun delete(collectionId: Long) {
        val index = indexOf(collectionId)
        val neighbour = (collections.getOrNull(index + 1) ?: collections.getOrNull(index - 1))?.id
        close(neighbour)
        scope.launch { source.deleteCollection(collectionId) }
    }
}

@Composable
internal fun LibraryCollectionManageHost(library: JarnLibraryStore) {
    val scope = rememberCoroutineScope()
    val actions = remember(library, scope) { CollectionSheetActions(library, scope) }
    DisposableEffect(library) { onDispose { library.collectionSheet = null } }
    val sheet = library.collectionSheet ?: return
    val collection = library.state.collections.items.firstOrNull { it.id == sheet.collectionId }
    if (collection == null) {
        LaunchedEffect(sheet) { actions.close() }
        return
    }
    when (sheet) {
        is CollectionSheet.Menu -> CollectionMenuSheet(collection, actions)
        is CollectionSheet.Rename -> CollectionRenameSheet(collection, actions)
        is CollectionSheet.ConfirmDelete -> CollectionDeleteDialog(collection, actions)
    }
}

@Composable
private fun CollectionMenuSheet(collection: JarnCollection, actions: CollectionSheetActions) {
    val first = remember { FocusRequester() }
    val index = actions.indexOf(collection.id)
    CanvasModalSheet(title = collection.name, initialFocus = first, onClose = { actions.close() }) {
        Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
            CollectionSheetRow(
                R.string.jarnfe_collection_rename,
                { actions.show(CollectionSheet.Rename(collection.id)) },
                Modifier.focusRequester(first),
            )
            CollectionSheetRow(
                R.string.jarnfe_collection_move_earlier,
                { actions.move(collection.id, -1) },
                enabled = index > 0,
            )
            CollectionSheetRow(
                R.string.jarnfe_collection_move_later,
                { actions.move(collection.id, 1) },
                enabled = index in 0 until actions.lastIndex,
            )
            CollectionSheetRow(
                R.string.jarnfe_collection_delete,
                { actions.show(CollectionSheet.ConfirmDelete(collection.id)) },
            )
        }
    }
}

@Composable
private fun CollectionSheetRow(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    JarnNavigateRow(
        label = stringResource(labelRes),
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}

@Composable
private fun CollectionRenameSheet(collection: JarnCollection, actions: CollectionSheetActions) {
    var problem by remember(collection.id) { mutableStateOf<CollectionNameResult?>(null) }
    val back = { actions.show(CollectionSheet.Menu(collection.id)) }
    JarnSheet(visible = true, onDismiss = back) {
        CollectionNameEntry(
            problem = problem,
            onDone = { name -> actions.rename(collection.id, name) { problem = it } },
            onCancel = back,
            initialName = collection.name,
        )
    }
}

@Composable
private fun CollectionDeleteDialog(collection: JarnCollection, actions: CollectionSheetActions) {
    UtilityConfirmDialog(
        visible = true,
        title = stringResource(R.string.jarnfe_collection_delete_title, collection.name),
        message = stringResource(R.string.jarnfe_collection_delete_message),
        confirmLabel = stringResource(R.string.jarnfe_collection_delete),
        onConfirm = { actions.delete(collection.id) },
        onDismiss = { actions.show(CollectionSheet.Menu(collection.id)) },
    )
}
