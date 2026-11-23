package jr.brian.jarnfe.deck

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasRoute
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibraryNowPanel
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.JarnScreenFrame
import jr.brian.jarnfe.shell.deckBindings

@StringRes
private fun DeckTab.labelRes(): Int = when (this) {
    DeckTab.Now -> R.string.jarnfe_tab_now
    DeckTab.Browse -> R.string.jarnfe_tab_browse
    DeckTab.Canvas -> R.string.jarnfe_tab_canvas
}

@Composable
internal fun DeckScreen(state: JarnSessionState) {
    JarnInputLayer(bindings = { deckBindings(state, JarnSession) }) {
        JarnScreenFrame(
            header = { DeckTabStrip(state.deckTab) },
        ) { DeckTabContent(state) }
    }
}

@Composable
internal fun DeckContent(state: JarnSessionState, modifier: Modifier = Modifier) {
    Column(modifier) {
        DeckTabStrip(state.deckTab)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DeckTabContent(state)
        }
    }
}

@Composable
private fun DeckTabStrip(selected: DeckTab) {
    val tabs = DeckTab.entries
    JarnTabs(
        labels = tabs.map { stringResource(it.labelRes()) },
        selectedIndex = tabs.indexOf(selected),
        onSelect = { index -> JarnSession.update { copy(deckTab = tabs[index]) } },
        modifier = Modifier.padding(horizontal = JarnTheme.space.deckMargin),
    )
}

@Composable
private fun DeckTabContent(state: JarnSessionState) {
    when (state.deckTab) {
        DeckTab.Now -> LibraryNowPanel(
            selection = JarnLibrary.state.selection(state.stageRoute, state.librarySection),
            recent = JarnLibrary.state.recent.items,
        )
        DeckTab.Browse -> DeckTabPlaceholder(R.string.jarnfe_empty_browse_title, R.string.jarnfe_empty_browse_message)
        DeckTab.Canvas -> CanvasRoute(state)
    }
}

@Composable
private fun DeckTabPlaceholder(@StringRes titleRes: Int, @StringRes messageRes: Int) {
    JarnEmptyState(title = stringResource(titleRes), message = stringResource(messageRes))
}
