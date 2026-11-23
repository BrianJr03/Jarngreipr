package jr.brian.jarnfe.search

import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute

/** Starts a search, scoped to the open system in a game list. Inside Search it returns to the keyboard. */
internal fun openJarnSearch(
    session: JarnSessionStore,
    library: JarnLibraryStore,
    search: JarnSearchStore = JarnSearch,
) {
    val route = session.state.stageRoute
    if (route != StageRoute.Search) {
        search.begin(scope = library.state.openSystemId.takeIf { route == StageRoute.GameList })
    }
    search.showKeyboard()
    session.update { openSearch() }
}

internal fun JarnBindingsBuilder.searchTabBindings(search: JarnSearchStore) {
    on(JarnAction.TabPrevious) { search.stepTab(-1) }
    on(JarnAction.TabNext) { search.stepTab(1) }
}

internal fun JarnBindingsBuilder.searchKeyboardBindings(
    session: JarnSessionStore,
    search: JarnSearchStore,
) {
    hint(JarnAction.Confirm, R.string.jarnfe_search_hint_type)
    on(JarnAction.Back, R.string.jarnfe_hint_back) { session.update { back() } }
    on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
        session.update { switchActiveScreen() }
    }
    if (search.query.isNotEmpty()) {
        on(JarnAction.Context, R.string.jarnfe_search_hint_delete) { search.deleteLast() }
    }
    if (!search.results.isEmpty) {
        on(JarnAction.Search, R.string.jarnfe_search_hint_results) { session.update { back() } }
        searchTabBindings(search)
    }
}
