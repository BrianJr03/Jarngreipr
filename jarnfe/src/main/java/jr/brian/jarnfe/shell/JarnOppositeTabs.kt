package jr.brian.jarnfe.shell

import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.search.JarnSearchStore
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute

internal enum class JarnTabStrip { LibrarySections, GameListSystems, SearchTabs, DeckTabs, SettingsSections }

/**
 * The tab strip on the screen the controller is not on, or null when there is nothing to step: a
 * single screen, a missing Deck window, a modal open on either screen, or a route without tabs.
 * Now follows the Stage selection, so a game action page in use there also blocks the step.
 */
internal fun JarnSessionState.oppositeTabStrip(
    searchActionsOnDeck: Boolean,
    anyModalOpen: Boolean,
    gameActionPageOpen: Boolean = false,
): JarnTabStrip? {
    if (!settingsOnDeck || anyModalOpen) return null
    if (gameActionPageOpen && activeScreen == JarnScreen.Deck && deckTab == DeckTab.Now) return null
    return when (oppositeScreen) {
        JarnScreen.Stage -> stageTabStrip()
        JarnScreen.Deck -> deckTabStrip(searchActionsOnDeck)
    }
}

private fun JarnSessionState.stageTabStrip(): JarnTabStrip? = when (stageRoute) {
    StageRoute.Library -> JarnTabStrip.LibrarySections
    StageRoute.GameList -> JarnTabStrip.GameListSystems
    StageRoute.Search -> JarnTabStrip.SearchTabs
    StageRoute.Settings, StageRoute.Systems -> null
}

private fun JarnSessionState.deckTabStrip(searchActionsOnDeck: Boolean): JarnTabStrip? = when {
    stageRoute == StageRoute.Settings -> JarnTabStrip.SettingsSections
    stageRoute == StageRoute.Search && !searchActionsOnDeck -> null
    else -> JarnTabStrip.DeckTabs
}

/** Only tab state changes, so the active screen and its focused item stay where they are. */
internal fun stepOppositeTabs(
    delta: Int,
    session: JarnSessionStore,
    library: JarnLibraryStore,
    search: JarnSearchStore,
    router: JarnInputRouter,
) {
    val state = session.state
    val anyModalOpen = JarnScreen.entries.any(router::hasModalLayer)
    val nowShowsGame = library.state.selection(state.stageRoute, state.librarySection) is LibrarySelection.Game
    val gameActionPageOpen = nowShowsGame && JarnGameActions.page != GameActionsPage.Actions
    when (state.oppositeTabStrip(search.showingActions, anyModalOpen, gameActionPageOpen)) {
        JarnTabStrip.LibrarySections -> session.update { stepLibrarySection(delta) }
        JarnTabStrip.GameListSystems -> library.update { stepOpenSystem(delta) }
        JarnTabStrip.SearchTabs -> search.stepTab(delta)
        JarnTabStrip.DeckTabs -> session.update { stepDeckTab(delta) }
        JarnTabStrip.SettingsSections -> session.update { stepSettingsSection(delta) }
        null -> Unit
    }
}
