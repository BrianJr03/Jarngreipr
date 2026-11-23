package jr.brian.jarnfe.launch

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class GameActionsPage { Actions, Emulators, EmulatorApps, Cores, Companions, CompanionLaunch }

private const val RETROARCH_PREFIX = "com.retroarch"

internal fun pageAfterEmulatorChoice(packageName: String): GameActionsPage =
    if (packageName.startsWith(RETROARCH_PREFIX)) GameActionsPage.Cores else GameActionsPage.Actions

internal fun pageFor(kind: JarnEmulatorChoiceKind): GameActionsPage = when (kind) {
    JarnEmulatorChoiceKind.Emulator -> GameActionsPage.Emulators
    JarnEmulatorChoiceKind.RetroArchCore -> GameActionsPage.Cores
}

/** Shared by the Stage and Deck windows, which live in one process. */
internal object JarnGameActions {
    var controller by mutableStateOf<JarnGameActionsController?>(null)
    var gameId by mutableStateOf<String?>(null)
    var page by mutableStateOf(GameActionsPage.Actions)
    var setup by mutableStateOf<JarnGameSetup?>(null)
    var apps by mutableStateOf<List<JarnAppOption>?>(null)
    var companionChoices by mutableStateOf<List<JarnAppOption>>(emptyList())
    var pendingFocus by mutableStateOf(false)
        private set

    fun focusActions() {
        page = GameActionsPage.Actions
        pendingFocus = true
    }

    fun focusPage(target: GameActionsPage) {
        page = target
        pendingFocus = true
    }

    fun focusHandled() {
        pendingFocus = false
    }
}

/** Every choice is saved only. Launching always takes a separate press of Play. */
internal class JarnGameActionsController(
    private val activity: ComponentActivity,
    private val data: JarnLibraryData,
    private val options: JarnGameOptions,
) {
    private val state = JarnGameActions
    private var loadJob: Job? = null

    fun show(game: JarnLibraryGame) {
        if (state.gameId == game.id) {
            if (state.setup == null) reload(game)
            return
        }
        state.gameId = game.id
        state.page = GameActionsPage.Actions
        state.setup = null
        state.companionChoices = emptyList()
        reload(game)
    }

    private fun reload(game: JarnLibraryGame) {
        loadJob?.cancel()
        loadJob = activity.lifecycleScope.launch {
            val setup = runCatching { ref(game)?.let { options.load(it, game.id) } }.getOrNull()
            if (state.gameId == game.id) state.setup = setup ?: JarnGameSetup()
        }
    }

    private suspend fun ref(game: JarnLibraryGame): JarnGameRef? {
        val base = data.findGame(game.id)?.toGameRef(game.artPath) ?: return null
        return runCatching { options.locate(base, game.id) }.getOrDefault(base)
    }

    private fun withRef(game: JarnLibraryGame, action: suspend (JarnGameRef) -> Unit) {
        activity.lifecycleScope.launch {
            val ref = ref(game)
            if (ref == null) JarnNotices.show(R.string.jarnfe_launch_missing_rom) else action(ref)
        }
    }

    fun requestChoice(game: JarnLibraryGame, kind: JarnEmulatorChoiceKind) {
        show(game)
        state.focusPage(pageFor(kind))
        JarnSession.update { openDeckNow() }
    }

    /** Never takes focus, because the game that just started owns the controller. */
    fun offerCompanions(game: JarnLibraryGame, packages: List<String>) {
        show(game)
        state.companionChoices = options.appOptions(packages)
        state.page = GameActionsPage.CompanionLaunch
        JarnSession.update { withDeckPanel(true) }
    }

    fun openPage(page: GameActionsPage) {
        state.focusPage(page)
        if (page == GameActionsPage.EmulatorApps || page == GameActionsPage.Companions) loadApps()
    }

    fun closePage() {
        state.companionChoices = emptyList()
        state.focusActions()
    }

    private fun loadApps() {
        if (state.apps != null) return
        activity.lifecycleScope.launch {
            state.apps = runCatching { options.installedApps() }.getOrDefault(emptyList())
        }
    }

    fun toggleFavorite(game: JarnLibraryGame) {
        val next = state.setup?.isFavorite?.not() ?: return
        activity.lifecycleScope.launch {
            val saved = runCatching { options.setFavorite(game.id, next) }.getOrDefault(false)
            if (saved && state.gameId == game.id) {
                state.setup = state.setup?.copy(isFavorite = next)
            } else if (!saved) {
                JarnNotices.show(R.string.jarnfe_game_favorite_failed)
            }
        }
    }

    fun chooseEmulator(game: JarnLibraryGame, option: JarnEmulatorOption) = withRef(game) { ref ->
        options.chooseEmulator(ref, option)
        afterChoice(game, pageAfterEmulatorChoice(option.packageName))
    }

    fun chooseCore(game: JarnLibraryGame, corePath: String) = withRef(game) { ref ->
        options.chooseCore(ref, corePath)
        afterChoice(game, GameActionsPage.Actions)
    }

    fun addCompanion(game: JarnLibraryGame, packageName: String) = withRef(game) { ref ->
        options.addCompanion(ref, packageName)
        afterChoice(game, GameActionsPage.Actions)
    }

    fun clearCompanions(game: JarnLibraryGame) = withRef(game) { ref ->
        options.clearCompanions(ref)
        afterChoice(game, GameActionsPage.Actions)
    }

    fun openCompanion(packageName: String) {
        runCatching { options.openCompanion(activity, packageName) }
            .onFailure { JarnNotices.show(R.string.jarnfe_launch_app_missing) }
        closePage()
    }

    private fun afterChoice(game: JarnLibraryGame, next: GameActionsPage) {
        state.setup = null
        reload(game)
        state.focusPage(next)
        if (next == GameActionsPage.Actions) JarnNotices.show(R.string.jarnfe_game_choice_saved)
    }
}
