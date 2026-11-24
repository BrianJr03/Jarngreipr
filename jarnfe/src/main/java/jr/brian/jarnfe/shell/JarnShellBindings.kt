package jr.brian.jarnfe.shell

import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindings
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.jarnBindings
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.library.LibraryConfirm
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.search.JarnSearch
import jr.brian.jarnfe.search.searchTabBindings
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.systems.JarnSystems

/**
 * Back is bound here without a label so every press goes through the session's one-layer rule,
 * and the root stays silent. Screens add the visible hint only when Back can do something.
 */
internal fun jarnGlobalBindings(
    session: JarnSessionStore,
    onSearch: (() -> Unit)? = null,
    onSwap: (() -> Unit)? = null,
    onOppositeTab: ((Int) -> Unit)? = null,
    deckMissing: Boolean = false,
    onBackAtRoot: (() -> Unit)? = null,
): JarnBindings = jarnBindings {
    val selectLabel = if (deckMissing) R.string.jarnfe_hint_restore_bottom else R.string.jarnfe_hint_switch_screen
    on(JarnAction.SwitchScreen, selectLabel) {
        session.update { switchActiveScreen() }
    }
    on(JarnAction.Back) {
        if (session.state.canGoBack) session.update { back() } else onBackAtRoot?.invoke()
    }
    onSearch?.let { on(JarnAction.Search, R.string.jarnfe_hint_search, onPress = it) }
    onSwap?.let { on(JarnAction.Swap, onPress = it) }
    onOppositeTab?.let { step ->
        on(JarnAction.OppositeTabPrevious) { step(-1) }
        on(JarnAction.OppositeTabNext) { step(1) }
    }
}

internal fun JarnBindingsBuilder.stageBindings(
    state: JarnSessionState,
    session: JarnSessionStore,
    library: JarnLibraryStore,
) {
    if (state.stageRoute == StageRoute.Settings && state.settingsOnDeck) {
        on(JarnAction.Confirm, R.string.jarnfe_settings_hint_edit) {
            session.update { activate(JarnScreen.Deck) }
        }
    } else if (library.bannerHasFocus(state)) {
        hint(JarnAction.Confirm, R.string.jarnfe_hint_play)
    } else {
        confirmHint(library.state.confirmFor(state.stageRoute, state.librarySection, library.canPlay))
    }
    backHint(state, session)
    gameActionsBinding(state, session, library)
    manageSystemBinding(state, session, library)
    if (state.stageRoute != StageRoute.Settings) {
        on(JarnAction.Menu, R.string.jarnfe_route_settings) {
            session.update { openSettings() }
        }
    }
    when (state.stageRoute) {
        StageRoute.Library -> {
            on(JarnAction.TabPrevious) { session.update { stepLibrarySection(-1) } }
            on(JarnAction.TabNext) { session.update { stepLibrarySection(1) } }
        }
        StageRoute.GameList -> {
            on(JarnAction.TabPrevious) { library.update { stepOpenSystem(-1) } }
            on(JarnAction.TabNext) { library.update { stepOpenSystem(1) } }
        }
        StageRoute.Search -> searchTabBindings(JarnSearch)
        StageRoute.Settings -> settingsTabBindings(session)
        StageRoute.Systems -> Unit
    }
}

private fun JarnBindingsBuilder.gameActionsBinding(
    state: JarnSessionState,
    session: JarnSessionStore,
    library: JarnLibraryStore,
) {
    val selection = library.state.selection(state.stageRoute, state.librarySection)
    if (selection !is LibrarySelection.Game || JarnGameActions.controller == null) return
    on(JarnAction.Context, R.string.jarnfe_hint_context) {
        if (state.stageRoute == StageRoute.Search) JarnSearch.showActions()
        JarnGameActions.focusActions()
        session.update { openDeckNow() }
    }
}

private fun JarnLibraryStore.bannerHasFocus(state: JarnSessionState) =
    heroFocused && canPlay && state.stageRoute == StageRoute.Library

private fun JarnBindingsBuilder.manageSystemBinding(
    state: JarnSessionState,
    session: JarnSessionStore,
    library: JarnLibraryStore,
) {
    if (library.bannerHasFocus(state)) return
    val selection = library.state.selection(state.stageRoute, state.librarySection)
    if (selection !is LibrarySelection.System || JarnSystems.manager == null) return
    on(JarnAction.Context, R.string.jarnfe_systems_manage_one) {
        JarnSystems.openAt(selection.system.id)
        session.update { openSystems() }
    }
}

private fun JarnBindingsBuilder.confirmHint(confirm: LibraryConfirm) {
    when (confirm) {
        LibraryConfirm.Open -> hint(JarnAction.Confirm, R.string.jarnfe_hint_open)
        LibraryConfirm.Play -> hint(JarnAction.Confirm, R.string.jarnfe_hint_play)
        LibraryConfirm.Generic -> hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
        LibraryConfirm.None -> Unit
    }
}

internal fun JarnBindingsBuilder.deckBindings(state: JarnSessionState, session: JarnSessionStore) {
    backHint(state, session)
    if (state.stageRoute == StageRoute.Settings && state.settingsOnDeck) {
        hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
        settingsTabBindings(session)
    } else {
        deckTabBindings(session)
    }
}

private fun JarnBindingsBuilder.settingsTabBindings(session: JarnSessionStore) {
    on(JarnAction.TabPrevious) { session.update { stepSettingsSection(-1) } }
    on(JarnAction.TabNext) { session.update { stepSettingsSection(1) } }
}

internal fun JarnBindingsBuilder.deckPanelBindings(session: JarnSessionStore) {
    on(JarnAction.Back, R.string.jarnfe_hint_back) { session.update { back() } }
    on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
        session.update { switchActiveScreen() }
    }
    deckTabBindings(session)
}

private fun JarnBindingsBuilder.deckTabBindings(session: JarnSessionStore) {
    on(JarnAction.TabPrevious) { session.update { stepDeckTab(-1) } }
    on(JarnAction.TabNext) { session.update { stepDeckTab(1) } }
}

private fun JarnBindingsBuilder.backHint(state: JarnSessionState, session: JarnSessionStore) {
    if (state.canGoBack) {
        on(JarnAction.Back, R.string.jarnfe_hint_back) { session.update { back() } }
    }
}
