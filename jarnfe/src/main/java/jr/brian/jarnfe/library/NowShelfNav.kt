package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute

/** Where B goes from a game opened off a shelf cover. */
internal data class NowShelfReturn(val systemId: String, val gameId: String, val route: StageRoute)

internal fun NowShelfReturn.isLive(route: StageRoute, selection: LibrarySelection): Boolean =
    route == StageRoute.GameList && when (selection) {
        is LibrarySelection.Game -> selection.game.id == gameId
        is LibrarySelection.System -> selection.system.id == systemId
        LibrarySelection.None -> false
    }

internal fun LibrarySelection.sceneSystemId(): String? = when (this) {
    LibrarySelection.None -> null
    is LibrarySelection.System -> system.id
    is LibrarySelection.Game -> game.systemId
}

/** Moves between a system's shelf and its games. Shared by the Stage and Deck windows. */
internal class JarnNowShelfStore(
    private val session: JarnSessionStore,
    private val library: JarnLibraryStore,
) {
    var returnTo by mutableStateOf<NowShelfReturn?>(null)
        private set
    var pendingCoverId by mutableStateOf<String?>(null)
        private set
    private var launched: JarnLibraryGame? = null

    fun browse(systemId: String) {
        returnTo = null
        library.update { openSystem(systemId) }
        session.update {
            openRoute(StageRoute.GameList).copy(activeScreen = JarnScreen.Stage, deckPanelOpen = false)
        }
    }

    /** A leftover picker page is cleared so only this launch can open the game's actions. */
    fun play(game: JarnLibraryGame) {
        if (JarnGameActions.gameId == game.id && JarnGameActions.page != GameActionsPage.Actions) {
            JarnGameActions.clearPending()
            JarnGameActions.page = GameActionsPage.Actions
        }
        launched = game
        library.play(game)
    }

    fun openDetails(game: JarnLibraryGame) {
        showGame(game)
        JarnGameActions.focusActions()
        session.update { openDeckNow() }
    }

    /** A launch from a cover can need a picker or offer companions, which live on the game's page. */
    fun followLaunchedCover() {
        val game = launched ?: return
        if (JarnGameActions.gameId != game.id || JarnGameActions.page == GameActionsPage.Actions) return
        showGame(game)
    }

    private fun showGame(game: JarnLibraryGame) {
        launched = null
        val route = session.state.stageRoute
        returnTo = NowShelfReturn(game.systemId, game.id, route).takeIf { route != StageRoute.GameList }
        library.update { openSystem(game.systemId).focusOpenGame(game.id) }
        session.update { openRoute(StageRoute.GameList) }
    }

    fun returnToCover() {
        val target = returnTo ?: return
        returnTo = null
        pendingCoverId = target.gameId
        session.update { openRoute(target.route) }
    }

    fun coverRestored() {
        pendingCoverId = null
    }

    fun forgetIfLeft(route: StageRoute, selection: LibrarySelection) {
        if (returnTo?.isLive(route, selection) == false) returnTo = null
        if (selection !is LibrarySelection.System) pendingCoverId = null
        if ((selection as? LibrarySelection.System)?.system?.id != launched?.systemId) launched = null
    }
}

internal val JarnNowShelf = JarnNowShelfStore(JarnSession, JarnLibrary)

@Composable
internal fun NowShelfReturnLayer(selection: LibrarySelection, content: @Composable () -> Unit) {
    val route = JarnSession.state.stageRoute
    val target = JarnNowShelf.returnTo
    LaunchedEffect(route, selection.focusKey(), target) { JarnNowShelf.forgetIfLeft(route, selection) }
    JarnInputLayer(
        enabled = target != null && selection is LibrarySelection.Game,
        bindings = {
            on(JarnAction.Back, R.string.jarnfe_now_hint_back_to_system) { JarnNowShelf.returnToCover() }
        },
        content = content,
    )
}
