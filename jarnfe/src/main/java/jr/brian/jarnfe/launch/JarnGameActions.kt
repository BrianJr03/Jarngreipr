package jr.brian.jarnfe.launch

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import jr.brian.frontend.library.art.ArtChoice
import jr.brian.frontend.library.art.ArtScreen
import jr.brian.frontend.library.art.GameArtChoices
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.media.MediaKind
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal enum class GameActionsPage {
    Actions,
    Emulators,
    EmulatorApps,
    Cores,
    Companions,
    CompanionLaunch,
    EmulatorScope,
    EmulatorConfirm,
    Edit,
    EditArt,
    ArtScope,
    ArtConfirm,
}

private const val RETROARCH_PREFIX = "com.retroarch"

internal fun pageAfterEmulatorChoice(packageName: String): GameActionsPage =
    if (packageName.startsWith(RETROARCH_PREFIX)) GameActionsPage.Cores else GameActionsPage.EmulatorScope

/** B goes back exactly one layer. */
internal fun pageBefore(
    page: GameActionsPage,
    pickedEmulator: Boolean,
    pickedCore: Boolean,
): GameActionsPage = when (page) {
    GameActionsPage.EmulatorApps -> GameActionsPage.Emulators
    GameActionsPage.Cores -> if (pickedEmulator) GameActionsPage.Emulators else GameActionsPage.Actions
    GameActionsPage.EmulatorScope -> if (pickedCore) GameActionsPage.Cores else GameActionsPage.Emulators
    GameActionsPage.EmulatorConfirm -> GameActionsPage.EmulatorScope
    GameActionsPage.EditArt -> GameActionsPage.Edit
    GameActionsPage.ArtScope -> GameActionsPage.EditArt
    GameActionsPage.ArtConfirm -> GameActionsPage.ArtScope
    else -> GameActionsPage.Actions
}

/** The companion offer is a sheet of its own, so a game page keeps its actions under it. */
internal fun GameActionsPage.showsOnGamePage(): Boolean =
    this != GameActionsPage.Actions && this != GameActionsPage.CompanionLaunch

/** Shown wherever the Deck is, since a launch can start from any part of Jarn. */
internal fun companionPickerShown(
    page: GameActionsPage,
    choices: List<JarnAppOption>,
    hasController: Boolean,
): Boolean = hasController && page == GameActionsPage.CompanionLaunch && choices.isNotEmpty()

internal fun isArtAvailable(choice: ArtChoice, kinds: Set<MediaKind>): Boolean =
    choice.kind == null || choice.kind in kinds

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
    var pendingEmulator by mutableStateOf<JarnEmulatorOption?>(null)
    var pendingCores by mutableStateOf<List<JarnCoreOption>?>(null)
    var pendingCore by mutableStateOf<String?>(null)
    var replaceCount by mutableIntStateOf(0)
    var artKinds by mutableStateOf<Set<MediaKind>?>(null)
    var artScreen by mutableStateOf(ArtScreen.Top)
    var pendingArt by mutableStateOf<ArtChoice?>(null)
    var pendingFocus by mutableStateOf(false)
        private set

    fun clearPending() {
        pendingEmulator = null
        pendingCores = null
        pendingCore = null
        replaceCount = 0
        pendingArt = null
    }

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

    val artChoices: StateFlow<GameArtChoices> = data.artChoices

    /** An open companion offer outlives a switch of game, since Now can move on while a game starts. */
    fun show(game: JarnLibraryGame) {
        if (state.gameId == game.id) {
            if (state.setup == null) reload(game)
            return
        }
        state.gameId = game.id
        if (state.page != GameActionsPage.CompanionLaunch) {
            state.page = GameActionsPage.Actions
            state.companionChoices = emptyList()
        }
        state.setup = null
        state.artKinds = null
        state.clearPending()
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
        val base = data.findGame(game.id)?.toGameRef(game.defaultArtPath) ?: return null
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

    /** Leaves focus where it is, since no game page drew the offer. */
    fun dismissCompanions() {
        if (state.page != GameActionsPage.CompanionLaunch) return
        state.companionChoices = emptyList()
        state.page = GameActionsPage.Actions
        JarnSession.update { withDeckPanel(false) }
    }

    fun openPage(page: GameActionsPage) {
        state.focusPage(page)
        if (page == GameActionsPage.EmulatorApps || page == GameActionsPage.Companions) loadApps()
    }

    fun closePage() {
        state.companionChoices = emptyList()
        state.clearPending()
        state.focusActions()
    }

    fun back() {
        val current = state.page
        val previous = pageBefore(current, state.pendingEmulator != null, state.pendingCore != null)
        if (previous == GameActionsPage.Actions) return closePage()
        when (current) {
            GameActionsPage.Cores -> {
                state.pendingEmulator = null
                state.pendingCores = null
            }
            GameActionsPage.EmulatorScope -> if (state.pendingCore != null) {
                state.pendingCore = null
            } else {
                state.pendingEmulator = null
            }
            GameActionsPage.ArtScope -> state.pendingArt = null
            else -> Unit
        }
        state.replaceCount = 0
        state.focusPage(previous)
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

    /** Held until the user says where it applies, so backing out changes nothing. */
    fun chooseEmulator(game: JarnLibraryGame, option: JarnEmulatorOption) {
        state.pendingEmulator = option
        state.pendingCore = null
        val next = pageAfterEmulatorChoice(option.packageName)
        if (next == GameActionsPage.Cores) loadPendingCores(game)
        state.focusPage(next)
    }

    private fun loadPendingCores(game: JarnLibraryGame) {
        state.pendingCores = null
        activity.lifecycleScope.launch {
            val cores = runCatching { options.retroArchCores() }.getOrDefault(emptyList())
            if (state.gameId == game.id && state.pendingEmulator != null) state.pendingCores = cores
        }
    }

    fun chooseCore(corePath: String) {
        state.pendingCore = corePath
        state.focusPage(GameActionsPage.EmulatorScope)
    }

    fun applyEmulatorToGame(game: JarnLibraryGame) = withRef(game) { ref ->
        state.pendingEmulator?.let { options.chooseEmulator(ref, it) }
        state.pendingCore?.let { options.chooseCore(ref, it) }
        state.clearPending()
        afterChoice(game, GameActionsPage.Actions)
    }

    private fun pendingSystemChoice(): JarnEmulatorChoice? {
        val packageName = state.pendingEmulator?.packageName ?: state.setup?.currentEmulator
        return packageName?.let {
            JarnEmulatorChoice(it, state.pendingEmulator?.command, state.pendingCore)
        }
    }

    /** Asks first when any game would lose its own choice. */
    fun requestEmulatorForSystem(game: JarnLibraryGame) {
        val choice = pendingSystemChoice() ?: return
        activity.lifecycleScope.launch {
            val conflicts = runCatching { options.systemChoiceConflicts(game.systemId, choice) }
                .getOrElse { return@launch JarnNotices.show(R.string.jarnfe_game_scope_failed) }
            if (state.gameId != game.id) return@launch
            if (conflicts > 0) {
                state.replaceCount = conflicts
                state.focusPage(GameActionsPage.EmulatorConfirm)
            } else {
                applyEmulatorToSystem(game)
            }
        }
    }

    fun applyEmulatorToSystem(game: JarnLibraryGame) {
        val choice = pendingSystemChoice() ?: return
        activity.lifecycleScope.launch {
            val saved = runCatching { options.applyToSystem(game.systemId, choice) }.isSuccess
            if (!saved) return@launch JarnNotices.show(R.string.jarnfe_game_scope_failed)
            state.clearPending()
            if (state.gameId == game.id) afterChoice(game, GameActionsPage.Actions)
        }
    }

    fun openEdit(game: JarnLibraryGame) {
        state.artKinds = null
        state.focusPage(GameActionsPage.Edit)
        activity.lifecycleScope.launch {
            val kinds = runCatching { data.availableArt(game.id) }.getOrDefault(emptySet())
            if (state.gameId == game.id) state.artKinds = kinds
        }
    }

    fun chooseArtScreen(screen: ArtScreen) {
        state.artScreen = screen
        state.focusPage(GameActionsPage.EditArt)
    }

    fun chooseArt(choice: ArtChoice) {
        state.pendingArt = choice
        state.focusPage(GameActionsPage.ArtScope)
    }

    fun applyArtToGame(game: JarnLibraryGame) {
        val choice = state.pendingArt ?: return
        if (!isArtAvailable(choice, state.artKinds.orEmpty())) return
        data.setGameArtChoice(state.artScreen, game.id, game.systemId, choice)
        finishArt()
    }

    fun requestArtForSystem(game: JarnLibraryGame) {
        val overrides = data.artChoices.value.gameOverrideCount(state.artScreen, game.systemId)
        if (overrides > 0) {
            state.replaceCount = overrides
            state.focusPage(GameActionsPage.ArtConfirm)
        } else {
            applyArtToSystem(game)
        }
    }

    fun applyArtToSystem(game: JarnLibraryGame) {
        val choice = state.pendingArt ?: return
        data.setSystemArtChoice(state.artScreen, game.systemId, choice)
        finishArt()
    }

    private fun finishArt() {
        state.pendingArt = null
        state.replaceCount = 0
        state.focusPage(GameActionsPage.Edit)
        JarnNotices.show(R.string.jarnfe_game_art_saved)
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
        dismissCompanions()
    }

    private fun afterChoice(game: JarnLibraryGame, next: GameActionsPage) {
        state.setup = null
        reload(game)
        state.focusPage(next)
        if (next == GameActionsPage.Actions) JarnNotices.show(R.string.jarnfe_game_choice_saved)
    }
}
