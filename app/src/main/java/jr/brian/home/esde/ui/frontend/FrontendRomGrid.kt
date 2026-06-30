package jr.brian.home.esde.ui.frontend

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.FrontendSelectionStateHolder
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.ui.RomGameLauncher
import jr.brian.home.esde.ui.RomResultsGrid
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.hiddenGameKey
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import java.io.File

@Composable
internal fun FrontendRomGrid(
    games: List<GameInfo>,
    isLoading: Boolean,
    isHiddenMode: Boolean,
    backgroundTransparent: Boolean,
    cardMediaType: RomSearchCardMediaType,
    focusAnimationEnabled: Boolean,
    focusAnimationDelayMs: Int,
    focusAnimationDisabledGames: Set<String>,
    gameMediaMap: Map<String, String>,
    focusResetKey: Any?,
    esdePrefs: ESDEPreferencesManager,
    viewModel: RomSearchResultsViewModel,
    romSearchStateHolder: RomSearchStateHolder,
    frontendSelectionStateHolder: FrontendSelectionStateHolder,
    managers: ManagerContainer,
    romLauncher: RomGameLauncher,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onChangeFolder: (GameInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    RomResultsGrid(
        games = games,
        isLoading = isLoading,
        focusResetKey = focusResetKey,
        isHiddenMode = isHiddenMode,
        backgroundTransparent = backgroundTransparent,
        cardMediaType = cardMediaType,
        focusAnimationEnabled = focusAnimationEnabled,
        focusAnimationDelayMs = focusAnimationDelayMs,
        isFocusAnimationDisabled = { game -> gameKey(game) in focusAnimationDisabledGames },
        onToggleGameDiscSpin = { game ->
            val key = gameKey(game)
            if (key in focusAnimationDisabledGames) esdePrefs.enableFocusAnimation(key)
            else esdePrefs.disableFocusAnimation(key)
        },
        getGameMediaType = { game ->
            gameMediaMap[gameKey(game)]
                ?.let { runCatching { RomSearchCardMediaType.valueOf(it) }.getOrNull() }
        },
        onSetGameMediaType = { game, type ->
            if (type == null) esdePrefs.clearGameMediaType(gameKey(game))
            else esdePrefs.setGameMediaType(gameKey(game), type)
        },
        modifier = modifier,
        onLaunchGame = { game ->
            if (romSearchStateHolder.isSelectMode.value) {
                romSearchStateHolder.pendingRomForPin.value =
                    romSearchStateHolder.pendingSelectPageIndex.value to game
                romSearchStateHolder.isSelectMode.value = false
                romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                romSearchStateHolder.dismissSignal.tryEmit(Unit)
            } else {
                romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                val pkg = esdePrefs.getGameEmulator(gameKey(game))
                    ?: game.emulatorPackage ?: game.path
                romLauncher.launchGame(
                    game,
                    context,
                    appDisplayPreferenceManager.getAppDisplayPreference(pkg)
                )
            }
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
            viewModel.updateFocusedGame(game)
            if (game != null) {
                managers.feature.jinglesManager.onGameSelected(File(game.path).name)
                frontendSelectionStateHolder.selectGame(game)
            }
        },
        onHideGame = { game -> esdePrefs.hideGame(hiddenGameKey(game)) },
        onUnhideGame = { game -> esdePrefs.unhideGame(hiddenGameKey(game)) },
        onUnhideAllGames = { gamesToUnhide ->
            esdePrefs.unhideAllGames(gamesToUnhide.map { hiddenGameKey(it) })
        },
        onToggleHintAndKeyboard = {
            val newVisible = !romSearchStateHolder.hintAndKbVisible.value
            romSearchStateHolder.hintAndKbVisible.value = newVisible
            esdePrefs.setRomSearchHintsKbVisible(newVisible)
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
                appDisplayPreferenceManager.getAppDisplayPreference(pkg)
            )
        },
        onChangeFolder = onChangeFolder
    )
}
