package jr.brian.home

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.PingBroadcastManager
import jr.brian.home.esde.data.ESDEEventListenerImpl
import jr.brian.home.esde.data.ESDEEventManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.ESDESetupHelper
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.data.ScriptManager
import jr.brian.home.esde.model.ScreensaverBehavior
import jr.brian.home.esde.model.SystemLaunchTrigger
import jr.brian.home.esde.ui.ESDEWallpaperContainer
import jr.brian.home.esde.ui.VideoPlayerActivity
import jr.brian.home.esde.viewmodels.ESDEViewModel
import jr.brian.home.model.VideoLaunchEvent
import jr.brian.home.ui.components.konfetti.KonfettiPreset
import jr.brian.home.ui.components.konfetti.KonfettiShapeFactory
import jr.brian.home.ui.components.konfetti.LetterFormationBurst
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalGameKonfettiManager
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.util.launchApp
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.ping.PingPermissions.hasPingPermissions
import jr.brian.pingnearby.PingNearbyPermissions.hasNearbyPermissions
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
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

    @Inject
    lateinit var pingBroadcastManager: PingBroadcastManager

    private val esdeViewModel: ESDEViewModel by viewModels()

    private val videoPlayerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            esdeViewModel.onVideoActivityFinished()
        }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }

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
                    val context = LocalContext.current
                    val powerViewModel: PowerViewModel = hiltViewModel()
                    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
                    val wallpaperState by esdeViewModel.wallpaperState
                    val themeManager = LocalThemeManager.current
                    val esdePreferencesManager = LocalESDEPreferencesManager.current
                    var triggerMarqueePressShortcut by remember { mutableStateOf(false) }
                    var isAnyOverlayVisible by remember { mutableStateOf(false) }
                    var currentPageIndex by remember { mutableIntStateOf(0) }
                    var pagerScrollProgress by remember { mutableFloatStateOf(0f) }
                    var hideLauncherUIForScreensaver by remember { mutableStateOf(false) }
                    var hideLauncherUIForGameBrowsing by remember { mutableStateOf(false) }
                    var dockTopY by remember { mutableStateOf<Float?>(null) }
                    val prefsState by esdePreferencesManager.state.collectAsStateWithLifecycle()
                    val isMarqueeVisibleOnPage = prefsState.isMarqueeVisibleOnPage(currentPageIndex)
                    val shouldHideMarquee = isAnyOverlayVisible || !isMarqueeVisibleOnPage
                    
                    LaunchedEffect(Unit) {
                        if (context.hasPingPermissions() && themeManager.isPingAutoStart) {
                            themeManager.shareCurrentTheme()
                        }
                        if (context.hasNearbyPermissions() && themeManager.isWallpaperNearbyAutoStart) {
                            themeManager.startWallpaperNearby()
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            themeManager.stopSharing()
                            themeManager.stopWallpaperNearby()
                        }
                    }

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

                    GameKonfettiOverlay(esdeViewModel = esdeViewModel)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onStart() {
        super.onStart()
        esdeViewModel.musicController.onActivityVisible()
    }

    override fun onResume() {
        super.onResume()
        esdeViewModel.musicController.onActivityResumed()
        esdeViewModel.onVideoActivityFinished()
    }

    override fun onStop() {
        super.onStop()
        esdeViewModel.musicController.onActivityInvisible()
    }

    override fun onDestroy() {
        super.onDestroy()
        esdeEventManager.stopWatching()
    }

    @UnstableApi
    private fun launchVideoPlayer(event: VideoLaunchEvent) {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, event.videoPath)
            putExtra(VideoPlayerActivity.EXTRA_AUDIO_ENABLED, event.audioEnabled)
            putExtra(VideoPlayerActivity.EXTRA_SCALE_MODE, event.scaleMode.name)
            putExtra(VideoPlayerActivity.EXTRA_OVERLAY_ENABLED, event.overlayEnabled)
        }

        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val externalDisplay = displayManager.displays.firstOrNull {
            it.displayId != Display.DEFAULT_DISPLAY
        }

        if (externalDisplay != null) {
            val options = ActivityOptions.makeBasic().apply {
                launchDisplayId = externalDisplay.displayId
            }.toBundle()
            startActivity(intent, options)
        } else {
            videoPlayerLauncher.launch(intent)
        }
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
                Toast.makeText(this, "ES-DE scripts created successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, setupResult.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    @OptIn(UnstableApi::class)
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

    @OptIn(UnstableApi::class)
    @Composable
    private fun SetupESDEEventListeners(
        esdeViewModel: ESDEViewModel,
        powerViewModel: PowerViewModel,
        esdePreferencesManager: ESDEPreferencesManager,
        onScreensaverUIVisibilityChanged: (Boolean) -> Unit,
        onGameBrowsingUIVisibilityChanged: (Boolean) -> Unit
    ) {
        LaunchedEffect(Unit) {
            fun launchSystemAppIfTriggered(systemName: String, trigger: SystemLaunchTrigger) {
                if (esdePreferencesManager.getSystemLaunchTrigger(systemName) == trigger) {
                    esdePreferencesManager.getSystemAppForSystem(systemName)?.let { pkg ->
                        val displayPref = if (!esdePreferencesManager.isSystemBottomScreen(systemName)) {
                            DisplayPreference.PRIMARY_DISPLAY
                        } else {
                            DisplayPreference.CURRENT_DISPLAY
                        }
                        launchApp(
                            context = this@MainActivity,
                            packageName = pkg,
                            displayPreference = displayPref
                        )
                    }
                }
            }

            esdeEventListener.onSystemSelected = { systemName ->
                VideoPlayerActivity.finishIfRunning()
                esdeViewModel.updateForSystem(systemName)
                onGameBrowsingUIVisibilityChanged(false)
                launchSystemAppIfTriggered(systemName, SystemLaunchTrigger.SystemSelect)
            }
            esdeEventListener.onGameSelected = { gameFilename, _, systemName ->
                VideoPlayerActivity.finishIfRunning()
                esdeViewModel.updateForGame(systemName, gameFilename)
                onGameBrowsingUIVisibilityChanged(true)
                launchSystemAppIfTriggered(systemName, SystemLaunchTrigger.GameSelect)
            }
            esdeEventListener.onGameStarted = { gameFilename, _, systemName ->
                VideoPlayerActivity.finishIfRunning()
                esdeViewModel.handleGameStarted(gameFilename)
                if (esdePreferencesManager.state.value.powerEventsEnabled) {
                    powerViewModel.powerOff()
                } else if (esdePreferencesManager.state.value.persistOnGameLaunch) {
                    powerViewModel.setGamePersistActive(true)
                }
                launchSystemAppIfTriggered(systemName, SystemLaunchTrigger.GameStart)
            }
            esdeEventListener.onGameEnded = { _, _, _ ->
                VideoPlayerActivity.finishIfRunning()
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
                VideoPlayerActivity.finishIfRunning()
                onScreensaverUIVisibilityChanged(false)
                esdeViewModel.handleScreensaverEnded()
                onGameBrowsingUIVisibilityChanged(false)
                if (esdePreferencesManager.state.value.screensaverBehavior == ScreensaverBehavior.PowerOff) {
                    powerViewModel.powerOn()
                }
            }
            esdeEventListener.onScreensaverGameSelected = { gameFilename, _, systemName ->
                VideoPlayerActivity.finishIfRunning()
                esdeViewModel.updateForScreensaverGame(systemName, gameFilename)
                onGameBrowsingUIVisibilityChanged(false)
            }
        }
    }

    @Composable
    private fun GameKonfettiOverlay(esdeViewModel: ESDEViewModel) {
        val context = LocalContext.current
        val gameKonfettiManager = LocalGameKonfettiManager.current

        val primaryArgb = ThemePrimaryColor.toArgb()
        val secondaryArgb = ThemeSecondaryColor.toArgb()
        val accentArgb = ThemeAccentColor.toArgb()

        val currentThemeColors by rememberUpdatedState(
            listOf(primaryArgb, secondaryArgb, accentArgb)
        )

        var konfettiParties by remember { mutableStateOf<List<Party>?>(null) }
        var letterBurstEvent by remember { mutableStateOf<LetterBurstState?>(null) }
        var animationKey by remember { mutableIntStateOf(0) }

        LaunchedEffect(esdeViewModel) {
            esdeViewModel.gameKonfettiEvent.collect { event ->
                val config = gameKonfettiManager.config
                if (!config.enabled) return@collect
                if (event.trigger != config.trigger) return@collect

                val gameFilename = event.gameFilename
                val themeColors = currentThemeColors

                konfettiParties = null
                letterBurstEvent = null
                animationKey++

                if (config.isLetterBurst) {
                    val char = gameKonfettiManager.resolveLetterBurstChar(gameFilename)
                    letterBurstEvent = LetterBurstState(
                        char = char,
                        colors = themeColors,
                        burstPreset = gameKonfettiManager.letterBurstExplodePreset(),
                        formationMs = config.letterBurstFormationMs,
                        holdMs = config.letterBurstHoldMs,
                        particleCount = config.letterBurstParticleCount
                    )
                } else {
                    val charShape = if (gameKonfettiManager.config.useCharShape) {
                        val firstChar = gameFilename
                            .substringAfterLast("/")
                            .substringBeforeLast(".")
                            .firstOrNull()
                            ?.uppercaseChar() ?: 'G'
                        KonfettiShapeFactory.createCharShape(context, firstChar)
                    } else null

                    konfettiParties = gameKonfettiManager.buildParties(themeColors, charShape)
                }
            }
        }

        konfettiParties?.let { parties ->
            key(animationKey) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = parties,
                    updateListener = object : OnParticleSystemUpdateListener {
                        override fun onParticleSystemEnded(
                            system: PartySystem,
                            activeSystems: Int
                        ) {
                            if (activeSystems == 0) konfettiParties = null
                        }
                    }
                )
            }
        }

        letterBurstEvent?.let { state ->
            key(animationKey) {
                LetterFormationBurst(
                    char = state.char,
                    colors = state.colors,
                    burstPreset = state.burstPreset,
                    formationDurationMs = state.formationMs,
                    holdDurationMs = state.holdMs,
                    particleCount = state.particleCount,
                    onComplete = { letterBurstEvent = null }
                )
            }
        }
    }

    private data class LetterBurstState(
        val char: Char,
        val colors: List<Int>,
        val burstPreset: KonfettiPreset,
        val formationMs: Int,
        val holdMs: Long,
        val particleCount: Int
    )
}