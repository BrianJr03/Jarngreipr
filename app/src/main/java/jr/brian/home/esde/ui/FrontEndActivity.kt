package jr.brian.home.esde.ui

import jr.brian.home.esde.data.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.FrontendSelectionStateHolder
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.ui.frontend.FrontendScreen
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.viewmodels.MainViewModel
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class FrontEndActivity : ComponentActivity() {
    @Inject
    lateinit var managers: ManagerContainer

    @Inject
    lateinit var esdePrefs: ESDEPreferencesManager

    @Inject
    lateinit var romSearchStateHolder: RomSearchStateHolder

    @Inject
    lateinit var frontendSelectionStateHolder: FrontendSelectionStateHolder

    private val viewModel: RomSearchResultsViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val romSearchViewModel: RomSearchViewModel by viewModels()

    private var pendingFolderChangeSystem: String? = null
    private lateinit var romLauncher: RomGameLauncher
    private var gameLaunched = false

    private val safTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri -> handleSafTreeResult(treeUri) }

    private fun handleSafTreeResult(treeUri: Uri?) {
        if (treeUri == null) {
            Toast.makeText(this, "Folder access denied", Toast.LENGTH_SHORT).show()
            romLauncher.pendingGameLaunch = null
            pendingFolderChangeSystem = null
            return
        }
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val systemName =
            pendingFolderChangeSystem ?: romLauncher.pendingGameLaunch?.first?.systemName

        val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()

        if (treeDocId?.startsWith("primary:") == true) {
            val rel = treeDocId.removePrefix("primary:")
            val pickedDir = "/storage/emulated/0/$rel"
            val romsRoot = if (systemName != null &&
                File(pickedDir).name.equals(systemName, ignoreCase = true)
            ) {
                File(pickedDir).parent ?: pickedDir
            } else {
                pickedDir
            }
            esdePrefs.addRomsPath(romsRoot)
            if (systemName != null) esdePrefs.setSafTreeUri(systemName, treeUri.toString())
        } else if (systemName != null) {
            esdePrefs.setSafTreeUri(systemName, treeUri.toString())
        }

        romLauncher.pendingGameLaunch?.let { (game, ctx) ->
            val pkg = esdePrefs.getGameEmulator(gameKey(game)) ?: game.emulatorPackage ?: game.path
            romLauncher.launchGame(
                game,
                ctx,
                managers.ui.appDisplayPreferenceManager.getAppDisplayPreference(pkg)
            )
        }
        romLauncher.pendingGameLaunch = null
        pendingFolderChangeSystem = null
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onStart() {
        super.onStart()
        isRunning = true
    }

    override fun onStop() {
        super.onStop()
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (gameLaunched) {
            romSearchStateHolder.gameLaunchSignal.tryEmit(Unit)
        }
        viewModel.clearState()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        romLauncher = RomGameLauncher(
            activity = this,
            esdePrefs = esdePrefs,
            onSignalGameLaunch = ::signalGameLaunch,
            onLaunchSafPicker = { uri -> safTreeLauncher.launch(uri) }
        )
        romSearchStateHolder.hintAndKbVisible.value = esdePrefs.state.value.romSearchHintsKbVisible
        romSearchStateHolder.currentRoute.value = FrontendRoute.Systems
        romSearchViewModel.loadGames()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            LauncherTheme {
                BackHandler {}
                managers.ManagerCompositionLocalProvider {
                    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
                    FrontendScreen(
                        esdePrefs = esdePrefs,
                        viewModel = viewModel,
                        mainViewModel = mainViewModel,
                        romSearchStateHolder = romSearchStateHolder,
                        frontendSelectionStateHolder = frontendSelectionStateHolder,
                        managers = managers,
                        romLauncher = romLauncher,
                        appDisplayPreferenceManager = appDisplayPreferenceManager,
                        onFinishImmediately = { finish() },
                        onSignalGameLaunch = ::signalGameLaunch,
                        onChangeFolder = ::launchSafPickerForGame
                    )
                }
            }
        }
    }

    private fun launchSafPickerForGame(game: GameInfo) {
        pendingFolderChangeSystem = game.systemName
        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths)
        val hint = romPath?.let {
            val dir = File(it).parent ?: "/storage/emulated/0"
            "content://com.android.externalstorage.documents/document/${
                Uri.encode("primary:${dir.removePrefix("/storage/emulated/0/")}")
            }".toUri()
        }
        safTreeLauncher.launch(hint)
    }

    private fun signalGameLaunch() {
        gameLaunched = true
        managers.feature.jinglesManager.onGameLaunched()
    }
}
