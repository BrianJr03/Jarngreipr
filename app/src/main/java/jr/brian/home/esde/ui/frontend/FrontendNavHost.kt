package jr.brian.home.esde.ui.frontend

import jr.brian.home.esde.data.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.FrontendSelectionStateHolder
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.ui.RomGameLauncher
import jr.brian.home.esde.ui.frontend.settings.AddSystemsScreen
import jr.brian.home.esde.ui.frontend.settings.FrontendSettingsScreen
import jr.brian.home.esde.ui.frontend.settings.ScreenScraperSettingsScreen
import jr.brian.home.esde.ui.frontend.settings.SteamGridDbSettingsScreen
import jr.brian.home.esde.ui.frontend.settings.SystemCustomizationScreen
import jr.brian.home.esde.util.heroBackgroundPath
import jr.brian.home.esde.util.hiddenGameKey
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import android.view.KeyEvent as AndroidKeyEvent

private const val ANDROID_APPS_SYSTEM = "androidapps"

private object Routes {
    const val SYSTEMS = "systems"
    const val GAMES = "games/{system}"
    const val SETTINGS = "settings"
    const val SETTINGS_FILTER = "settings/filter"
    const val SETTINGS_ADD_SYSTEMS = "settings/add-systems"
    const val SETTINGS_SCRAPER_SCREENSCRAPER = "settings/scraper/screenscraper"
    const val SETTINGS_SCRAPER_STEAMGRIDDB = "settings/scraper/steamgriddb"
    const val CUSTOMIZE = "customize/{system}"

    fun games(system: String) = "games/$system"
    fun customize(system: String) = "customize/$system"
}

private val BASE_ROUTES = setOf(Routes.SYSTEMS, Routes.GAMES)

private const val REORDER_SIGNAL_KEY = "startReorder"

private fun Modifier.frontendControlKeys(
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit
): Modifier = onPreviewKeyEvent { keyEvent ->
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
internal fun FrontendNavHost(
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder,
    managers: ManagerContainer,
    romLauncher: RomGameLauncher,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onChangeFolder: (GameInfo) -> Unit,
    showFrontendWallpaper: Boolean,
) {
    val navController = rememberNavController()
    val esdeState by esdePrefs.state.collectAsStateWithLifecycle()
    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val scanLoaded by viewModel.systemScanLoaded.collectAsStateWithLifecycle()
    val scanTotal by viewModel.systemScanTotal.collectAsStateWithLifecycle()
    val allSystemNames = rememberAllSystemNames(allGames)

    NavHost(
        navController = navController,
        startDestination = Routes.SYSTEMS,
        modifier = Modifier
            .fillMaxSize()
            // SystemsRoute's outer Box is transparent unless the focus background
            // is active, and during the Systems↔Games cross-fade both panels
            // have transparent regions. Without an opaque backer the activity
            // window's underlying (system) wallpaper flashes through on scroll
            // and during transition. Leave it transparent when the user
            // explicitly opted into the ES-DE wallpaper OR has a frontend
            // wallpaper layer mounted below us.
            .background(
                if (esdeState.romSearchUseWallpaper || showFrontendWallpaper) Color.Transparent
                else OledBackgroundColor
            ),
        enterTransition = {
            val fromBase = initialState.destination.route in BASE_ROUTES
            val toBase = targetState.destination.route in BASE_ROUTES
            if (fromBase && toBase) {
                val forward = initialState.destination.route == Routes.SYSTEMS
                esdeState.frontendTransition
                    .transform(forward, esdeState.frontendTransitionMs)
                    .targetContentEnter
            } else EnterTransition.None
        },
        exitTransition = {
            val fromBase = initialState.destination.route in BASE_ROUTES
            val toBase = targetState.destination.route in BASE_ROUTES
            if (fromBase && toBase) {
                val forward = initialState.destination.route == Routes.SYSTEMS
                esdeState.frontendTransition
                    .transform(forward, esdeState.frontendTransitionMs)
                    .initialContentExit
            } else ExitTransition.None
        },
        popEnterTransition = {
            val fromBase = initialState.destination.route in BASE_ROUTES
            val toBase = targetState.destination.route in BASE_ROUTES
            if (fromBase && toBase) {
                val forward = targetState.destination.route == Routes.SYSTEMS
                esdeState.frontendTransition
                    .transform(forward, esdeState.frontendTransitionMs)
                    .targetContentEnter
            } else EnterTransition.None
        },
        popExitTransition = {
            val fromBase = initialState.destination.route in BASE_ROUTES
            val toBase = targetState.destination.route in BASE_ROUTES
            if (fromBase && toBase) {
                val forward = targetState.destination.route == Routes.SYSTEMS
                esdeState.frontendTransition
                    .transform(forward, esdeState.frontendTransitionMs)
                    .initialContentExit
            } else ExitTransition.None
        },
    ) {
        composable(Routes.SYSTEMS) { backStackEntry ->
            SystemsRoute(
                allGames = allGames,
                isLoading = isLoading,
                scanLoaded = scanLoaded,
                scanTotal = scanTotal,
                hiddenGames = esdeState.hiddenGames,
                hiddenSystems = esdeState.hiddenSystems,
                layout = run {
                    val assignment = jr.brian.home.ui.util.rememberEffectiveAssignment()
                    if (assignment == jr.brian.core.model.ScreenAssignment.FRONTEND_ON_TOP)
                        esdeState.primarySystemLayout
                    else esdeState.secondarySystemLayout
                },
                useWallpaper = esdeState.romSearchUseWallpaper || showFrontendWallpaper,
                customizations = esdeState.systemCustomizations,
                systemOrder = esdeState.systemOrder,
                hintsVisible = esdeState.frontendHintsVisible,
                esdePrefs = esdePrefs,
                romSearchStateHolder = romSearchStateHolder,
                frontendSelectionStateHolder = frontendSelectionStateHolder,
                esdeState = esdeState,
                reorderSignal = backStackEntry.savedStateHandle
                    .getStateFlow<String?>(REORDER_SIGNAL_KEY, null)
                    .collectAsStateWithLifecycle().value,
                onReorderConsumed = {
                    backStackEntry.savedStateHandle[REORDER_SIGNAL_KEY] = null
                },
                onOpenGames = { system -> navController.navigate(Routes.games(system)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenCustomize = { system -> navController.navigate(Routes.customize(system)) },
            )
        }

        composable(
            route = Routes.GAMES,
            arguments = listOf(navArgument("system") { type = NavType.StringType }),
        ) { backStackEntry ->
            val system = backStackEntry.arguments?.getString("system").orEmpty()
            GamesRoute(
                system = system,
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
                onChangeFolder = onChangeFolder,
                showFrontendWallpaper = showFrontendWallpaper,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            FrontendSettingsScreen(
                onDismiss = { navController.popBackStack() },
                onOpenSystemFilter = { navController.navigate(Routes.SETTINGS_FILTER) },
                onOpenAddSystems = { navController.navigate(Routes.SETTINGS_ADD_SYSTEMS) },
                onOpenScreenScraperSettings = {
                    navController.navigate(Routes.SETTINGS_SCRAPER_SCREENSCRAPER)
                },
                onOpenSteamGridDbSettings = {
                    navController.navigate(Routes.SETTINGS_SCRAPER_STEAMGRIDDB)
                },
            )
        }

        composable(Routes.SETTINGS_FILTER) {
            SystemFilterSheet(
                systems = allSystemNames,
                hiddenSystems = esdeState.hiddenSystems,
                onToggle = esdePrefs::setSystemHidden,
                onShowAll = { esdePrefs.setHiddenSystems(emptySet()) },
                onHideAll = { esdePrefs.setHiddenSystems(allSystemNames.toSet()) },
                onDismiss = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS_ADD_SYSTEMS) {
            AddSystemsScreen(
                esdePrefs = esdePrefs,
                knownSystemSuggestions = allSystemNames,
                onDismiss = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS_SCRAPER_SCREENSCRAPER) {
            ScreenScraperSettingsScreen(onDismiss = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_SCRAPER_STEAMGRIDDB) {
            SteamGridDbSettingsScreen(onDismiss = { navController.popBackStack() })
        }

        composable(
            route = Routes.CUSTOMIZE,
            arguments = listOf(navArgument("system") { type = NavType.StringType }),
        ) { backStackEntry ->
            val system = backStackEntry.arguments?.getString("system").orEmpty()
            val gamesForSystem = remember(allGames, system) {
                allGames.filter { it.systemName.equals(system, ignoreCase = true) }
            }
            SystemCustomizationScreen(
                systemName = system,
                customization = esdeState.systemCustomizations[system]
                    ?: SystemCustomization(),
                esdePrefs = esdePrefs,
                gamesForSystem = gamesForSystem,
                romsPaths = esdeState.romsPaths,
                onDismiss = { navController.popBackStack() },
                onChange = { updated -> esdePrefs.setSystemCustomization(system, updated) },
                onReset = {
                    esdePrefs.clearSystemCustomization(system)
                    navController.popBackStack()
                },
                onEnterReorder = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(REORDER_SIGNAL_KEY, system)
                    navController.popBackStack()
                },
            )
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
    onChangeFolder: (GameInfo) -> Unit,
    showFrontendWallpaper: Boolean,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(system) {
        frontendSelectionStateHolder.selectSystem(system)
    }

    val filteredGames = rememberFilteredGames(
        allGames = allGames,
        hiddenGames = esdeState.hiddenGames,
        forcedPlatform = system
    )

    LaunchedEffect(filteredGames) {
        filteredGames.firstOrNull()?.let { viewModel.updateFocusedGame(it) }
    }

    val initialGameIndex = remember(filteredGames, system) {
        val target = romSearchStateHolder.lastFocusedGameBySystem.value[system]
        filteredGames.indexOfFirst { it.path == target }.takeIf { it >= 0 } ?: 0
    }

    var focusedGame by remember(system) { mutableStateOf<GameInfo?>(null) }
    // Route B/BACK through the OnBackPressedDispatcher so the innermost enabled
    // BackHandler (e.g. the one that closes RomResultsGrid's details panel)
    // fires first, and only when nothing inner is enabled does popBackStack
    // leave the route.
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

    BackHandler { onBack() }

    Surface(
        color = if (esdeState.romSearchUseWallpaper || showFrontendWallpaper) Color.Transparent else OledBackgroundColor,
        modifier = Modifier
            .fillMaxSize()
            .frontendControlKeys(
                onSearch = { romSearchStateHolder.showSearchKeyboardSignal.tryEmit(Unit) },
                onOpenSettings = onOpenSettings,
            )
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.nativeKeyEvent.keyCode) {
                    // Hack: catch back at the preview stage so neither the focus
                    // animation nor any descendant can swallow the first press —
                    // then hand off to the back dispatcher so any active inner
                    // BackHandler (like the details panel's) runs before we
                    // fall back to leaving the route.
                    AndroidKeyEvent.KEYCODE_BUTTON_B,
                    AndroidKeyEvent.KEYCODE_BACK -> {
                        backDispatcher?.onBackPressed() ?: onBack()
                        true
                    }

                    else -> false
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FrontendFocusBackground(
                focusedUri = focusBackgroundUri,
                dimAlpha = esdeState.frontendFocusBackgroundDimGames,
                fallbackColor = if (focusBackgroundActive) OledBackgroundColor else Color.Transparent
            )
            FrontendRomGrid(
                games = filteredGames,
                isLoading = isLoading,
                isHiddenMode = false,
                backgroundTransparent = esdeState.romSearchUseWallpaper || showFrontendWallpaper || focusBackgroundActive,
                cardMediaType = esdeState.romSearchCardMediaType,
                focusAnimationEnabled = esdeState.romSearchDiscSpin,
                focusAnimationDelayMs = esdeState.romSearchFocusAnimationDelayMs,
                focusAnimationDisabledGames = esdeState.romSearchFocusAnimationDisabledGames,
                gameMediaMap = esdeState.romSearchGameMediaMap,
                systemMediaMap = esdeState.systemMediaMap,
                focusResetKey = system,
                layout = run {
                    val assignment = jr.brian.home.ui.util.rememberEffectiveAssignment()
                    if (assignment == jr.brian.core.model.ScreenAssignment.FRONTEND_ON_TOP)
                        esdeState.primaryGameLayout
                    else esdeState.secondaryGameLayout
                },
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
}

@Composable
private fun SystemsRoute(
    allGames: List<GameInfo>,
    isLoading: Boolean,
    scanLoaded: Int,
    scanTotal: Int,
    hiddenGames: Set<String>,
    hiddenSystems: Set<String>,
    layout: FrontendLayout,
    useWallpaper: Boolean,
    customizations: Map<String, SystemCustomization>,
    systemOrder: List<String>,
    hintsVisible: Boolean,
    esdePrefs: ESDEPreferencesManager,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder,
    esdeState: jr.brian.home.esde.model.ESDEPrefsState,
    reorderSignal: String?,
    onReorderConsumed: () -> Unit,
    onOpenGames: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCustomize: (String) -> Unit,
) {
    val baseSystems = rememberSystemTiles(
        allGames = allGames,
        hiddenGames = hiddenGames,
        hiddenSystems = hiddenSystems
    )
    var workingOrder by remember(baseSystems, systemOrder) {
        mutableStateOf(applySystemOrder(baseSystems, systemOrder))
    }
    var reorderingSystem by remember { mutableStateOf<String?>(null) }
    var focusedSystemName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reorderSignal) {
        val pending = reorderSignal ?: return@LaunchedEffect
        reorderingSystem = pending
        onReorderConsumed()
    }

    val initialSystemIndex = remember(workingOrder) {
        val target = romSearchStateHolder.lastFocusedSystem.value
        workingOrder.indexOfFirst { it.systemName == target }.takeIf { it >= 0 } ?: 0
    }

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

    // Back / B at the systems base is intentionally inert (kiosk behaviour) —
    // the frontend is the launcher's home, so the user shouldn't be able to
    // back out of it. Only cancel in-progress reorder here; everything else
    // falls through to FrontEndActivity's no-op kiosk BackHandler.
    BackHandler(enabled = reorderingSystem != null) {
        workingOrder = applySystemOrder(baseSystems, systemOrder)
        reorderingSystem = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                handleSystemsRouteKey(
                    keyEvent = keyEvent,
                    reorderingSystem = reorderingSystem,
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
                    onOpenSettings = onOpenSettings,
                    onCustomizeFocused = {
                        val name = romSearchStateHolder.lastFocusedSystem.value
                            ?: workingOrder.firstOrNull()?.systemName
                        if (name != null) onOpenCustomize(name)
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
            focusResetKey = Unit,
            backgroundTransparent = useWallpaper || focusBackgroundActive,
            customizations = customizations,
            reorderingSystem = reorderingSystem,
            scanTotal = scanTotal,
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
                    onOpenGames(tile.systemName)
                }
            },
            onSystemLongPressed = { tile -> onOpenCustomize(tile.systemName) },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading && scanTotal > 0) {
            SystemsLoadingBadge(
                loaded = scanLoaded,
                total = scanTotal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        if (hintsVisible || reorderingSystem != null) {
            FrontendAffordanceHints(
                modifier = Modifier.align(Alignment.BottomEnd),
                reorderActive = reorderingSystem != null
            )
        }
    }
}

private fun handleSystemsRouteKey(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    reorderingSystem: String?,
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
private fun SystemsLoadingBadge(
    loaded: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(
            R.string.frontend_systems_loading_badge,
            loaded.coerceAtMost(total),
            total
        ),
        modifier = modifier,
        color = ThemeAccentColor,
        style = MaterialTheme.typography.labelMedium
    )
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
