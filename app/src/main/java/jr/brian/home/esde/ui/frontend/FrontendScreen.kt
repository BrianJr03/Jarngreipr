package jr.brian.home.esde.ui.frontend

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.FrontendSelectionStateHolder
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.ui.PlatformSuggestionsDropdown
import jr.brian.home.esde.ui.RomGameLauncher
import jr.brian.home.esde.util.hiddenGameKey
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.model.rom.toGameInfo
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.viewmodels.MainViewModel
import kotlinx.coroutines.delay

private const val ANIM_ENTER_MS = 220
private const val ANIM_EXIT_MS = 180

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
            mainViewModel = mainViewModel,
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
    mainViewModel: MainViewModel,
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

    when (val currentRoute = route) {
        is FrontendRoute.Search -> SearchRoute(
            allGames = allGames,
            isLoading = isLoading,
            esdeState = esdeState,
            esdePrefs = esdePrefs,
            viewModel = viewModel,
            mainViewModel = mainViewModel,
            romSearchStateHolder = romSearchStateHolder,
            frontendSelectionStateHolder = frontendSelectionStateHolder,
            managers = managers,
            romLauncher = romLauncher,
            appDisplayPreferenceManager = appDisplayPreferenceManager,
            onChangeFolder = onChangeFolder
        )

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

        is FrontendRoute.Systems -> SystemsRouteStub()
    }
}

@Composable
private fun SearchRoute(
    allGames: List<GameInfo>,
    isLoading: Boolean,
    esdeState: jr.brian.home.esde.model.ESDEPrefsState,
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    mainViewModel: MainViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder,
    managers: ManagerContainer,
    romLauncher: RomGameLauncher,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onChangeFolder: (GameInfo) -> Unit
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val queryTrimmed = query.trim()
    val homeUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val hiddenGames = esdeState.hiddenGames
    val romSearchUseWallpaper = esdeState.romSearchUseWallpaper
    val cardMediaType = esdeState.romSearchCardMediaType
    val gameMediaMap = esdeState.romSearchGameMediaMap
    val romSearchShowAllAndroidApps = esdeState.romSearchShowAllAndroidApps

    val platformImageMap by rememberPlatformImageMap(
        enabled = esdeState.romSearchPlatformImagesEnabled,
        folderUri = esdeState.romSearchPlatformImagesFolderUri,
        folderType = esdeState.romSearchPlatformImagesFolderType
    )

    LaunchedEffect(romSearchShowAllAndroidApps) {
        if (romSearchShowAllAndroidApps) mainViewModel.loadAllApps(context)
    }

    val modes = rememberSearchModes(
        queryTrimmed = queryTrimmed,
        allGames = allGames,
        hiddenGames = hiddenGames,
        romSearchShowAllAndroidApps = romSearchShowAllAndroidApps
    )
    val allAndroidApps = remember(romSearchShowAllAndroidApps, homeUiState.allApps) {
        if (!romSearchShowAllAndroidApps) emptyList()
        else homeUiState.allApps.map { appInfo ->
            GameInfo(
                path = appInfo.packageName,
                name = appInfo.label,
                systemName = "androidapps"
            )
        }
    }

    var autoFilterPlatform by remember { mutableStateOf<String?>(null) }

    val filteredGames = rememberFilteredGames(
        allGames = allGames,
        hiddenGames = hiddenGames,
        hideNoMetadata = esdeState.romSearchHideNoMetadata,
        hideNoImage = esdeState.romSearchHideNoImage,
        cardMediaType = cardMediaType,
        queryTrimmed = queryTrimmed,
        selectedPlatform = modes.selectedPlatform,
        isPlatformMode = modes.isPlatformMode,
        isHiddenMode = modes.isHiddenMode,
        isAndroidMode = modes.isAndroidMode,
        androidModeFilter = modes.androidModeFilter,
        platformSearch = modes.platformSearch,
        autoFilterPlatform = autoFilterPlatform,
        allAndroidApps = allAndroidApps
    )

    val dropdownVisible = modes.isPlatformMode && modes.selectedPlatform == null &&
            (modes.platformSuggestions.isNotEmpty() || romSearchShowAllAndroidApps)
    var dropdownFocusedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(dropdownVisible) {
        if (!dropdownVisible) dropdownFocusedIndex = -1
    }
    LaunchedEffect(dropdownFocusedIndex) {
        if (dropdownFocusedIndex < 0) autoFilterPlatform = null
    }

    val dropdownItems = remember(romSearchShowAllAndroidApps, modes.platformSuggestions) {
        buildList {
            if (romSearchShowAllAndroidApps) add("android")
            addAll(modes.platformSuggestions)
        }
    }

    var focusResetCounter by remember { mutableIntStateOf(0) }
    LaunchedEffect(queryTrimmed) {
        if (queryTrimmed.isNotBlank()) focusResetCounter++
    }

    LaunchedEffect(filteredGames) {
        filteredGames.firstOrNull()?.let { viewModel.updateFocusedGame(it) }
    }

    val firstAvailableSystem by remember(allGames) {
        derivedStateOf {
            allGames.map { it.systemName }.distinct().sorted().firstOrNull()
        }
    }

    BackHandler {
        if (queryTrimmed.isNotEmpty()) {
            viewModel.clearState()
        } else {
            romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
            romSearchStateHolder.dismissSignal.tryEmit(Unit)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .searchKeyEvents(
                dropdownVisible = dropdownVisible,
                dropdownFocusedIndex = dropdownFocusedIndex,
                dropdownItems = dropdownItems,
                onSetDropdownIndex = { dropdownFocusedIndex = it },
                onUpdateQuery = viewModel::updateQuery,
                onOpenGamesForFirstSystem = {
                    // TODO(phase2): replace with system tile selection
                    firstAvailableSystem?.let { system ->
                        viewModel.navigateTo(FrontendRoute.Games(system))
                    }
                }
            )
    ) {
        Surface(
            color = if (romSearchUseWallpaper) Color.Transparent else OledBackgroundColor,
            modifier = Modifier.fillMaxSize()
        ) {
            FrontendRomGrid(
                games = filteredGames,
                isLoading = isLoading,
                isHiddenMode = modes.isHiddenMode,
                backgroundTransparent = romSearchUseWallpaper,
                cardMediaType = cardMediaType,
                focusAnimationEnabled = esdeState.romSearchDiscSpin,
                focusAnimationDelayMs = esdeState.romSearchFocusAnimationDelayMs,
                focusAnimationDisabledGames = esdeState.romSearchFocusAnimationDisabledGames,
                gameMediaMap = gameMediaMap,
                focusResetKey = focusResetCounter,
                esdePrefs = esdePrefs,
                viewModel = viewModel,
                romSearchStateHolder = romSearchStateHolder,
                frontendSelectionStateHolder = frontendSelectionStateHolder,
                managers = managers,
                romLauncher = romLauncher,
                appDisplayPreferenceManager = appDisplayPreferenceManager,
                onChangeFolder = onChangeFolder,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (dropdownVisible) {
            PlatformSuggestionsDropdown(
                platforms = modes.platformSuggestions,
                showAllApps = romSearchShowAllAndroidApps,
                focusedIndex = dropdownFocusedIndex,
                autoFilter = esdeState.romSearchPlatformAutoFilter,
                onPlatformSelected = { platform ->
                    viewModel.updateQuery("@$platform ")
                    if (!esdeState.romSearchPlatformAutoFilter) {
                        dropdownFocusedIndex = -1
                    }
                },
                onPlatformFocused = { platform -> autoFilterPlatform = platform },
                platformImagesEnabled = esdeState.romSearchPlatformImagesEnabled,
                getPlatformImage = { platform -> platformImageMap[platform.lowercase()] },
                modifier = Modifier.align(Alignment.BottomCenter)
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

    val focusResetKey = remember(system) { system }

    BackHandler {
        viewModel.navigateTo(FrontendRoute.Search)
    }

    Surface(
        color = if (esdeState.romSearchUseWallpaper) Color.Transparent else OledBackgroundColor,
        modifier = Modifier.fillMaxSize()
    ) {
        FrontendRomGrid(
            games = filteredGames,
            isLoading = isLoading,
            isHiddenMode = false,
            backgroundTransparent = esdeState.romSearchUseWallpaper,
            cardMediaType = esdeState.romSearchCardMediaType,
            focusAnimationEnabled = esdeState.romSearchDiscSpin,
            focusAnimationDelayMs = esdeState.romSearchFocusAnimationDelayMs,
            focusAnimationDisabledGames = esdeState.romSearchFocusAnimationDisabledGames,
            gameMediaMap = esdeState.romSearchGameMediaMap,
            focusResetKey = focusResetKey,
            esdePrefs = esdePrefs,
            viewModel = viewModel,
            romSearchStateHolder = romSearchStateHolder,
            frontendSelectionStateHolder = frontendSelectionStateHolder,
            managers = managers,
            romLauncher = romLauncher,
            appDisplayPreferenceManager = appDisplayPreferenceManager,
            onChangeFolder = onChangeFolder,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SystemsRouteStub() {
    // Phase 2: replace with the system tile grid.
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "", color = Color.Transparent)
    }
}

private data class SearchModes(
    val isHiddenMode: Boolean,
    val isAndroidMode: Boolean,
    val androidModeFilter: String,
    val isPlatformMode: Boolean,
    val platformSearch: String?,
    val selectedPlatform: String?,
    val platformSuggestions: List<String>
)

@Composable
private fun rememberSearchModes(
    queryTrimmed: String,
    allGames: List<GameInfo>,
    hiddenGames: Set<String>,
    romSearchShowAllAndroidApps: Boolean
): SearchModes {
    val isHiddenMode = queryTrimmed.equals("@hidden", ignoreCase = true)
    val isAndroidMode = romSearchShowAllAndroidApps && (
            queryTrimmed.equals("@android", ignoreCase = true) ||
                    queryTrimmed.startsWith("@android ", ignoreCase = true)
            )
    val androidModeFilter =
        if (isAndroidMode && queryTrimmed.length > "@android ".length - 1)
            queryTrimmed.drop("@android ".length).trim()
        else ""
    val isPlatformMode = !isHiddenMode && !isAndroidMode && queryTrimmed.startsWith("@")
    val platformSearch = if (isPlatformMode) queryTrimmed.removePrefix("@") else null

    val allPlatforms = remember(allGames) {
        allGames.map { it.systemName }.distinct().sorted()
    }
    val platformSuggestions = remember(platformSearch, allPlatforms, allGames, hiddenGames) {
        platformSearch?.let { text ->
            val candidates = if (text.isBlank()) allPlatforms
            else allPlatforms.filter { it.contains(text, ignoreCase = true) }
            candidates.filter { platform ->
                allGames.any { game ->
                    game.systemName.equals(platform, ignoreCase = true) &&
                            hiddenGameKey(game) !in hiddenGames
                }
            }
        } ?: emptyList()
    }
    val selectedPlatform = remember(platformSearch, allPlatforms) {
        allPlatforms.firstOrNull { it.equals(platformSearch, ignoreCase = true) }
    }

    return SearchModes(
        isHiddenMode = isHiddenMode,
        isAndroidMode = isAndroidMode,
        androidModeFilter = androidModeFilter,
        isPlatformMode = isPlatformMode,
        platformSearch = platformSearch,
        selectedPlatform = selectedPlatform,
        platformSuggestions = platformSuggestions
    )
}

private fun Modifier.searchKeyEvents(
    dropdownVisible: Boolean,
    dropdownFocusedIndex: Int,
    dropdownItems: List<String>,
    onSetDropdownIndex: (Int) -> Unit,
    onUpdateQuery: (String) -> Unit,
    onOpenGamesForFirstSystem: () -> Unit
): Modifier = this
    .onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val isDpad = keyEvent.nativeKeyEvent.keyCode in DPAD_KEYS
        if (isDpad && dropdownFocusedIndex >= 0) {
            onSetDropdownIndex(-1)
            false
        } else false
    }
    .onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
        val code = keyEvent.nativeKeyEvent.keyCode
        // TODO(phase2): replace with system tile selection
        if (code == AndroidKeyEvent.KEYCODE_BUTTON_Y) {
            onOpenGamesForFirstSystem()
            return@onKeyEvent true
        }
        val isShoulderOrTrigger = code in SHOULDER_KEYS
        if (isShoulderOrTrigger && !dropdownVisible) {
            onUpdateQuery("@")
            return@onKeyEvent true
        }
        if (!dropdownVisible) return@onKeyEvent false
        val maxIdx = dropdownItems.lastIndex
        when (code) {
            AndroidKeyEvent.KEYCODE_BUTTON_L1,
            AndroidKeyEvent.KEYCODE_BUTTON_L2 -> {
                val next = if (dropdownFocusedIndex < 0) 0
                else (dropdownFocusedIndex - 1).coerceAtLeast(0)
                onSetDropdownIndex(next)
                true
            }

            AndroidKeyEvent.KEYCODE_BUTTON_R1,
            AndroidKeyEvent.KEYCODE_BUTTON_R2 -> {
                val next = if (dropdownFocusedIndex < 0) 0
                else (dropdownFocusedIndex + 1).coerceAtMost(maxIdx)
                onSetDropdownIndex(next)
                true
            }

            AndroidKeyEvent.KEYCODE_BUTTON_A -> {
                val idx = dropdownFocusedIndex
                if (idx in dropdownItems.indices) {
                    onUpdateQuery("@${dropdownItems[idx]} ")
                    onSetDropdownIndex(-1)
                    true
                } else false
            }

            else -> false
        }
    }

private val DPAD_KEYS = listOf(
    AndroidKeyEvent.KEYCODE_DPAD_UP,
    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
    AndroidKeyEvent.KEYCODE_DPAD_RIGHT
)

private val SHOULDER_KEYS = listOf(
    AndroidKeyEvent.KEYCODE_BUTTON_L1,
    AndroidKeyEvent.KEYCODE_BUTTON_L2,
    AndroidKeyEvent.KEYCODE_BUTTON_R1,
    AndroidKeyEvent.KEYCODE_BUTTON_R2
)
