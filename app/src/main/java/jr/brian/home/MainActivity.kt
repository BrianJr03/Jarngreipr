package jr.brian.home

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.events.ESDEEventListenerImpl
import jr.brian.home.esde.events.ESDEEventManager
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.preferences.ScreensaverBehavior
import jr.brian.home.model.PageType
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.esde.scripts.ScriptManager
import jr.brian.home.esde.setup.ESDESetupHelper
import jr.brian.home.esde.ui.ESDEWallpaperContainer
import jr.brian.home.esde.viewmodel.ESDEViewModel
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
                    
                    LaunchedEffect(Unit) {
                        esdeEventListener.onSystemSelected = { systemName ->
                            esdeViewModel.updateForSystem(systemName)
                        }
                        esdeEventListener.onGameSelected = { gameFilename, _, systemName ->
                            esdeViewModel.updateForGame(systemName, gameFilename)
                        }
                        esdeEventListener.onGameStarted = { _, _, _ ->
                            esdeViewModel.handleGameStarted()
                            if (esdePreferencesManager.state.value.powerEventsEnabled) {
                                powerViewModel.powerOff()
                            }
                        }
                        esdeEventListener.onGameEnded = { _, _, _ ->
                            esdeViewModel.handleGameEnded()
                            if (esdePreferencesManager.state.value.powerEventsEnabled) {
                                powerViewModel.powerOn()
                            }
                        }
                        esdeEventListener.onScreensaverStarted = {
                            esdeViewModel.handleScreensaverStarted()
                            if (esdePreferencesManager.state.value.screensaverBehavior == ScreensaverBehavior.PowerOff) {
                                powerViewModel.powerOff()
                            }
                        }
                        esdeEventListener.onScreensaverEnded = { _ ->
                            esdeViewModel.handleScreensaverEnded()
                            if (esdePreferencesManager.state.value.screensaverBehavior == ScreensaverBehavior.PowerOff) {
                                powerViewModel.powerOn()
                            }
                        }
                        esdeEventListener.onScreensaverGameSelected =
                            { gameFilename, _, systemName ->
                                esdeViewModel.updateForScreensaverGame(systemName, gameFilename)
                            }
                    }
                    val prefsState by esdePreferencesManager.state.collectAsState()
                    val pageTypeManager = LocalPageTypeManager.current
                    val pageTypes by pageTypeManager.pageTypes.collectAsState()
                    val isAppDrawerTab = pageTypes.getOrNull(currentPageIndex) == PageType.APP_DRAWER_TAB
                    val isMarqueeVisibleOnPage = prefsState.isMarqueeVisibleOnPage(currentPageIndex)
                    val overlayMode = !isAppDrawerTab && prefsState.isMarqueeOverlayOnPage(currentPageIndex)
                    val shouldHideMarquee = isAnyOverlayVisible || !isMarqueeVisibleOnPage

                    ESDEWallpaperContainer(
                        state = wallpaperState,
                        onMarqueeLongClick = if (overlayMode) {{ triggerMarqueePressShortcut = true }} else null,
                        hideMarquee = shouldHideMarquee || isPoweredOff,
                        pagerScrollProgress = pagerScrollProgress,
                        overlayMode = overlayMode,
                        currentPageIndex = currentPageIndex,
                        content = {
                            MainContent(
                                triggerMarqueePressShortcut = triggerMarqueePressShortcut,
                                onMarqueePressShortcutHandled = { triggerMarqueePressShortcut = false },
                                onAnyOverlayVisibleChanged = { isAnyOverlayVisible = it },
                                onCurrentPageChanged = { currentPageIndex = it },
                                onPagerScrollProgressChanged = { pagerScrollProgress = it }
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
}