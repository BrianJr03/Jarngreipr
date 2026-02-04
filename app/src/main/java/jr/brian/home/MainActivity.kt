package jr.brian.home

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.events.ESDEEventListenerImpl
import jr.brian.home.esde.events.ESDEEventManager
import jr.brian.home.esde.setup.ESDESetupHelper
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.esde.scripts.ScriptManager
import jr.brian.home.esde.ui.ESDEWallpaperContainer
import java.io.File
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.viewmodels.PowerViewModel
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
                    val wallpaperState by esdeViewModel.wallpaperState
                    LaunchedEffect(Unit) {
                        esdeEventListener.onSystemSelected = { systemName ->
                            esdeViewModel.updateForSystem(systemName)
                        }
                        esdeEventListener.onGameSelected = { gameFilename, _, systemName ->
                            esdeViewModel.updateForGame(systemName, gameFilename)
                        }
                        esdeEventListener.onGameStarted = { _, _, _ ->
                            esdeViewModel.handleGameStarted()
                            powerViewModel.powerOff()
                        }
                        esdeEventListener.onGameEnded = { _, _, _ ->
                            esdeViewModel.handleGameEnded()
                            powerViewModel.powerOn()
                        }
                        esdeEventListener.onScreensaverStarted = {
                            esdeViewModel.handleScreensaverStarted()
                        }
                        esdeEventListener.onScreensaverEnded = { _ ->
                            esdeViewModel.handleScreensaverEnded()
                        }
                        esdeEventListener.onScreensaverGameSelected =
                            { gameFilename, _, systemName ->
                                esdeViewModel.updateForScreensaverGame(systemName, gameFilename)
                            }
                    }
                    ESDEWallpaperContainer(
                        state = wallpaperState,
                        content = {
                            MainContent()
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