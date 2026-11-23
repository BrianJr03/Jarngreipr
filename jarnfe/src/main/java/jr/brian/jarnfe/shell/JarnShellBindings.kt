package jr.brian.jarnfe.shell

import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindings
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.jarnBindings
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.library.LibraryConfirm
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute

/**
 * Back is bound here without a label so every press goes through the session's one-layer rule,
 * and the root stays silent. Screens add the visible hint only when Back can do something.
 */
internal fun jarnGlobalBindings(session: JarnSessionStore): JarnBindings = jarnBindings {
    on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
        session.update { switchActiveScreen() }
    }
    on(JarnAction.Back) { session.update { back() } }
}

internal fun JarnBindingsBuilder.stageBindings(
    state: JarnSessionState,
    session: JarnSessionStore,
    library: JarnLibraryStore,
) {
    confirmHint(library.state.confirmFor(state.stageRoute, state.librarySection, library.canPlay))
    backHint(state, session)
    if (state.stageRoute != StageRoute.Settings) {
        on(JarnAction.Menu, R.string.jarnfe_route_settings) {
            session.update { openRoute(StageRoute.Settings) }
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
        StageRoute.Settings -> Unit
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
    deckTabBindings(session)
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
