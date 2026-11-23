package jr.brian.jarnfe.search

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnChip
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.session.JarnSession

private val SearchKeyMinHeight = 44.dp
private const val WIDE_KEY_WEIGHT = 2f

/** The controller keyboard for Search. Results update live on Stage. */
@Composable
internal fun JarnSearchDeck(modifier: Modifier = Modifier, search: JarnSearchStore = JarnSearch) {
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstKey.requestFocus() }
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
    val rows = stringArrayResource(R.array.jarnfe_search_keyboard_rows)
    rows.forEachIndexed { rowIndex, row ->
        SearchKeyRow {
            row.forEachIndexed { keyIndex, character ->
                val keyModifier = if (rowIndex == 0 && keyIndex == 0) Modifier.focusRequester(firstKey) else Modifier
                SearchKey(
                    label = character.uppercase(),
                    onClick = { search.type(character.toString()) },
                    modifier = keyModifier.weight(1f),
                )
            }
        }
    }
    SearchActionKeys(search, firstKey)
}

@Composable
private fun SearchActionKeys(search: JarnSearchStore, firstKey: FocusRequester) {
    SearchKeyRow {
        SearchKey(
            label = stringResource(R.string.jarnfe_search_key_space),
            onClick = { search.type(" ") },
            modifier = Modifier.weight(WIDE_KEY_WEIGHT),
        )
        SearchKey(
            label = stringResource(R.string.jarnfe_search_key_delete),
            onClick = search::deleteLast,
            modifier = Modifier.weight(1f),
        )
        SearchKey(
            label = stringResource(R.string.jarnfe_search_key_clear),
            onClick = {
                search.clearQuery()
                firstKey.requestFocus()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchKeyRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        content = content,
    )
}

@Composable
private fun SearchKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.radius.smShape
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .jarnInteractive(shape, source, onClick = onClick)
            .heightIn(min = SearchKeyMinHeight)
            .background(colors.raised, shape),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(label, JarnTheme.type.labelStrong, colors.textHigh, maxLines = 1)
    }
}
