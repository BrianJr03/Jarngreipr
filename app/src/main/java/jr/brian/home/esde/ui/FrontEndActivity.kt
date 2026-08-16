package jr.brian.home.esde.ui

import jr.brian.home.esde.data.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.FrontendSelectionStateHolder
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.WallpaperStateHolder
import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.ui.frontend.FrontendScreen
import jr.brian.home.esde.util.LocalEsdeWallpaperState
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.persistSafTreeForSystem
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.viewmodels.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    @Inject
    lateinit var wallpaperStateHolder: WallpaperStateHolder

    private val viewModel: RomSearchResultsViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val romSearchViewModel: RomSearchViewModel by viewModels()

    private var pendingFolderChangeSystem: String? = null
    private lateinit var romLauncher: RomGameLauncher
    private var gameLaunched = false
    private var mediaMountReceiver: BroadcastReceiver? = null

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
        val systemName =
            pendingFolderChangeSystem ?: romLauncher.pendingGameLaunch?.first?.systemName
        persistSafTreeForSystem(this, esdePrefs, systemName, treeUri)

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
        mediaMountReceiver?.let { runCatching { unregisterReceiver(it) } }
        mediaMountReceiver = null
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
        romSearchStateHolder.currentRoute.value = FrontendRoute.Systems
        romSearchViewModel.loadGames()
        registerMediaMountReceiver()
        observeFrontendEnabledFlag()
        observeShowRomSearchResultsSignal()
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
                    // Mirror MainActivity: read the shared holder inside the
                    // composable so the read is tracked, then re-provide the
                    // current value. Any consumer under FrontEndActivity (e.g.
                    // FrontendTile inside a canvas widget) now sees the same
                    // live state MainActivity's ES-DE event listener writes to.
                    val wallpaperState by wallpaperStateHolder.state
                    CompositionLocalProvider(
                        LocalEsdeWallpaperState provides wallpaperState
                    ) {
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
    }

    /**
     * Kick a re-scan when a removable volume finishes mounting. The launcher
     * runs as HOME, so its first scan often races the SD card mount at boot
     * and comes back "degraded" — the ViewModel keeps the cached SD systems
     * in place, and this receiver is what lets it actually rebuild once vold
     * catches up, so the user never has to open Settings → Refresh Library.
     *
     * `ACTION_MEDIA_MOUNTED` requires a `file` data scheme; declaring it
     * without would silently receive nothing.
     */
    private fun registerMediaMountReceiver() {
        if (mediaMountReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_MEDIA_MOUNTED) {
                    romSearchViewModel.retryIfLastScanWasDegraded()
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_MEDIA_MOUNTED).apply {
            addDataScheme("file")
        }
        registerReceiver(receiver, filter)
        mediaMountReceiver = receiver
    }

    private fun observeFrontendEnabledFlag() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                esdePrefs.state
                    .map { it.frontendEnabled }
                    .distinctUntilChanged()
                    .collect { enabled -> if (!enabled) finish() }
            }
        }
    }

    /**
     * The keyboard on the bottom display used to start [RomSearchResultsActivity]
     * itself, from MainActivity's context, using FLAG_ACTIVITY_NEW_TASK on the top
     * displayId. That cross-display / cross-task launch destroyed this activity and
     * blanked MainActivity during the Y → back cycle. Now the keyboard emits a
     * signal and we start the results activity on our own task on the display we
     * already own — finishing it pops back to this activity without a recreate.
     */
    private fun observeShowRomSearchResultsSignal() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                romSearchStateHolder.showRomSearchResultsSignal.collect {
                    val intent = Intent(
                        this@FrontEndActivity,
                        RomSearchResultsActivity::class.java
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        putExtra(RomSearchResultsActivity.EXTRA_FROM_FRONTEND, true)
                    }
                    startActivity(intent)
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
