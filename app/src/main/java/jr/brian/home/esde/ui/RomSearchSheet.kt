package jr.brian.home.esde.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.clearGameMediaType
import jr.brian.home.esde.data.clearSystemMediaType
import jr.brian.home.esde.data.disableFocusAnimation
import jr.brian.home.esde.data.enableFocusAnimation
import jr.brian.home.esde.data.getGameCore
import jr.brian.home.esde.data.getGameEmulator
import jr.brian.home.esde.data.getGameLaunchCommand
import jr.brian.home.esde.data.hideGame
import jr.brian.home.esde.data.setGameCore
import jr.brian.home.esde.data.setGameEmulator
import jr.brian.home.esde.data.setGameLaunchCommand
import jr.brian.home.esde.data.setGameMediaType
import jr.brian.home.esde.data.setSystemMediaType
import jr.brian.home.esde.data.unhideAllGames
import jr.brian.home.esde.data.unhideGame
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.ui.frontend.rememberFilteredGames
import jr.brian.home.esde.ui.frontend.rememberRomSearchQueryState
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.hiddenGameKey
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.viewmodels.MainViewModel
import java.io.File

private const val ANDROID_APPS_SYSTEM = "androidapps"

/**
 * ROM search as a full-screen overlay on the bottom display (mirror of
 * [jr.brian.home.ui.screens.AppSearchScreen] for apps). Reuses [RomResultsGrid]
 * so tap → [RomGameLauncher.launchGame] and long-press → [RomDetailScreen]
 * pick up all the existing PS2/SAF/grant behavior.
 *
 * Hosted from `MainContent` on the bottom display — the caller supplies a
 * [RomGameLauncher] scoped to the hosting activity. Input goes through the
 * same on-screen [QwertyKeyboard] AppSearchScreen uses, so gamepad D-pad text
 * entry works without an IME popup.
 *
 * NOT a `ModalBottomSheet`: that renders inside a Dialog, which swaps
 * `LocalContext` to a `ContextThemeWrapper`. `QwertyKeyboard` internally
 * does `LocalContext.current as ComponentActivity` (for `hiltViewModel`) and
 * crashes with a `ClassCastException` under that wrapper. A plain [Surface]
 * kept in the host activity's composition preserves the activity context.
 *
 * All `@` command flags come from [rememberRomSearchQueryState] — the same
 * function the legacy activity now calls — so the two paths cannot drift.
 */
@Composable
fun RomSearchSheet(
    esdePrefs: ESDEPreferencesManager,
    romLauncher: RomGameLauncher,
    romSearchStateHolder: RomSearchStateHolder,
    mainViewModel: MainViewModel,
    managers: ManagerContainer,
    onChangeFolder: (GameInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Surface(
        color = OledBackgroundColor,
        modifier = Modifier.fillMaxSize(),
    ) {
        RomSearchSheetBody(
            esdePrefs = esdePrefs,
            romLauncher = romLauncher,
            romSearchStateHolder = romSearchStateHolder,
            mainViewModel = mainViewModel,
            managers = managers,
            onChangeFolder = onChangeFolder,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun RomSearchSheetBody(
    esdePrefs: ESDEPreferencesManager,
    romLauncher: RomGameLauncher,
    romSearchStateHolder: RomSearchStateHolder,
    mainViewModel: MainViewModel,
    managers: ManagerContainer,
    onChangeFolder: (GameInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val allGames by romSearchStateHolder.allGames.collectAsStateWithLifecycle()
    val isLoading by romSearchStateHolder.isLoading.collectAsStateWithLifecycle()
    val esdeState by esdePrefs.state.collectAsStateWithLifecycle()
    val homeUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    val queryTrimmed = query.trim()

    LaunchedEffect(esdeState.romSearchShowAllAndroidApps) {
        if (esdeState.romSearchShowAllAndroidApps) mainViewModel.loadAllApps(context)
    }

    val queryState = rememberRomSearchQueryState(
        queryTrimmed = queryTrimmed,
        romSearchShowAllAndroidApps = esdeState.romSearchShowAllAndroidApps,
        allGames = allGames,
        hiddenGames = esdeState.hiddenGames,
    )

    val allAndroidApps = remember(esdeState.romSearchShowAllAndroidApps, homeUiState.allApps) {
        if (!esdeState.romSearchShowAllAndroidApps) emptyList()
        else homeUiState.allApps.map { appInfo ->
            GameInfo(
                path = appInfo.packageName,
                name = appInfo.label,
                systemName = ANDROID_APPS_SYSTEM,
            )
        }
    }

    val filteredGames = rememberFilteredGames(
        allGames = allGames,
        hiddenGames = esdeState.hiddenGames,
        hideNoMetadata = esdeState.romSearchHideNoMetadata,
        hideNoImage = esdeState.romSearchHideNoImage,
        cardMediaType = esdeState.romSearchCardMediaType,
        queryTrimmed = queryTrimmed,
        selectedPlatform = queryState.selectedPlatform,
        isPlatformMode = queryState.isPlatformMode,
        isHiddenMode = queryState.isHiddenMode,
        isAndroidMode = queryState.isAndroidMode,
        androidModeFilter = queryState.androidModeFilter,
        platformSearch = queryState.platformSearch,
        allAndroidApps = allAndroidApps,
    )

    // See RomResultsGrid: focusResetKey resets grid focus to index 0 when it
    // changes. Bump only on non-blank query changes so clearing the field keeps
    // the user's scroll position.
    var focusResetCounter by remember { mutableIntStateOf(0) }
    LaunchedEffect(queryTrimmed) {
        if (queryTrimmed.isNotBlank()) focusResetCounter++
    }

    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }

    // Details overlay owns the whole sheet — the keyboard would only compete
    // for focus and space. Hide it while RomResultsGrid's details panel is up
    // and restore it when the user backs out.
    var detailsVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            RomResultsGrid(
                games = filteredGames,
                isLoading = isLoading,
                // Single horizontal row so ROM art renders large — the sheet
                // shares vertical space with the on-screen keyboard, so a full
                // grid would shrink tiles to unreadable sizes.
                layout = FrontendLayout.Row,
                focusResetKey = focusResetCounter,
                isHiddenMode = queryState.isHiddenMode,
                cardMediaType = esdeState.romSearchCardMediaType,
                focusAnimationEnabled = esdeState.romSearchDiscSpin,
                focusAnimationDelayMs = esdeState.romSearchFocusAnimationDelayMs,
                isFocusAnimationDisabled = { game ->
                    gameKey(game) in esdeState.romSearchFocusAnimationDisabledGames
                },
                onToggleGameDiscSpin = { game ->
                    val key = gameKey(game)
                    if (key in esdeState.romSearchFocusAnimationDisabledGames)
                        esdePrefs.enableFocusAnimation(key)
                    else esdePrefs.disableFocusAnimation(key)
                },
                getGameMediaType = { game ->
                    esdeState.romSearchGameMediaMap[gameKey(game)]
                        ?.let { runCatching { RomSearchCardMediaType.valueOf(it) }.getOrNull() }
                        ?: esdeState.systemMediaMap[game.systemName]
                            ?.let { runCatching { RomSearchCardMediaType.valueOf(it) }.getOrNull() }
                },
                onSetGameMediaType = { game, type ->
                    if (type == null) esdePrefs.clearGameMediaType(gameKey(game))
                    else esdePrefs.setGameMediaType(gameKey(game), type)
                },
                onSetMediaTypeForSystem = { game, type ->
                    if (type == null) esdePrefs.clearSystemMediaType(game.systemName)
                    else esdePrefs.setSystemMediaType(game.systemName, type)
                    filteredGames.filter { it.systemName == game.systemName }
                        .forEach { esdePrefs.clearGameMediaType(gameKey(it)) }
                },
                onLaunchGame = { game ->
                    val pkg = esdePrefs.getGameEmulator(gameKey(game))
                        ?: game.emulatorPackage ?: game.path
                    romLauncher.launchGame(
                        game,
                        context,
                        managers.ui.appDisplayPreferenceManager.getAppDisplayPreference(pkg),
                    )
                    onDismiss()
                },
                onSaveEmulator = { game, pkg, cmd ->
                    esdePrefs.setGameEmulator(gameKey(game), pkg)
                    cmd?.let { esdePrefs.setGameLaunchCommand(gameKey(game), it) }
                },
                hasSavedEmulator = { game ->
                    esdePrefs.getGameLaunchCommand(gameKey(game)) != null ||
                            esdePrefs.getGameEmulator(gameKey(game)) != null
                },
                onGameFocused = { game ->
                    game?.let { managers.feature.jinglesManager.onGameSelected(File(it.path).name) }
                },
                onHideGame = { game -> esdePrefs.hideGame(hiddenGameKey(game)) },
                onUnhideGame = { game -> esdePrefs.unhideGame(hiddenGameKey(game)) },
                onUnhideAllGames = { games ->
                    esdePrefs.unhideAllGames(games.map { hiddenGameKey(it) })
                },
                onAndroidAppInfo = { game ->
                    val pkg = game.path.trimEnd('/').removeSuffix(".app")
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:$pkg".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                isRetroArchGame = { game ->
                    val saved = esdePrefs.getGameEmulator(gameKey(game))
                    (saved ?: game.emulatorPackage)?.startsWith("com.retroarch") == true
                },
                hasSavedCore = { game -> esdePrefs.getGameCore(gameKey(game)) != null },
                onCoreSelected = { game, _, corePath ->
                    esdePrefs.setGameCore(gameKey(game), corePath)
                    val pkg = esdePrefs.getGameEmulator(gameKey(game))
                        ?: game.emulatorPackage ?: game.path
                    romLauncher.launchGame(
                        game,
                        context,
                        managers.ui.appDisplayPreferenceManager.getAppDisplayPreference(pkg),
                    )
                    onDismiss()
                },
                onChangeFolder = { game ->
                    onChangeFolder(game)
                    onDismiss()
                },
                onDetailsVisibleChanged = { detailsVisible = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!detailsVisible) {
            // Same on-screen keyboard AppSearchScreen uses. showAtKey exposes
            // `@` so gamepad users can type `@hidden`, `@android`,
            // `@<platform>` without an IME popup. Flip-layout / navigate-to-
            // search / settings shortcuts are hidden — this sheet doesn't
            // own those flows.
            //
            // Sheet-only 10% height trim: graphicsLayer scales the pixels
            // (bottom-anchored so the query bar stays at the top of the
            // section), layout reports 90% of measured height back to the
            // parent Column so grid.weight(1f) actually gains the 10%. The
            // QwertyKeyboard composable itself is untouched — this doesn't
            // affect any other place that uses it.
            QwertyKeyboard(
                searchQuery = query,
                onQueryChange = { query = it },
                keyboardFocusRequesters = keyboardFocusRequesters,
                showFlipLayoutButton = false,
                showAtKey = true,
                showController = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleY = 0.9f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val trimmedHeight = (placeable.height * 0.9f).toInt()
                        layout(placeable.width, trimmedHeight) {
                            placeable.place(0, 0)
                        }
                    },
            )
        }
    }
}
