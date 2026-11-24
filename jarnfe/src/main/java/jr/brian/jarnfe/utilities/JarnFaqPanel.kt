package jr.brian.jarnfe.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.tools.ToolsInitialFocus

@Composable
internal fun JarnFaqPanel(utilities: JarnUtilities, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val sections = remember(utilities) { utilities.faqSections() }
    var sectionIndex by remember { mutableStateOf<Int?>(null) }
    var lastSection by remember { mutableStateOf(0) }
    var entryIndex by remember { mutableStateOf<Int?>(null) }
    var lastEntry by remember { mutableStateOf(0) }
    val section = sectionIndex?.let(sections::getOrNull)
    val entry = entryIndex?.let { section?.entries?.getOrNull(it) }
    when {
        entry != null -> UtilityReader(
            title = entry.question,
            body = entry.answer,
            snippets = entry.snippets,
            onBack = { entryIndex = null },
            modifier = modifier,
        )
        section != null -> FaqList(
            title = section.title,
            labels = section.entries.map { FaqRowLabel(it.question, null) },
            focusedIndex = lastEntry,
            onOpen = {
                lastEntry = it
                entryIndex = it
            },
            onBack = { sectionIndex = null },
            modifier = modifier,
        )
        else -> FaqList(
            title = androidx.compose.ui.res.stringResource(R.string.jarnfe_utilities_faq),
            labels = sections.map {
                FaqRowLabel(
                    it.title,
                    pluralStringResource(R.plurals.jarnfe_utilities_faq_count, it.entries.size, it.entries.size),
                )
            },
            focusedIndex = lastSection,
            onOpen = {
                lastSection = it
                lastEntry = 0
                sectionIndex = it
            },
            onBack = onClose,
            modifier = modifier,
        )
    }
}

private data class FaqRowLabel(val label: String, val supporting: String?)

@Composable
private fun FaqList(
    title: String,
    labels: List<FaqRowLabel>,
    focusedIndex: Int,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = remember { FocusRequester() }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = focusedIndex)
    ToolsInitialFocus(target)
    UtilityLayer(onBack = onBack, bindings = { hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm) }) {
        JarnRevealLazyItems(list) {
            LazyColumn(
                state = list,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(JarnTheme.space.lg),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
            ) {
                item(key = "title") { UtilityTitle(title) }
                itemsIndexed(labels, key = { index, _ -> index }) { index, row ->
                    JarnNavigateRow(
                        label = row.label,
                        supporting = row.supporting,
                        onClick = { onOpen(index) },
                        modifier = if (index == focusedIndex) Modifier.focusRequester(target) else Modifier,
                    )
                }
            }
        }
    }
}
