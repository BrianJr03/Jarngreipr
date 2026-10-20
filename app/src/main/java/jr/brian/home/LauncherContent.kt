package jr.brian.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.home.data.LauncherUiStateHolder
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.WallpaperStateHolder
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.ui.ESDEWallpaperContainer
import jr.brian.home.esde.ui.RomGameLauncher
import jr.brian.home.esde.util.LocalEsdeWallpaperState
import jr.brian.home.viewmodels.PowerViewModel

/**
 * Shared launcher-side content. Replaces the inline wallpaper +
 * MainContent wiring that used to live in [MainActivity.setContent]
 * so both host activities can render the launcher identically — the
 * pinned-slot swap moves the launcher content between activities,
 * and the Compose state had been stuck on whichever activity defined
 * the `remember`-locals.
 *
 * All transient UI state (page index, dock Y, overlay flags) lives
 * in app-scoped [LauncherUiStateHolder], so the two possible host
 * compositions see the same state no matter which one is currently
 * rendering the launcher.
 */
@OptIn(UnstableApi::class)
@Composable
fun LauncherContent(
    managers: ManagerContainer,
    romSearchStateHolder: RomSearchStateHolder,
    romLauncher: RomGameLauncher,
    onChangeFolder: (GameInfo) -> Unit,
    navigateToThemeShare: Boolean,
    onNavigateToThemeShareHandled: () -> Unit,
) {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LauncherContentEntryPoint::class.java,
        )
    }
    val launcherState = entryPoint.launcherUiStateHolder()
    val wallpaperStateHolder = entryPoint.wallpaperStateHolder()

    val powerViewModel: PowerViewModel = hiltViewModel()
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    val wallpaperState by wallpaperStateHolder.state
    val esdePreferencesManager = LocalESDEPreferencesManager.current
    val prefsState by esdePreferencesManager.state.collectAsStateWithLifecycle()

    val triggerMarqueePressShortcut by launcherState.triggerMarqueePressShortcut
        .collectAsStateWithLifecycle()
    val isAnyOverlayVisible by launcherState.isAnyOverlayVisible.collectAsStateWithLifecycle()
    val currentPageIndex by launcherState.currentPageIndex.collectAsStateWithLifecycle()
    val pagerScrollProgress by launcherState.pagerScrollProgress.collectAsStateWithLifecycle()
    val hideLauncherUIForScreensaver by launcherState.hideLauncherUIForScreensaver
        .collectAsStateWithLifecycle()
    val hideLauncherUIForGameBrowsing by launcherState.hideLauncherUIForGameBrowsing
        .collectAsStateWithLifecycle()
    val dockTopY by launcherState.dockTopY.collectAsStateWithLifecycle()

    val isMarqueeVisibleOnPage = prefsState.isMarqueeVisibleOnPage(currentPageIndex)
    val shouldHideMarquee = isAnyOverlayVisible || !isMarqueeVisibleOnPage

    ESDEWallpaperContainer(
        state = wallpaperState,
        onOpenMarqueeShortcut = { launcherState.triggerMarquee() },
        onWallpaperClick = null,
        onWallpaperDoubleClick = when {
            wallpaperState.isScreensaverActive -> {
                {
                    launcherState.setHideLauncherUIForScreensaver(!hideLauncherUIForScreensaver)
                }
            }
            wallpaperState.isGameRunning && prefsState.persistOnGameLaunch -> {
                { powerViewModel.togglePower() }
            }
            else -> null
        },
        hideMarquee = shouldHideMarquee || isPoweredOff,
        pagerScrollProgress = pagerScrollProgress,
        currentPageIndex = currentPageIndex,
        dockTopY = dockTopY,
        content = {
            CompositionLocalProvider(LocalEsdeWallpaperState provides wallpaperState) {
                MainContent(
                    romSearchStateHolder = romSearchStateHolder,
                    managers = managers,
                    romLauncher = romLauncher,
                    onChangeFolder = onChangeFolder,
                    triggerMarqueePressShortcut = triggerMarqueePressShortcut,
                    onMarqueePressShortcutHandled = { launcherState.markMarqueeHandled() },
                    navigateToThemeShare = navigateToThemeShare,
                    onNavigateToThemeShareHandled = onNavigateToThemeShareHandled,
                    onAnyOverlayVisibleChanged = { launcherState.setAnyOverlayVisible(it) },
                    onCurrentPageChanged = { launcherState.setCurrentPageIndex(it) },
                    onPagerScrollProgressChanged = { launcherState.setPagerScrollProgress(it) },
                    onDockPositioned = { launcherState.setDockTopY(it) },
                    hideLauncherUI = (wallpaperState.isScreensaverActive && hideLauncherUIForScreensaver) ||
                        (prefsState.hideUIForGameBrowsing && hideLauncherUIForGameBrowsing),
                )
            }
        },
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface LauncherContentEntryPoint {
    fun launcherUiStateHolder(): LauncherUiStateHolder
    fun wallpaperStateHolder(): WallpaperStateHolder
}
