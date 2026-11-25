package jr.brian.jarnfe.deck

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppsRoute
import jr.brian.jarnfe.canvas.CanvasHeaderControls
import jr.brian.jarnfe.canvas.CanvasHeaderEditBanner
import jr.brian.jarnfe.canvas.CanvasRoute
import jr.brian.jarnfe.dock.JarnDockRow
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibraryNowPanel
import jr.brian.jarnfe.library.NowShortcutControls
import jr.brian.jarnfe.library.nowShortcutGap
import jr.brian.jarnfe.search.JarnSearch
import jr.brian.jarnfe.search.JarnSearchDeck
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.settings.JarnSettingsControls
import jr.brian.jarnfe.settings.JarnSettingsScraperHost
import jr.brian.jarnfe.shell.JarnScreenFrame
import jr.brian.jarnfe.shell.JarnWordmark
import jr.brian.jarnfe.shell.deckBindings
import jr.brian.jarnfe.splash.JarnDeckSplash
import jr.brian.jarnfe.splash.jarnSplashFade
import jr.brian.jarnfe.transition.LocalJarnMarkHandoff
import jr.brian.jarnfe.wallpaper.JarnWallpaperBackdrop
import jr.brian.jarnfe.wallpaper.LocalJarnDisabledBacking

@StringRes
private fun DeckTab.labelRes(): Int = when (this) {
    DeckTab.Now -> R.string.jarnfe_tab_now
    DeckTab.Apps -> R.string.jarnfe_tab_apps
    DeckTab.Canvas -> R.string.jarnfe_tab_canvas
}

@Composable
internal fun DeckScreen(state: JarnSessionState) {
    JarnInputLayer(bindings = { deckBindings(state, JarnSession) }) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.jarnSplashFade()) {
                JarnWallpaperBackdrop(showWallpaper = true)
            }
            CompositionLocalProvider(LocalJarnDisabledBacking provides JarnTheme.colors.bg) {
                JarnScreenFrame(
                    header = { DeckHeader(state) },
                    background = Color.Transparent,
                ) { DeckBody(state) }
            }
            if (deckShowsSettings(state)) JarnSettingsScraperHost()
            JarnDeckSplash()
        }
    }
}

private fun deckShowsSearch(state: JarnSessionState) =
    state.stageRoute == StageRoute.Search && !JarnSearch.showingActions

private fun deckShowsSettings(state: JarnSessionState) =
    state.stageRoute == StageRoute.Settings && state.settingsOnDeck

private fun deckShowsTabs(state: JarnSessionState) =
    !deckShowsSearch(state) && !deckShowsSettings(state)

/** Search and Settings hide the tabs, so the Jarn label gets a row of its own there. */
@Composable
private fun DeckHeader(state: JarnSessionState) {
    val showsMark = LocalJarnMarkHandoff.current.follows
    when {
        deckShowsTabs(state) -> DeckTabStrip(state.deckTab, showsMark)
        showsMark -> DeckMarkRow()
    }
}

@Composable
private fun DeckMarkRow() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = JarnTheme.space.deckMargin, vertical = JarnTheme.space.md),
    ) { JarnWordmark() }
}

@Composable
private fun DeckBody(state: JarnSessionState) {
    when {
        deckShowsSearch(state) -> JarnSearchDeck()
        deckShowsSettings(state) -> JarnSettingsControls(state.settingsSection, JarnTheme.space.deckMargin)
        else -> Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                DeckTabContent(state)
            }
            JarnDockRow()
        }
    }
}

@Composable
internal fun DeckContent(state: JarnSessionState, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (deckShowsSearch(state)) {
            JarnSearchDeck()
        } else {
            DeckTabStrip(state.deckTab, showsMark = false)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DeckTabContent(state)
            }
        }
    }
}

/** Random and Last Played live in the header only on Now, opposite the tabs. */
internal fun DeckTab.showsNowShortcuts(): Boolean = this == DeckTab.Now

@Composable
private fun DeckTabStrip(selected: DeckTab, showsMark: Boolean) {
    val tabs = DeckTab.entries
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = JarnTheme.space.deckMargin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showsMark) JarnWordmark(Modifier.padding(end = JarnTheme.space.lg))
            JarnTabs(
                labels = tabs.map { stringResource(it.labelRes()) },
                selectedIndex = tabs.indexOf(selected),
                onSelect = { index -> JarnSession.update { copy(deckTab = tabs[index]) } },
                modifier = Modifier.weight(1f),
                showsSwitchMark = true,
            )
            if (selected == DeckTab.Canvas) CanvasHeaderControls()
            if (selected.showsNowShortcuts()) NowShortcutControls(Modifier.padding(start = nowShortcutGap(JarnTheme.space.md.value).dp))
        }
        if (selected == DeckTab.Canvas) CanvasHeaderEditBanner(Modifier.matchParentSize())
    }
}

@Composable
private fun DeckTabContent(state: JarnSessionState) {
    when (state.deckTab) {
        DeckTab.Now -> LibraryNowPanel(
            selection = JarnLibrary.nowSelection(state.stageRoute, state.librarySection),
        )
        DeckTab.Apps -> JarnAppsRoute()
        DeckTab.Canvas -> CanvasRoute(state)
    }
}
