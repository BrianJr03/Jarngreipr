package jr.brian.jarnfe.stage

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasStagePreview
import jr.brian.jarnfe.deck.DeckContent
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibraryGameList
import jr.brian.jarnfe.library.LibraryGameListTitle
import jr.brian.jarnfe.library.LibraryHome
import jr.brian.jarnfe.library.LibraryOrphanGuard
import jr.brian.jarnfe.library.LibrarySetupPanel
import jr.brian.jarnfe.library.jarnLibrarySetupOrNull
import jr.brian.jarnfe.library.librarySetupPrompt
import jr.brian.jarnfe.rss.JarnRssStageHost
import jr.brian.jarnfe.search.JarnSearchResultsPanel
import jr.brian.jarnfe.search.JarnSearchTabs
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.settings.JarnSettingsPreview
import jr.brian.jarnfe.settings.JarnSettingsSingleScreen
import jr.brian.jarnfe.shell.JarnNoticeHost
import jr.brian.jarnfe.shell.JarnScreenFrame
import jr.brian.jarnfe.shell.JarnStatusReadout
import jr.brian.jarnfe.shell.deckPanelBindings
import jr.brian.jarnfe.shell.stageBindings
import jr.brian.jarnfe.splash.JarnSplash
import jr.brian.jarnfe.splash.JarnStageSplash
import jr.brian.jarnfe.splash.jarnSplashFade
import jr.brian.jarnfe.splash.jarnSplashMark
import jr.brian.jarnfe.wallpaper.JarnWallpaperBackdrop
import jr.brian.jarnfe.systems.JarnSystems
import jr.brian.jarnfe.systems.JarnSystemsRoute

@StringRes
private fun LibrarySection.labelRes(): Int = when (this) {
    LibrarySection.Systems -> R.string.jarnfe_section_systems
    LibrarySection.Favorites -> R.string.jarnfe_section_favorites
    LibrarySection.Recent -> R.string.jarnfe_section_recent
}

@Composable
internal fun StageScreen(state: JarnSessionState) {
    LaunchedEffect(JarnLibrary.source) { JarnLibrary.observe() }
    LibraryOrphanGuard(JarnLibrary.state.gameListIsOrphaned(state.stageRoute)) {
        JarnSession.update { openRoute(StageRoute.Library) }
    }
    val reducedMotion = JarnTheme.motion.reduced
    LaunchedEffect(Unit) { JarnSplash.play(reducedMotion) }
    JarnInputLayer(bindings = { stageBindings(state, JarnSession, JarnLibrary) }) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.jarnSplashFade()) {
                JarnWallpaperBackdrop(showWallpaper = true)
            }
            JarnScreenFrame(
                header = { StageHeader(state) },
                background = Color.Transparent,
            ) {
                StageRouteContent(state)
                JarnRssStageHost(state)
            }
            CanvasStagePreview(state)
            SingleScreenDeckPanel(state)
            JarnNoticeHost()
            JarnStageSplash()
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
        JarnText(
            text = stringResource(R.string.jarnfe_wordmark),
            style = JarnTheme.type.heading,
            color = colors.textHigh,
            modifier = Modifier.jarnSplashMark(),
            maxLines = 1,
        )
        Box(Modifier.weight(1f).jarnSplashFade()) { StageHeaderTitle(state) }
        Box(Modifier.jarnSplashFade()) { JarnStatusReadout() }
    }
}

@Composable
private fun StageHeaderTitle(state: JarnSessionState) {
    when (state.stageRoute) {
        StageRoute.Library -> LibrarySectionTabs(state.librarySection)
        StageRoute.GameList -> LibraryGameListTitle(JarnLibrary.state)
        StageRoute.Settings -> StageRouteTitle(R.string.jarnfe_route_settings)
        StageRoute.Search -> JarnSearchTabs()
        StageRoute.Systems -> StageRouteTitle(R.string.jarnfe_systems_title)
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
        StageRoute.Library -> StageLibrary(state)
        StageRoute.GameList -> LibraryGameList(JarnLibrary)
        StageRoute.Settings -> StageSettings(state)
        StageRoute.Search -> JarnSearchResultsPanel(state)
        StageRoute.Systems -> StageSystems()
    }
}

@Composable
private fun StageLibrary(state: JarnSessionState) {
    val context = LocalContext.current
    val setup = remember(context) { context.jarnLibrarySetupOrNull() }
    val setupState = setup?.state?.collectAsState()?.value
    val prompt = librarySetupPrompt(setupState, JarnLibrary.state.systems)
    if (prompt != null) {
        LibrarySetupPanel(prompt)
    } else {
        LibraryHome(
            section = state.librarySection,
            library = JarnLibrary,
            onOpenSystem = ::openSystem,
            emptyActions = { LibraryRouteLinks(showDeckPanelLink = !state.deckDisplayAvailable) },
        )
    }
}

@Composable
private fun StageSettings(state: JarnSessionState) {
    if (state.settingsOnDeck) {
        JarnSettingsPreview()
    } else {
        JarnSettingsSingleScreen(state.settingsSection)
    }
}

@Composable
private fun StageSystems() {
    val manager = JarnSystems.manager
    if (manager != null) {
        JarnSystemsRoute(manager, onExit = { JarnSession.update { back() } })
    } else {
        StageSubRoutePlaceholder(
            R.string.jarnfe_systems_empty_title,
            R.string.jarnfe_systems_empty_message,
        )
    }
}

private fun openSystem(systemId: String) {
    JarnLibrary.update { openSystem(systemId) }
    JarnSession.update { openRoute(StageRoute.GameList) }
}

@Composable
private fun LibraryRouteLinks(showDeckPanelLink: Boolean) {
    val firstLink = remember { FocusRequester() }
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(Unit) { if (!tabsFocus.hasFocus) firstLink.requestFocus() }
    Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
        StageLinkButton(R.string.jarnfe_route_settings, Modifier.focusRequester(firstLink)) {
            JarnSession.update { openSettings() }
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
private fun DeckPanelFocusReturn(panelOpen: Boolean) {
    val focusManager = LocalFocusManager.current
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(panelOpen) {
        if (wasOpen && !panelOpen) {
            focusManager.clearFocus(force = true)
            withFrameNanos { }
            JarnLibrary.requestStageRefocus()
        }
        wasOpen = panelOpen
    }
}

@Composable
private fun SingleScreenDeckPanel(state: JarnSessionState) {
    DeckPanelFocusReturn(state.deckPanelOpen)
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
