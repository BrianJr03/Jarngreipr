package jr.brian.jarnfe.launch

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnChoiceRow
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.collections.COLLECTION_NAME_MAX_LENGTH
import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.jarnfe.R
import jr.brian.jarnfe.keyboard.JarnTextKeyboard
import jr.brian.jarnfe.library.JarnCollection
import jr.brian.jarnfe.library.JarnCollectionSource
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** A new collection starts with the game in it, so one press of Done does both. */
internal suspend fun JarnCollectionSource.createWith(name: String, gameId: String): CollectionNameResult {
    val result = createCollection(name)
    if (result is CollectionNameResult.Saved) addToCollection(result.collectionId, gameId)
    return result
}

/** Adds the game when it is missing from [collectionId], or takes it out when it is there. */
internal suspend fun JarnCollectionSource.toggle(collectionId: Long, gameId: String, isMember: Boolean): Boolean =
    if (isMember) removeFromCollection(collectionId, gameId) else addToCollection(collectionId, gameId)

/** Null for a name that saved. Every other result has its own message. */
@StringRes
internal fun CollectionNameResult.problemRes(): Int? = when (this) {
    is CollectionNameResult.Saved -> null
    CollectionNameResult.Blank -> R.string.jarnfe_collection_name_blank
    CollectionNameResult.TooLong -> R.string.jarnfe_collection_name_too_long
    CollectionNameResult.Duplicate -> R.string.jarnfe_collection_name_duplicate
    CollectionNameResult.NotFound -> R.string.jarnfe_collection_save_failed
}

@Composable
private fun rememberCollections(source: JarnCollectionSource): List<JarnCollection>? {
    val collections by produceState<List<JarnCollection>?>(null, source) {
        source.observeCollections().catch { }.collect { value = it.items }
    }
    return collections
}

@Composable
private fun rememberGameCollections(source: JarnCollectionSource, gameId: String): Set<Long>? {
    val ids by produceState<Set<Long>?>(null, source, gameId) {
        source.observeGameCollections(gameId).catch { emit(emptySet()) }.collect { value = it }
    }
    return ids
}

/** The More sheet's way into the picker, showing how many collections hold the game. */
@Composable
internal fun GameCollectionsRow(
    game: JarnLibraryGame,
    onOpen: () -> Unit,
    source: JarnCollectionSource = JarnLibrary.source.collections,
) {
    val count = rememberGameCollections(source, game.id)?.size
    val focus = remember { FocusRequester() }
    ReturnFocusEffect(GameActionsPage.Collections, focus)
    JarnValueRow(
        label = stringResource(R.string.jarnfe_section_collections),
        value = count?.toString().orEmpty(),
        onClick = onOpen,
        modifier = Modifier.focusRequester(focus),
    )
}

@Composable
private fun ReturnFocusEffect(page: GameActionsPage, requester: FocusRequester) {
    val state = JarnGameActions
    LaunchedEffect(state.returnFrom) {
        if (state.returnFrom != page) return@LaunchedEffect
        withFrameNanos { }
        runCatching { requester.requestFocus() }
        state.returnHandled()
    }
}

/**
 * New collection first, then one row per collection. A on a collection adds or removes the game,
 * and the check shows what is saved. [firstFocus] lands on New collection.
 */
@Composable
internal fun GameCollectionsPage(
    game: JarnLibraryGame,
    firstFocus: FocusRequester,
    source: JarnCollectionSource = JarnLibrary.source.collections,
) {
    var naming by remember(game.id) { mutableStateOf(false) }
    var problem by remember(game.id) { mutableStateOf<CollectionNameResult?>(null) }
    val scope = rememberCoroutineScope()
    if (naming) {
        CollectionNameEntry(
            problem = problem,
            onDone = { name ->
                scope.launch {
                    val result = source.createWith(name, game.id)
                    problem = result.takeUnless { it is CollectionNameResult.Saved }
                    if (problem == null) naming = false
                }
            },
            onCancel = {
                problem = null
                naming = false
            },
        )
    } else {
        CollectionChoices(game, source, firstFocus, onNew = { naming = true })
    }
}

@Composable
private fun CollectionChoices(
    game: JarnLibraryGame,
    source: JarnCollectionSource,
    firstFocus: FocusRequester,
    onNew: () -> Unit,
) {
    val collections = rememberCollections(source)
    val memberOf = rememberGameCollections(source, game.id)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstFocus.requestFocus() }
    }
    val colors = JarnTheme.colors
    JarnText(stringResource(R.string.jarnfe_section_collections), JarnTheme.type.heading, colors.textHigh, maxLines = 2)
    JarnText(stringResource(R.string.jarnfe_game_collections_supporting), JarnTheme.type.body, colors.textMid, maxLines = 3)
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_collection_new),
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().focusRequester(firstFocus),
        )
        collections.orEmpty().forEach { collection ->
            val isMember = memberOf?.contains(collection.id) == true
            JarnChoiceRow(
                label = collection.name,
                chosen = isMember,
                onClick = { scope.launch { source.toggle(collection.id, game.id, isMember) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The name keyboard, opened on a key, with the last save's problem above it. */
@Composable
internal fun CollectionNameEntry(
    problem: CollectionNameResult?,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
    initialName: String = "",
) {
    problem?.problemRes()?.let { res ->
        JarnText(
            text = stringResource(res, COLLECTION_NAME_MAX_LENGTH),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.danger,
            maxLines = 2,
        )
    }
    JarnTextKeyboard(
        label = stringResource(R.string.jarnfe_collection_name_label),
        masked = false,
        onDone = onDone,
        onCancel = onCancel,
        modifier = Modifier.fillMaxWidth(),
        initialText = initialName,
        maxLength = COLLECTION_NAME_MAX_LENGTH,
        takesFocus = true,
    )
}
