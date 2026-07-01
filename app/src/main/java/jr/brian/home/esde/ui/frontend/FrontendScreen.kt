package jr.brian.home.esde.ui.frontend

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.ui.RomGameLauncher
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

    when (val currentRoute = route) {
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

    val initialGameIndex = remember(filteredGames, system) {
        val target = romSearchStateHolder.lastFocusedGameBySystem.value[system]
        filteredGames.indexOfFirst { it.path == target }.takeIf { it >= 0 } ?: 0
    }

    val focusResetKey = remember(system) { system }

    BackHandler {
        viewModel.navigateTo(FrontendRoute.Systems)
    }

    Surface(
        color = if (esdeState.romSearchUseWallpaper) Color.Transparent else OledBackgroundColor,
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_BUTTON_Y -> true
                    // Hack: catch back at the preview stage so neither the focus
                    // animation nor any descendant can swallow the first press.
                    AndroidKeyEvent.KEYCODE_BUTTON_B,
                    AndroidKeyEvent.KEYCODE_BACK -> {
                        viewModel.navigateTo(FrontendRoute.Systems)
                        true
                    }

                    else -> false
                }
            }
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
            initialRealIndex = initialGameIndex
        )
    }
}

@Composable
private fun SystemsRoute(
    allGames: List<GameInfo>,
    isLoading: Boolean,
    hiddenGames: Set<String>,
    layout: jr.brian.home.esde.model.FrontendLayout,
    useWallpaper: Boolean,
    customizations: Map<String, jr.brian.home.esde.model.SystemCustomization>,
    systemOrder: List<String>,
    hintsVisible: Boolean,
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder
) {
    val baseSystems = rememberSystemTiles(allGames = allGames, hiddenGames = hiddenGames)
    var workingOrder by remember(baseSystems, systemOrder) {
        mutableStateOf(applySystemOrder(baseSystems, systemOrder))
    }
    val initialSystemIndex = remember(workingOrder) {
        val target = romSearchStateHolder.lastFocusedSystem.value
        workingOrder.indexOfFirst { it.systemName == target }.takeIf { it >= 0 } ?: 0
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var customizingSystem by remember { mutableStateOf<String?>(null) }
    var reorderingSystem by remember { mutableStateOf<String?>(null) }

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
            showSettingsDialog -> showSettingsDialog = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
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
        SystemGrid(
            systems = workingOrder,
            isLoading = isLoading,
            layout = layout,
            initialRealIndex = initialSystemIndex,
            backgroundTransparent = useWallpaper,
            customizations = customizations,
            reorderingSystem = reorderingSystem,
            onSystemFocused = { tile ->
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
        FrontendSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    val activeCustomizeTarget = customizingSystem
    if (activeCustomizeTarget != null) {
        SystemCustomizationDialog(
            systemName = activeCustomizeTarget,
            customization = customizations[activeCustomizeTarget]
                ?: jr.brian.home.esde.model.SystemCustomization(),
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
            AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_BUTTON_START -> {
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
        AndroidKeyEvent.KEYCODE_BUTTON_SELECT -> { onOpenSettings(); true }
        AndroidKeyEvent.KEYCODE_BUTTON_START -> { onCustomizeFocused(); true }
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
    hiddenGames: Set<String>
): List<SystemTile> = remember(allGames, hiddenGames) {
    allGames
        .asSequence()
        .filter { hiddenGameKey(it) !in hiddenGames }
        .filter { !it.systemName.equals(ANDROID_APPS_SYSTEM, ignoreCase = true) }
        .groupBy { it.systemName }
        .map { (name, _) -> SystemTile(systemName = name) }
        .sortedBy { it.systemName.lowercase() }
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
                text = stringResource(R.string.frontend_open_settings_hint),
                color = ThemeAccentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.weight(1f))
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
        }
    }
}

