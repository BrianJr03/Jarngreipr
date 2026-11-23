package jr.brian.jarnfe.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnChip
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.keyboard.JarnKeyboardAutoFocus
import jr.brian.jarnfe.keyboard.JarnKeyboardGrid
import jr.brian.jarnfe.keyboard.JarnKeyboardKey
import jr.brian.jarnfe.keyboard.JarnKeyboardSize
import jr.brian.jarnfe.keyboard.jarnKeyRow
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.session.JarnSession

private val SearchControls = listOf(JarnKeyboardKey.Space, JarnKeyboardKey.Delete, JarnKeyboardKey.Clear)

/** The controller keyboard for Search. Results update live on Stage. */
@Composable
internal fun JarnSearchDeck(modifier: Modifier = Modifier, search: JarnSearchStore = JarnSearch) {
    val firstKey = remember { FocusRequester() }
    JarnKeyboardAutoFocus(firstKey)
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = { searchKeyboardBindings(JarnSession, search) },
    ) {
        val scroll = rememberScrollState()
        JarnRevealScrollTop(scroll) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = JarnTheme.space.deckMargin, vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                SearchQueryField(search.query)
                SearchScopeChip(search) { firstKey.requestFocus() }
                SearchKeyboard(search, firstKey)
            }
        }
    }
}

@Composable
private fun SearchQueryField(query: String) {
    val colors = JarnTheme.colors
    JarnSurface(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(horizontal = JarnTheme.space.lg, vertical = JarnTheme.space.md)) {
            JarnText(
                text = query.ifEmpty { stringResource(R.string.jarnfe_search_placeholder) },
                style = JarnTheme.type.body,
                color = if (query.isEmpty()) colors.textMid else colors.textHigh,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SearchScopeChip(search: JarnSearchStore, onRemoved: () -> Unit) {
    val scope = search.scopeSystemId ?: return
    val title = JarnLibrary.state.systems.items.firstOrNull { it.id == scope }?.title ?: scope
    JarnChip(
        label = stringResource(R.string.jarnfe_search_scope, title),
        selected = true,
        onClick = {
            onRemoved()
            search.clearScope()
        },
    )
}

@Composable
private fun SearchKeyboard(search: JarnSearchStore, firstKey: FocusRequester) {
    val rawRows = stringArrayResource(R.array.jarnfe_search_keyboard_rows)
    val rows = remember(rawRows.toList()) { rawRows.map(::jarnKeyRow) }
    JarnKeyboardGrid(
        rows = rows,
        controls = SearchControls,
        firstKey = firstKey,
        onKey = { key -> search.press(key, firstKey) },
        size = JarnKeyboardSize.Roomy,
        letterLabel = { it.uppercase() },
    )
}

private fun JarnSearchStore.press(key: JarnKeyboardKey, firstKey: FocusRequester) {
    when (key) {
        is JarnKeyboardKey.Letter -> type(key.char.toString())
        JarnKeyboardKey.Space -> type(" ")
        JarnKeyboardKey.Delete -> deleteLast()
        JarnKeyboardKey.Clear -> {
            clearQuery()
            firstKey.requestFocus()
        }
        JarnKeyboardKey.Shift, JarnKeyboardKey.Symbols, JarnKeyboardKey.Done -> Unit
    }
}
