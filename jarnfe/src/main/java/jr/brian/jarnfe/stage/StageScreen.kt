package jr.brian.jarnfe.stage

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.deck.DeckContent
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.shell.JarnScreenFrame
import jr.brian.jarnfe.shell.JarnStatusReadout
import jr.brian.jarnfe.shell.deckPanelBindings
import jr.brian.jarnfe.shell.stageBindings

@StringRes
private fun LibrarySection.labelRes(): Int = when (this) {
    LibrarySection.Systems -> R.string.jarnfe_section_systems
    LibrarySection.Favorites -> R.string.jarnfe_section_favorites
    LibrarySection.Recent -> R.string.jarnfe_section_recent
}

@Composable
internal fun StageScreen(state: JarnSessionState) {
    JarnInputLayer(bindings = { stageBindings(state, JarnSession) }) {
        Box(Modifier.fillMaxSize()) {
            JarnScreenFrame(
                active = state.isLit(JarnScreen.Stage),
                header = { StageHeader(state) },
            ) { StageRouteContent(state) }
            SingleScreenDeckPanel(state)
        }
    }
}

@Composable
private fun StageHeader(state: JarnSessionState) {
    val colors = JarnTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = JarnTheme.space.stageMargin),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(stringResource(R.string.jarnfe_wordmark), JarnTheme.type.heading, colors.textHigh, maxLines = 1)
        Box(Modifier.weight(1f)) { StageHeaderTitle(state) }
        JarnStatusReadout()
    }
}

@Composable
private fun StageHeaderTitle(state: JarnSessionState) {
    when (state.stageRoute) {
        StageRoute.Library -> LibrarySectionTabs(state.librarySection)
        StageRoute.GameList -> StageRouteTitle(R.string.jarnfe_route_game_list)
        StageRoute.Settings -> StageRouteTitle(R.string.jarnfe_route_settings)
    }
}

@Composable
private fun StageRouteTitle(@StringRes titleRes: Int) {
    JarnText(
        text = stringResource(titleRes),
        style = JarnTheme.type.label,
        color = JarnTheme.colors.textMid,
        modifier = Modifier.padding(vertical = JarnTheme.space.xl),
        maxLines = 1,
    )
}

@Composable
private fun LibrarySectionTabs(selected: LibrarySection) {
    val sections = LibrarySection.entries
    JarnTabs(
        labels = sections.map { stringResource(it.labelRes()) },
        selectedIndex = sections.indexOf(selected),
        onSelect = { index -> JarnSession.update { copy(librarySection = sections[index]) } },
    )
}

@Composable
private fun StageRouteContent(state: JarnSessionState) {
    when (state.stageRoute) {
        StageRoute.Library -> LibraryPlaceholder(state)
        StageRoute.GameList -> StageSubRoutePlaceholder(
            R.string.jarnfe_empty_games_title,
            R.string.jarnfe_empty_games_message,
        )
        StageRoute.Settings -> StageSubRoutePlaceholder(
            R.string.jarnfe_empty_settings_title,
            R.string.jarnfe_empty_settings_message,
        )
    }
}

@Composable
private fun LibraryPlaceholder(state: JarnSessionState) {
    val (titleRes, messageRes) = when (state.librarySection) {
        LibrarySection.Systems -> R.string.jarnfe_empty_systems_title to R.string.jarnfe_empty_systems_message
        LibrarySection.Favorites -> R.string.jarnfe_empty_favorites_title to R.string.jarnfe_empty_favorites_message
        LibrarySection.Recent -> R.string.jarnfe_empty_recent_title to R.string.jarnfe_empty_recent_message
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        JarnEmptyState(title = stringResource(titleRes), message = stringResource(messageRes))
        LibraryRouteLinks(showDeckPanelLink = !state.deckDisplayAvailable)
    }
}

@Composable
private fun LibraryRouteLinks(showDeckPanelLink: Boolean) {
    val firstLink = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstLink.requestFocus() }
    Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
        StageLinkButton(R.string.jarnfe_route_game_list, Modifier.focusRequester(firstLink)) {
            JarnSession.update { openRoute(StageRoute.GameList) }
        }
        StageLinkButton(R.string.jarnfe_route_settings) {
            JarnSession.update { openRoute(StageRoute.Settings) }
        }
        if (showDeckPanelLink) {
            StageLinkButton(R.string.jarnfe_open_deck) { JarnSession.update { withDeckPanel(true) } }
        }
    }
}

@Composable
private fun StageLinkButton(
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    JarnButton(
        label = stringResource(labelRes),
        onClick = onClick,
        modifier = modifier,
        variant = JarnButtonVariant.Secondary,
    )
}

@Composable
private fun StageSubRoutePlaceholder(@StringRes titleRes: Int, @StringRes messageRes: Int) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(titleRes) { focusManager.moveFocus(FocusDirection.Next) }
    JarnEmptyState(
        title = stringResource(titleRes),
        message = stringResource(messageRes),
        actionLabel = stringResource(R.string.jarnfe_back_to_library),
        onAction = { JarnSession.update { openRoute(StageRoute.Library) } },
    )
}

@Composable
private fun SingleScreenDeckPanel(state: JarnSessionState) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        enabled = state.deckPanelOpen,
        bindings = { deckPanelBindings(JarnSession) },
    ) {
        JarnSheet(
            visible = state.deckPanelOpen,
            onDismiss = { JarnSession.update { withDeckPanel(false) } },
            title = stringResource(R.string.jarnfe_deck_title),
        ) {
            DeckContent(state, Modifier.fillMaxWidth().weight(1f, fill = false))
        }
    }
}
