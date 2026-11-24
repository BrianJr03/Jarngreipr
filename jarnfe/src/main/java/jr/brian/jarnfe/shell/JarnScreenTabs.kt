package jr.brian.jarnfe.shell

import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnShoulderPair
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

/** A swap never moves a window, so the Stage window always sits on the top display. */
internal fun JarnSessionState.roleAt(pair: JarnShoulderPair): JarnScreen = when (pair) {
    JarnShoulderPair.Top -> roleOn(JarnScreen.Stage)
    JarnShoulderPair.Bottom -> roleOn(JarnScreen.Deck)
}

/** The role whose tabs [pair] steps from the shell. With one screen both pairs step Stage. */
internal fun JarnSessionState.tabTargetOf(pair: JarnShoulderPair): JarnScreen =
    if (deckDisplayAvailable) roleAt(pair) else JarnScreen.Stage

/** The pairs that reach [role]'s screen. With one screen both pairs do, so neither is wasted. */
internal fun JarnSessionState.shoulderPairsFor(role: JarnScreen): List<JarnShoulderPair> =
    JarnShoulderPair.entries.filter { tabTargetOf(it) == role }

/**
 * The tab strip [pair] steps, or null when there is nothing to step. Reaching the screen the
 * controller is not on also needs a live Deck window and no modal open on either screen. Now
 * follows the Stage selection, so a game action page in use there also blocks reaching Stage.
 */
internal fun JarnSessionState.tabStripAt(
    pair: JarnShoulderPair,
    searchActionsOnDeck: Boolean,
    anyModalOpen: Boolean,
    gameActionPageOpen: Boolean = false,
): JarnTabStrip? {
    val target = tabTargetOf(pair)
    if (target != inputScreen && !canReachAcross(anyModalOpen, gameActionPageOpen)) return null
    return when (target) {
        JarnScreen.Stage -> stageTabStrip()
        JarnScreen.Deck -> deckTabStrip(searchActionsOnDeck)
    }
}

private fun JarnSessionState.canReachAcross(anyModalOpen: Boolean, gameActionPageOpen: Boolean): Boolean {
    if (!settingsOnDeck || anyModalOpen) return false
    return !(gameActionPageOpen && activeScreen == JarnScreen.Deck && deckTab == DeckTab.Now)
}

/** Settings sections live with the controls, which move to the Deck whenever it has a window. */
private fun JarnSessionState.stageTabStrip(): JarnTabStrip? = when (stageRoute) {
    StageRoute.Library -> JarnTabStrip.LibrarySections
    StageRoute.GameList -> JarnTabStrip.GameListSystems
    StageRoute.Search -> JarnTabStrip.SearchTabs
    StageRoute.Settings -> JarnTabStrip.SettingsSections.takeUnless { settingsOnDeck }
    StageRoute.Systems -> null
}

private fun JarnSessionState.deckTabStrip(searchActionsOnDeck: Boolean): JarnTabStrip? = when {
    stageRoute == StageRoute.Settings -> JarnTabStrip.SettingsSections
    stageRoute == StageRoute.Search && !searchActionsOnDeck -> null
    else -> JarnTabStrip.DeckTabs
}

internal fun tabStripNow(
    pair: JarnShoulderPair,
    session: JarnSessionStore,
    library: JarnLibraryStore,
    search: JarnSearchStore,
    router: JarnInputRouter,
): JarnTabStrip? {
    val state = session.state
    val anyModalOpen = JarnScreen.entries.any(router::hasModalLayer)
    val nowShowsGame = library.state.selection(state.stageRoute, state.librarySection) is LibrarySelection.Game
    val gameActionPageOpen = nowShowsGame && JarnGameActions.page != GameActionsPage.Actions
    return state.tabStripAt(pair, search.showingActions, anyModalOpen, gameActionPageOpen)
}

/** The screen [pair] would step right now, for the hint bar. */
internal fun shellTabTarget(
    pair: JarnShoulderPair,
    session: JarnSessionStore,
    library: JarnLibraryStore,
    search: JarnSearchStore,
    router: JarnInputRouter,
): JarnScreen? {
    if (tabStripNow(pair, session, library, search, router) == null) return null
    return session.state.tabTargetOf(pair)
}

/** Only tab state changes, so the active screen and its focused item stay where they are. */
internal fun stepScreenTabs(
    pair: JarnShoulderPair,
    delta: Int,
    session: JarnSessionStore,
    library: JarnLibraryStore,
    search: JarnSearchStore,
    router: JarnInputRouter,
) {
    when (tabStripNow(pair, session, library, search, router)) {
        JarnTabStrip.LibrarySections -> session.update { stepLibrarySection(delta) }
        JarnTabStrip.GameListSystems -> library.update { stepOpenSystem(delta) }
        JarnTabStrip.SearchTabs -> search.stepTab(delta)
        JarnTabStrip.DeckTabs -> session.update { stepDeckTab(delta) }
        JarnTabStrip.SettingsSections -> session.update { stepSettingsSection(delta) }
        null -> Unit
    }
}
