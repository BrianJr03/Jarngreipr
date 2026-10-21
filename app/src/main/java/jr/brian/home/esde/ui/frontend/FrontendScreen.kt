package jr.brian.home.esde.ui.frontend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.FrontendSelectionStateHolder
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.ui.RomGameLauncher
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.model.rom.toGameInfo
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
    onChangeFolder: (GameInfo) -> Unit,
    /**
     * True when FrontEndActivity has mounted a [jr.brian.home.ui.components.wallpaper.WallpaperDisplay]
     * underneath this screen — e.g. the user picked a frontend wallpaper or
     * "Launcher" (mirror). Collapses the Surface/background backers so the
     * wallpaper layer below shows through.
     */
    showFrontendWallpaper: Boolean = false,
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
        FrontendNavHost(
            esdePrefs = esdePrefs,
            viewModel = viewModel,
            romSearchStateHolder = romSearchStateHolder,
            frontendSelectionStateHolder = frontendSelectionStateHolder,
            managers = managers,
            romLauncher = romLauncher,
            appDisplayPreferenceManager = appDisplayPreferenceManager,
            onChangeFolder = onChangeFolder,
            showFrontendWallpaper = showFrontendWallpaper,
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
