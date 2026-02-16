package jr.brian.home

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.events.ESDEEventListenerImpl
import jr.brian.home.esde.events.ESDEEventManager
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.preferences.ScreensaverBehavior
import jr.brian.home.esde.scripts.ScriptManager
import jr.brian.home.esde.setup.ESDESetupHelper
import jr.brian.home.esde.ui.ESDEWallpaperContainer
import jr.brian.home.esde.ui.VideoPlayerActivity
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.model.VideoLaunchEvent
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.viewmodels.PowerViewModel
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var managers: ManagerContainer

    @Inject
    lateinit var esdeEventManager: ESDEEventManager

    @Inject
    lateinit var esdeEventListener: ESDEEventListenerImpl

    private var esdeViewModelRef: ESDEViewModel? = null

    private val videoPlayerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            esdeViewModelRef?.onVideoActivityFinished()
        }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        esdeEventManager.startWatching()
        esdeEventManager.startPolling()
        checkAndCreateScripts()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            LauncherTheme {
                managers.ManagerCompositionLocalProvider {
                    val esdeViewModel: ESDEViewModel = hiltViewModel()
                    val powerViewModel: PowerViewModel = hiltViewModel()
                    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
                    val wallpaperState by esdeViewModel.wallpaperState
                    val esdePreferencesManager = LocalESDEPreferencesManager.current
                    var triggerMarqueePressShortcut by remember { mutableStateOf(false) }
                    var isAnyOverlayVisible by remember { mutableStateOf(false) }
                    var currentPageIndex by remember { mutableStateOf(0) }
                    var pagerScrollProgress by remember { mutableStateOf(0f) }
                    var hideLauncherUIForScreensaver by remember { mutableStateOf(false) }
                    var hideLauncherUIForGameBrowsing by remember { mutableStateOf(false) }
                    var dockTopY by remember { mutableStateOf<Float?>(null) }
                    val prefsState by esdePreferencesManager.state.collectAsStateWithLifecycle()
                    val isMarqueeVisibleOnPage = prefsState.isMarqueeVisibleOnPage(currentPageIndex)
                    val shouldHideMarquee = isAnyOverlayVisible || !isMarqueeVisibleOnPage

                    ObserveESDEViewModel(esdeViewModel)

                    ObserveVideoLaunchEvents(
                        esdeViewModel = esdeViewModel,
                        powerViewModel = powerViewModel
                    )

                    SetupESDEEventListeners(
                        esdeViewModel = esdeViewModel,
                        powerViewModel = powerViewModel,
                        esdePreferencesManager = esdePreferencesManager,
                        onScreensaverUIVisibilityChanged = { hideLauncherUIForScreensaver = it },
                        onGameBrowsingUIVisibilityChanged = { hideLauncherUIForGameBrowsing = it }
                    )

                    ESDEWallpaperContainer(
                        state = wallpaperState,
                        onOpenMarqueeShortcut = { triggerMarqueePressShortcut = true },
                        onWallpaperClick = null,
                        onWallpaperDoubleClick = when {
                            wallpaperState.isScreensaverActive -> {
                                { hideLauncherUIForScreensaver = !hideLauncherUIForScreensaver }
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
                            MainContent(
                                triggerMarqueePressShortcut = triggerMarqueePressShortcut,
                                onMarqueePressShortcutHandled = {
                                    triggerMarqueePressShortcut = false
                                },
                                onAnyOverlayVisibleChanged = { isAnyOverlayVisible = it },
                                onCurrentPageChanged = { currentPageIndex = it },
                                onPagerScrollProgressChanged = { pagerScrollProgress = it },
                                onDockPositioned = { y -> dockTopY = y },
                                hideLauncherUI = (wallpaperState.isScreensaverActive && hideLauncherUIForScreensaver) ||
                                        (prefsState.hideUIForGameBrowsing && hideLauncherUIForGameBrowsing)
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onStart() {
        super.onStart()
        esdeViewModelRef?.musicController?.onActivityVisible()
    }

    override fun onStop() {
        super.onStop()
        esdeViewModelRef?.musicController?.onActivityInvisible()
    }

    override fun onDestroy() {
        super.onDestroy()
        esdeEventManager.stopWatching()
    }

    private fun checkAndCreateScripts() {
        if (!ESDESetupHelper.hasStoragePermission(this)) {
            ESDESetupHelper.requestStoragePermission(this)
            return
        }

        val scriptsDir = File(ScriptManager.DEFAULT_SCRIPTS_PATH)
        if (!ScriptManager.areScriptsValid(scriptsDir)) {
            val setupResult = ESDESetupHelper.initializeESDEIntegration(this)
            if (setupResult.success) {
                Toast.makeText(
                    this,
                    "ES-DE scripts created successfully!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    setupResult.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun launchVideoPlayer(event: VideoLaunchEvent) {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, event.videoPath)
            putExtra(VideoPlayerActivity.EXTRA_AUDIO_ENABLED, event.audioEnabled)
            putExtra(VideoPlayerActivity.EXTRA_SCALE_MODE, event.scaleMode.name)
        }
        videoPlayerLauncher.launch(intent)
    }


    @Composable
    private fun ObserveESDEViewModel(esdeViewModel: ESDEViewModel) {
        LaunchedEffect(esdeViewModel) {
            esdeViewModelRef = esdeViewModel
        }
    }

    @Composable
    private fun ObserveVideoLaunchEvents(
        esdeViewModel: ESDEViewModel,
        powerViewModel: PowerViewModel
    ) {
        LaunchedEffect(esdeViewModel) {
            esdeViewModel.videoLaunchEvent.collect { event ->
                val isGameRunning = esdeViewModel.wallpaperState.value.isGameRunning
                if (!powerViewModel.isPoweredOff.value && !isGameRunning) {
                    launchVideoPlayer(event)
                }
            }
        }
    }

    @Composable
    private fun SetupESDEEventListeners(
        esdeViewModel: ESDEViewModel,
        powerViewModel: PowerViewModel,
        esdePreferencesManager: ESDEPreferencesManager,
        onScreensaverUIVisibilityChanged: (Boolean) -> Unit,
        onGameBrowsingUIVisibilityChanged: (Boolean) -> Unit
    ) {
        LaunchedEffect(Unit) {
            esdeEventListener.onSystemSelected = { systemName ->
                VideoPlayerActivity.finishIfRunning()
                esdeViewModel.updateForSystem(systemName)
                onGameBrowsingUIVisibilityChanged(false)
            }
            esdeEventListener.onGameSelected = { gameFilename, _, systemName ->
                VideoPlayerActivity.finishIfRunning()
                esdeViewModel.updateForGame(systemName, gameFilename)
                onGameBrowsingUIVisibilityChanged(true)
            }
            esdeEventListener.onGameStarted = { _, _, _ ->
                VideoPlayerActivity.finishIfRunning()
                esdeViewModel.handleGameStarted()
                if (esdePreferencesManager.state.value.powerEventsEnabled) {
                    powerViewModel.powerOff()
                } else if (esdePreferencesManager.state.value.persistOnGameLaunch) {
                    powerViewModel.setGamePersistActive(true)
                }
            }
            esdeEventListener.onGameEnded = { _, _, _ ->
                esdeViewModel.handleGameEnded()
                if (esdePreferencesManager.state.value.powerEventsEnabled) {
                    powerViewModel.powerOn()
                }
                powerViewModel.setGamePersistActive(false)
            }
            esdeEventListener.onScreensaverStarted = {
                VideoPlayerActivity.finishIfRunning()
                onScreensaverUIVisibilityChanged(true)
                esdeViewModel.handleScreensaverStarted()
                onGameBrowsingUIVisibilityChanged(false)
                if (esdePreferencesManager.state.value.screensaverBehavior == ScreensaverBehavior.PowerOff) {
                    powerViewModel.powerOff()
                }
            }
            esdeEventListener.onScreensaverEnded = { _ ->
                onScreensaverUIVisibilityChanged(false)
                esdeViewModel.handleScreensaverEnded()
                onGameBrowsingUIVisibilityChanged(false)
                if (esdePreferencesManager.state.value.screensaverBehavior == ScreensaverBehavior.PowerOff) {
                    powerViewModel.powerOn()
                }
            }
            esdeEventListener.onScreensaverGameSelected =
                { gameFilename, _, systemName ->
                    esdeViewModel.updateForScreensaverGame(systemName, gameFilename)
                    // Don't hide UI during screensaver
                    onGameBrowsingUIVisibilityChanged(false)
                }
        }
    }
}