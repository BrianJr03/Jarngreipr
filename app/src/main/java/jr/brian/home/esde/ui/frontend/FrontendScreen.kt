package jr.brian.home.esde.ui.frontend

import jr.brian.home.esde.data.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.FrontendSelectionStateHolder
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.ui.RomGameLauncher
import jr.brian.home.esde.ui.frontend.settings.AddSystemsScreen
import jr.brian.home.esde.ui.frontend.settings.FrontendSettingsScreen
import jr.brian.home.esde.ui.frontend.settings.SystemCustomizationScreen
import jr.brian.home.esde.util.heroBackgroundPath
import jr.brian.home.esde.util.hiddenGameKey
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.model.rom.toGameInfo
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import android.view.KeyEvent as AndroidKeyEvent

private const val ANIM_ENTER_MS = 220
private const val ANIM_EXIT_MS = 180
private const val ANDROID_APPS_SYSTEM = "androidapps"

private fun Modifier.frontendControlKeys(
    overlayVisible: Boolean,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit
): Modifier = onPreviewKeyEvent { keyEvent ->
    if (overlayVisible) return@onPreviewKeyEvent false
    val code = keyEvent.nativeKeyEvent.keyCode
    if (code != AndroidKeyEvent.KEYCODE_BUTTON_Y &&
        code != AndroidKeyEvent.KEYCODE_BUTTON_START
    ) return@onPreviewKeyEvent false
    if (keyEvent.type == KeyEventType.KeyDown) {
        when (code) {
            AndroidKeyEvent.KEYCODE_BUTTON_Y -> onSearch()
            AndroidKeyEvent.KEYCODE_BUTTON_START -> onOpenSettings()
        }
    }
    true
}

@Composable
internal fun FrontendScreen(
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    mainViewModel: MainViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder,
    managers: ManagerContainer,
    romLauncher: RomGameLauncher,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onFinishImmediately: () -> Unit,
    onSignalGameLaunch: () -> Unit,
    onChangeFolder: (GameInfo) -> Unit
) {
    RomSearchExperimentalDialogIfNeeded()

    var isVisible by remember { mutableStateOf(false) }

    HandlePendingRomLaunch(
        romSearchStateHolder = romSearchStateHolder,
        romLauncher = romLauncher,
        appDisplayPreferenceManager = appDisplayPreferenceManager,
        onSignalGameLaunch = onSignalGameLaunch,
        onFinishImmediately = onFinishImmediately,
        onNothingPending = { isVisible = true }
    )

    LaunchedEffect(Unit) {
        viewModel.dismissSignal.collect {
            isVisible = false
            delay(ANIM_EXIT_MS.toLong())
            onFinishImmediately()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(ANIM_ENTER_MS)) +
                scaleIn(tween(ANIM_ENTER_MS), initialScale = 0.92f),
        exit = fadeOut(tween(ANIM_EXIT_MS)) +
                scaleOut(tween(ANIM_EXIT_MS), targetScale = 0.92f)
    ) {
        FrontendRouteHost(
            esdePrefs = esdePrefs,
            viewModel = viewModel,
            romSearchStateHolder = romSearchStateHolder,
            frontendSelectionStateHolder = frontendSelectionStateHolder,
            managers = managers,
            romLauncher = romLauncher,
            appDisplayPreferenceManager = appDisplayPreferenceManager,
            onChangeFolder = onChangeFolder
        )
    }
}

@Composable
private fun HandlePendingRomLaunch(
    romSearchStateHolder: RomSearchStateHolder,
    romLauncher: RomGameLauncher,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onSignalGameLaunch: () -> Unit,
    onFinishImmediately: () -> Unit,
    onNothingPending: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val pending = romSearchStateHolder.pendingRomToLaunch.value
        if (pending != null) {
            romSearchStateHolder.pendingRomToLaunch.value = null
            val game = pending.toGameInfo()
            romLauncher.launchGame(
                game,
                context,
                appDisplayPreferenceManager.getAppDisplayPreference(pending.key)
            )
            onSignalGameLaunch()
            onFinishImmediately()
        } else {
            onNothingPending()
        }
    }
}

@Composable
private fun FrontendRouteHost(
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder,
    managers: ManagerContainer,
    romLauncher: RomGameLauncher,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onChangeFolder: (GameInfo) -> Unit
) {
    val route by viewModel.currentRoute.collectAsStateWithLifecycle()
    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val esdeState by esdePrefs.state.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val forward = initialState is FrontendRoute.Systems &&
                    targetState is FrontendRoute.Games
            esdeState.frontendTransition.transform(forward, esdeState.frontendTransitionMs)
        },
        // Key on route TYPE so navigating between two Games routes (if that ever
        // happens) does not re-trigger the transition. Systems is a singleton.
        contentKey = { it is FrontendRoute.Games },
        label = "frontendRoute",
        // SystemsRoute's outer Box is transparent unless the focus background
        // is active, and during the Systems↔Games cross-fade both panels have
        // transparent regions. Without an opaque backer here the activity
        // window's underlying (system) wallpaper flashes through on scroll and
        // during transition. Leave it transparent only when the user has
        // explicitly opted into the ES-DE wallpaper as the frontend backdrop.
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (esdeState.romSearchUseWallpaper) Color.Transparent
                else OledBackgroundColor
            )
    ) { currentRoute ->
        // AnimatedContent keeps BOTH routes composed for the duration of the
        // transition. FocusableTileLayout retries requestFocus() a few times on
        // (re)composition, so the outgoing route can steal focus back from the
        // incoming one mid-transition. Block focus on the non-target route.
        //
        // The receiver's `transition` tracks EnterExitState: the incoming pane's
        // targetState is Visible, the outgoing pane's is PostExit.
        val isTarget = this.transition.targetState == EnterExitState.Visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = isTarget }
        ) {
            when (currentRoute) {
                is FrontendRoute.Games -> GamesRoute(
                    system = currentRoute.system,
                    allGames = allGames,
                    isLoading = isLoading,
                    esdeState = esdeState,
                    esdePrefs = esdePrefs,
                    viewModel = viewModel,
                    romSearchStateHolder = romSearchStateHolder,
                    frontendSelectionStateHolder = frontendSelectionStateHolder,
                    managers = managers,
                    romLauncher = romLauncher,
                    appDisplayPreferenceManager = appDisplayPreferenceManager,
                    onChangeFolder = onChangeFolder
                )

                is FrontendRoute.Systems -> SystemsRoute(
                    allGames = allGames,
                    isLoading = isLoading,
                    hiddenGames = esdeState.hiddenGames,
                    hiddenSystems = esdeState.hiddenSystems,
                    layout = esdeState.systemLayout,
                    useWallpaper = esdeState.romSearchUseWallpaper,
                    customizations = esdeState.systemCustomizations,
                    systemOrder = esdeState.systemOrder,
                    hintsVisible = esdeState.frontendHintsVisible,
                    esdePrefs = esdePrefs,
                    viewModel = viewModel,
                    romSearchStateHolder = romSearchStateHolder,
                    frontendSelectionStateHolder = frontendSelectionStateHolder
                )
            }
        }
    }
}

@Composable
private fun GamesRoute(
    system: String,
    allGames: List<GameInfo>,
    isLoading: Boolean,
    esdeState: jr.brian.home.esde.model.ESDEPrefsState,
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder,
    managers: ManagerContainer,
    romLauncher: RomGameLauncher,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onChangeFolder: (GameInfo) -> Unit
) {
    LaunchedEffect(system) {
        frontendSelectionStateHolder.selectSystem(system)
    }

    val filteredGames = rememberFilteredGames(
        allGames = allGames,
        hiddenGames = esdeState.hiddenGames,
        hideNoMetadata = esdeState.romSearchHideNoMetadata,
        hideNoImage = esdeState.romSearchHideNoImage,
        cardMediaType = esdeState.romSearchCardMediaType,
        forcedPlatform = system
    )

    LaunchedEffect(filteredGames) {
        filteredGames.firstOrNull()?.let { viewModel.updateFocusedGame(it) }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSystemFilter by remember { mutableStateOf(false) }
    var showAddSystems by remember { mutableStateOf(false) }
    // Any overlay drawn on top of the grid holds focus for its whole lifetime.
    // On open we drop focus explicitly so the grid stops receiving events; on
    // close the reset tick below re-seeds focus onto the tile the user was on
    // before the overlay opened.
    val overlayVisible = showSettingsDialog || showSystemFilter || showAddSystems

    val focusManager = LocalFocusManager.current
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) focusManager.clearFocus(force = true)
    }

    // Only bump on the true→false edge — bumping on open would race the
    // overlay's own focus request and sometimes leave focus on the grid.
    var overlayCloseTick by remember { mutableIntStateOf(0) }
    var previousOverlayVisible by remember { mutableStateOf(overlayVisible) }
    LaunchedEffect(overlayVisible) {
        if (previousOverlayVisible && !overlayVisible) overlayCloseTick++
        previousOverlayVisible = overlayVisible
    }

    val initialGameIndex = remember(filteredGames, system, overlayCloseTick) {
        val target = romSearchStateHolder.lastFocusedGameBySystem.value[system]
        filteredGames.indexOfFirst { it.path == target }.takeIf { it >= 0 } ?: 0
    }

    val focusResetKey = remember(system, overlayCloseTick) { "$system|$overlayCloseTick" }

    val allSystemNames = rememberAllSystemNames(allGames = allGames)

    BackHandler {
        when {
            showAddSystems -> showAddSystems = false
            showSystemFilter -> showSystemFilter = false
            showSettingsDialog -> showSettingsDialog = false
            else -> viewModel.navigateTo(FrontendRoute.Systems)
        }
    }

    var focusedGame by remember(system) { mutableStateOf<GameInfo?>(null) }
    // Route B/BACK through the OnBackPressedDispatcher so the innermost enabled
    // BackHandler (e.g. the one that closes RomResultsGrid's details panel)
    // fires first, and only when nothing inner is enabled does this screen's
    // own BackHandler at the top of GamesRoute take over.
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    // Focus events only fire on *change*, so nothing sets this on first composition.
    // Fall back to the game the grid seeds focus onto (same index it uses) until a
    // real focus event arrives, so the hero background paints on frame one instead
    // of after the first D-pad press.
    val effectiveFocusedGame = focusedGame ?: filteredGames.getOrNull(initialGameIndex)
    val focusBackgroundActive =
        esdeState.frontendFocusBackgroundEnabled && esdeState.frontendFocusBackgroundGames
    val focusBackgroundUri = if (focusBackgroundActive) {
        effectiveFocusedGame?.heroBackgroundPath()
    } else null

    Surface(
        color = if (esdeState.romSearchUseWallpaper) Color.Transparent else OledBackgroundColor,
        modifier = Modifier
            .fillMaxSize()
            .frontendControlKeys(
                overlayVisible = overlayVisible,
                onSearch = { romSearchStateHolder.showSearchKeyboardSignal.tryEmit(Unit) },
                onOpenSettings = { showSettingsDialog = true }
            )
            .onPreviewKeyEvent { keyEvent ->
                if (overlayVisible) return@onPreviewKeyEvent false
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.nativeKeyEvent.keyCode) {
                    // Hack: catch back at the preview stage so neither the focus
                    // animation nor any descendant can swallow the first press —
                    // then hand off to the back dispatcher so any active inner
                    // BackHandler (like the details panel's) runs before we
                    // fall back to leaving the route.
                    AndroidKeyEvent.KEYCODE_BUTTON_B,
                    AndroidKeyEvent.KEYCODE_BACK -> {
                        backDispatcher?.onBackPressed()
                            ?: viewModel.navigateTo(FrontendRoute.Systems)
                        true
                    }

                    else -> false
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = !overlayVisible }
        ) {
            FrontendFocusBackground(
                focusedUri = focusBackgroundUri,
                dimAlpha = esdeState.frontendFocusBackgroundDimGames,
                fallbackColor = if (focusBackgroundActive) OledBackgroundColor else Color.Transparent
            )
            FrontendRomGrid(
                games = filteredGames,
                isLoading = isLoading,
                isHiddenMode = false,
                backgroundTransparent = esdeState.romSearchUseWallpaper || focusBackgroundActive,
                cardMediaType = esdeState.romSearchCardMediaType,
                focusAnimationEnabled = esdeState.romSearchDiscSpin,
                focusAnimationDelayMs = esdeState.romSearchFocusAnimationDelayMs,
                focusAnimationDisabledGames = esdeState.romSearchFocusAnimationDisabledGames,
                gameMediaMap = esdeState.romSearchGameMediaMap,
                systemMediaMap = esdeState.systemMediaMap,
                focusResetKey = focusResetKey,
                layout = esdeState.gameLayout,
                esdePrefs = esdePrefs,
                viewModel = viewModel,
                romSearchStateHolder = romSearchStateHolder,
                frontendSelectionStateHolder = frontendSelectionStateHolder,
                managers = managers,
                romLauncher = romLauncher,
                appDisplayPreferenceManager = appDisplayPreferenceManager,
                onChangeFolder = onChangeFolder,
                modifier = Modifier.fillMaxSize(),
                system = system,
                initialRealIndex = initialGameIndex,
                onFocusedGameChanged = { focusedGame = it }
            )
        }
    }

    if (showSettingsDialog) {
        FrontendSettingsScreen(
            onDismiss = { showSettingsDialog = false },
            onOpenSystemFilter = {
                showSettingsDialog = false
                showSystemFilter = true
            },
            onOpenAddSystems = {
                showSettingsDialog = false
                showAddSystems = true
            }
        )
    }
    if (showAddSystems) {
        AddSystemsScreen(
            esdePrefs = esdePrefs,
            knownSystemSuggestions = allSystemNames,
            onDismiss = { showAddSystems = false }
        )
    }
    if (showSystemFilter) {
        SystemFilterSheet(
            systems = allSystemNames,
            hiddenSystems = esdeState.hiddenSystems,
            onToggle = esdePrefs::setSystemHidden,
            onShowAll = { esdePrefs.setHiddenSystems(emptySet()) },
            onHideAll = { esdePrefs.setHiddenSystems(allSystemNames) },
            onDismiss = { showSystemFilter = false }
        )
    }
}

@Composable
private fun SystemsRoute(
    allGames: List<GameInfo>,
    isLoading: Boolean,
    hiddenGames: Set<String>,
    hiddenSystems: Set<String>,
    layout: FrontendLayout,
    useWallpaper: Boolean,
    customizations: Map<String, SystemCustomization>,
    systemOrder: List<String>,
    hintsVisible: Boolean,
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder
) {
    val baseSystems = rememberSystemTiles(
        allGames = allGames,
        hiddenGames = hiddenGames,
        hiddenSystems = hiddenSystems
    )
    val allSystemNames = rememberAllSystemNames(allGames = allGames)
    var workingOrder by remember(baseSystems, systemOrder) {
        mutableStateOf(applySystemOrder(baseSystems, systemOrder))
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSystemFilter by remember { mutableStateOf(false) }
    var showAddSystems by remember { mutableStateOf(false) }
    var customizingSystem by remember { mutableStateOf<String?>(null) }
    var reorderingSystem by remember { mutableStateOf<String?>(null) }
    var focusedSystemName by remember { mutableStateOf<String?>(null) }

    // Any overlay drawn on top of the grid holds focus for its whole lifetime.
    // On open we drop focus explicitly so the grid stops receiving events; on
    // close the reset tick below re-seeds focus onto the tile the user was on
    // before the overlay opened.
    val overlayVisible = showSettingsDialog || showSystemFilter || showAddSystems ||
        customizingSystem != null

    val focusManager = LocalFocusManager.current
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) focusManager.clearFocus(force = true)
    }

    // Only bump on the true→false edge — bumping on open would race the
    // overlay's own focus request and sometimes leave focus on the grid.
    var overlayCloseTick by remember { mutableIntStateOf(0) }
    var previousOverlayVisible by remember { mutableStateOf(overlayVisible) }
    LaunchedEffect(overlayVisible) {
        if (previousOverlayVisible && !overlayVisible) overlayCloseTick++
        previousOverlayVisible = overlayVisible
    }

    val initialSystemIndex = remember(workingOrder, overlayCloseTick) {
        val target = romSearchStateHolder.lastFocusedSystem.value
        workingOrder.indexOfFirst { it.systemName == target }.takeIf { it >= 0 } ?: 0
    }

    val esdeState by esdePrefs.state.collectAsStateWithLifecycle()
    // Focus events only fire on *change*, so nothing sets focusedSystemName on the
    // very first composition. Fall back to the tile the grid is about to seed focus
    // onto (same index SystemGrid is given via initialRealIndex) until a real focus
    // event arrives. initialSystemIndex is remember(workingOrder)-keyed above, so
    // this self-corrects when the list resolves after loading.
    val effectiveFocusedSystem = focusedSystemName
        ?: workingOrder.getOrNull(initialSystemIndex)?.systemName
    val focusBackgroundActive =
        esdeState.frontendFocusBackgroundEnabled && esdeState.frontendFocusBackgroundSystems
    val focusBackgroundUri = if (focusBackgroundActive) {
        effectiveFocusedSystem?.let { customizations[it]?.focusBackgroundUri }
    } else null

    // Back / B button on Systems is intentionally inert (kiosk behaviour) — the
    // frontend is the launcher's home, so the user shouldn't be able to back out of
    // it. Only the in-route settings dialog uses back to close.
    BackHandler {
        when {
            reorderingSystem != null -> {
                workingOrder = applySystemOrder(baseSystems, systemOrder)
                reorderingSystem = null
            }
            customizingSystem != null -> customizingSystem = null
            showAddSystems -> showAddSystems = false
            showSystemFilter -> showSystemFilter = false
            showSettingsDialog -> showSettingsDialog = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusProperties { canFocus = !overlayVisible }
            .onPreviewKeyEvent { keyEvent ->
                if (overlayVisible) return@onPreviewKeyEvent false
                handleSystemsRouteKey(
                    keyEvent = keyEvent,
                    reorderingSystem = reorderingSystem,
                    workingOrder = workingOrder,
                    onReorderMove = { delta ->
                        val name = reorderingSystem ?: return@handleSystemsRouteKey
                        val moved = moveSystemInList(workingOrder, name, delta)
                        if (moved !== workingOrder) {
                            workingOrder = moved
                            romSearchStateHolder.lastFocusedSystem.value = name
                            frontendSelectionStateHolder.selectSystem(name)
                        }
                    },
                    onReorderConfirm = {
                        esdePrefs.setSystemOrder(workingOrder.map { it.systemName })
                        reorderingSystem = null
                    },
                    onReorderCancel = {
                        workingOrder = applySystemOrder(baseSystems, systemOrder)
                        reorderingSystem = null
                    },
                    onSearch = { romSearchStateHolder.showSearchKeyboardSignal.tryEmit(Unit) },
                    onOpenSettings = { showSettingsDialog = true },
                    onCustomizeFocused = {
                        val name = romSearchStateHolder.lastFocusedSystem.value
                            ?: workingOrder.firstOrNull()?.systemName
                        if (name != null) customizingSystem = name
                    }
                )
            }
    ) {
        FrontendFocusBackground(
            focusedUri = focusBackgroundUri,
            dimAlpha = esdeState.frontendFocusBackgroundDimSystems,
            fallbackColor = if (focusBackgroundActive) OledBackgroundColor else Color.Transparent
        )
        SystemGrid(
            systems = workingOrder,
            isLoading = isLoading,
            layout = layout,
            initialRealIndex = initialSystemIndex,
            focusResetKey = overlayCloseTick,
            backgroundTransparent = useWallpaper || focusBackgroundActive,
            customizations = customizations,
            reorderingSystem = reorderingSystem,
            onSystemFocused = { tile ->
                focusedSystemName = tile.systemName
                frontendSelectionStateHolder.selectSystem(tile.systemName)
                romSearchStateHolder.lastFocusedSystem.value = tile.systemName
            },
            onSystemSelected = { tile ->
                if (reorderingSystem != null) {
                    esdePrefs.setSystemOrder(workingOrder.map { it.systemName })
                    reorderingSystem = null
                } else {
                    viewModel.navigateTo(FrontendRoute.Games(tile.systemName))
                }
            },
            onSystemLongPressed = { tile -> customizingSystem = tile.systemName },
            modifier = Modifier.fillMaxSize()
        )

        if (hintsVisible || reorderingSystem != null) {
            FrontendAffordanceHints(
                modifier = Modifier.align(Alignment.BottomEnd),
                reorderActive = reorderingSystem != null
            )
        }
    }

    if (showSettingsDialog) {
        FrontendSettingsScreen(
            onDismiss = { showSettingsDialog = false },
            onOpenSystemFilter = {
                showSettingsDialog = false
                showSystemFilter = true
            },
            onOpenAddSystems = {
                showSettingsDialog = false
                showAddSystems = true
            }
        )
    }
    if (showAddSystems) {
        AddSystemsScreen(
            esdePrefs = esdePrefs,
            knownSystemSuggestions = allSystemNames,
            onDismiss = { showAddSystems = false }
        )
    }
    if (showSystemFilter) {
        SystemFilterSheet(
            systems = allSystemNames,
            hiddenSystems = hiddenSystems,
            onToggle = esdePrefs::setSystemHidden,
            onShowAll = { esdePrefs.setHiddenSystems(emptySet()) },
            onHideAll = { esdePrefs.setHiddenSystems(allSystemNames) },
            onDismiss = { showSystemFilter = false }
        )
    }

    val activeCustomizeTarget = customizingSystem
    if (activeCustomizeTarget != null) {
        val gamesForSystem = remember(allGames, activeCustomizeTarget) {
            allGames.filter { it.systemName.equals(activeCustomizeTarget, ignoreCase = true) }
        }
        SystemCustomizationScreen(
            systemName = activeCustomizeTarget,
            customization = customizations[activeCustomizeTarget]
                ?: jr.brian.home.esde.model.SystemCustomization(),
            esdePrefs = esdePrefs,
            gamesForSystem = gamesForSystem,
            romsPaths = esdeState.romsPaths,
            onDismiss = { customizingSystem = null },
            onChange = { updated -> esdePrefs.setSystemCustomization(activeCustomizeTarget, updated) },
            onReset = {
                esdePrefs.clearSystemCustomization(activeCustomizeTarget)
                customizingSystem = null
            },
            onEnterReorder = {
                reorderingSystem = activeCustomizeTarget
                customizingSystem = null
            }
        )
    }
}

private fun handleSystemsRouteKey(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    reorderingSystem: String?,
    workingOrder: List<SystemTile>,
    onReorderMove: (Int) -> Unit,
    onReorderConfirm: () -> Unit,
    onReorderCancel: () -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onCustomizeFocused: () -> Unit
): Boolean {
    if (keyEvent.type != KeyEventType.KeyDown) return false
    val code = keyEvent.nativeKeyEvent.keyCode
    if (reorderingSystem != null) {
        return when (code) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                onReorderMove(-1); true
            }
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                onReorderMove(1); true
            }
            AndroidKeyEvent.KEYCODE_BUTTON_A, AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            AndroidKeyEvent.KEYCODE_ENTER -> {
                onReorderConfirm(); true
            }
            AndroidKeyEvent.KEYCODE_BUTTON_B, AndroidKeyEvent.KEYCODE_BACK -> {
                onReorderCancel(); true
            }
            else -> true
        }
    }
    return when (code) {
        AndroidKeyEvent.KEYCODE_BUTTON_Y -> { onSearch(); true }
        AndroidKeyEvent.KEYCODE_BUTTON_START -> { onOpenSettings(); true }
        AndroidKeyEvent.KEYCODE_BUTTON_SELECT -> { onCustomizeFocused(); true }
        else -> false
    }
}

private fun moveSystemInList(
    list: List<SystemTile>,
    systemName: String,
    delta: Int
): List<SystemTile> {
    val from = list.indexOfFirst { it.systemName == systemName }
    if (from < 0) return list
    val to = (from + delta).coerceIn(0, list.lastIndex)
    if (to == from) return list
    val mutable = list.toMutableList()
    val tile = mutable.removeAt(from)
    mutable.add(to, tile)
    return mutable
}

private fun applySystemOrder(
    systems: List<SystemTile>,
    order: List<String>
): List<SystemTile> {
    if (order.isEmpty()) return systems
    val byName = systems.associateBy { it.systemName }
    val ordered = order.mapNotNull { byName[it] }
    val remaining = systems.filter { it.systemName !in order }
    return ordered + remaining
}

@Composable
private fun rememberSystemTiles(
    allGames: List<GameInfo>,
    hiddenGames: Set<String>,
    hiddenSystems: Set<String>
): List<SystemTile> = remember(allGames, hiddenGames, hiddenSystems) {
    allGames
        .asSequence()
        .filter { hiddenGameKey(it) !in hiddenGames }
        .filter { !it.systemName.equals(ANDROID_APPS_SYSTEM, ignoreCase = true) }
        .groupBy { it.systemName }
        .map { (name, _) -> SystemTile(systemName = name) }
        .filter { it.systemName !in hiddenSystems }
        .sortedBy { it.systemName.lowercase() }
}

@Composable
internal fun rememberAllSystemNames(allGames: List<GameInfo>): List<String> =
    remember(allGames) {
        allGames
            .asSequence()
            .filter { !it.systemName.equals(ANDROID_APPS_SYSTEM, ignoreCase = true) }
            .map { it.systemName }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()
    }

@Composable
private fun FrontendAffordanceHints(
    modifier: Modifier = Modifier,
    reorderActive: Boolean
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        if (reorderActive) {
            Text(
                text = stringResource(R.string.frontend_reorder_hint),
                color = ThemeAccentColor.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelMedium
            )
        } else {
            Text(
                text = stringResource(R.string.frontend_open_customize_hint),
                color = ThemeAccentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.frontend_open_search_hint),
                color = ThemeAccentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.frontend_open_settings_hint),
                color = ThemeAccentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

