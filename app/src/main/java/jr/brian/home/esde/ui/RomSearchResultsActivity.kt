package jr.brian.home.esde.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.OledBackgroundColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.core.content.FileProvider
import androidx.core.net.toUri

@AndroidEntryPoint
class RomSearchResultsActivity : ComponentActivity() {

    @Inject
    lateinit var managers: ManagerContainer

    @Inject
    lateinit var esdePrefs: ESDEPreferencesManager

    @Inject
    lateinit var romSearchStateHolder: RomSearchStateHolder

    private val viewModel: RomSearchResultsViewModel by viewModels()

    // Holds a game launch that needs SAF access before it can proceed.
    private var pendingGameLaunch: Pair<GameInfo, Context>? = null
    // Holds the system name for a manual folder-change request (no auto-launch after grant).
    private var pendingFolderChangeSystem: String? = null

    private val safTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) {
            Toast.makeText(this, "Folder access denied", Toast.LENGTH_SHORT).show()
            pendingGameLaunch = null
            pendingFolderChangeSystem = null
            return@registerForActivityResult
        }
        // Persist read permission so we don't need to prompt again next time.
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Store the tree URI keyed by system name so each platform has its own independent grant.
        val systemName = pendingFolderChangeSystem ?: pendingGameLaunch?.first?.systemName
        if (systemName != null) {
            esdePrefs.setSafTreeUri(systemName, treeUri.toString())
            Log.d("RomSearchResults", "SAF tree granted | system=$systemName | treeUri=$treeUri")
        }
        // Retry the pending launch if this grant came from an auto-launch request.
        pendingGameLaunch?.let { (game, ctx) -> launchGame(game, ctx) }
        pendingGameLaunch = null
        pendingFolderChangeSystem = null
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                managers.ManagerCompositionLocalProvider {
                    var isVisible by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    val animDurationEnter = 220
                    val animDurationExit = 180

                    fun dismiss() {
                        isVisible = false
                        scope.launch {
                            delay(animDurationExit.toLong())
                            finish()
                        }
                    }

                    LaunchedEffect(Unit) { isVisible = true }

                    val query by viewModel.query.collectAsStateWithLifecycle()
                    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
                    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.dismissSignal.collect { dismiss() }
                    }

                    BackHandler {
                        if (query.isNotEmpty()) {
                            viewModel.clearState()
                        } else {
                            dismiss()
                        }
                    }

                    val esdeState by esdePrefs.state.collectAsStateWithLifecycle()
                    val hiddenGames = esdeState.hiddenGames

                    val isHiddenMode = query.equals("@hidden", ignoreCase = true)
                    val isPlatformMode = !isHiddenMode && query.startsWith("@")
                    val platformSearch = if (isPlatformMode) query.removePrefix("@") else null
                    val allPlatforms = remember(allGames) {
                        allGames.map { it.systemName }.distinct().sorted()
                    }
                    val platformSuggestions = remember(platformSearch, allPlatforms) {
                        platformSearch?.let { text ->
                            if (text.isBlank()) allPlatforms
                            else allPlatforms.filter { it.contains(text, ignoreCase = true) }
                        } ?: emptyList()
                    }
                    val selectedPlatform = remember(platformSearch, allPlatforms) {
                        allPlatforms.firstOrNull { it.equals(platformSearch, ignoreCase = true) }
                    }
                    val filteredGames = remember(
                        allGames,
                        query,
                        selectedPlatform,
                        isPlatformMode,
                        isHiddenMode,
                        hiddenGames
                    ) {
                        val list = when {
                            isHiddenMode ->
                                allGames.filter { hiddenGameKey(it) in hiddenGames }

                            selectedPlatform != null ->
                                allGames.filter {
                                    it.systemName.equals(selectedPlatform, ignoreCase = true)
                                }

                            isPlatformMode && platformSearch != null ->
                                allGames.filter {
                                    it.systemName.contains(platformSearch, ignoreCase = true)
                                }

                            query.isBlank() -> allGames
                            else -> allGames.filter { game ->
                                game.name.contains(query, ignoreCase = true) ||
                                        game.systemName.contains(query, ignoreCase = true) ||
                                        game.genre?.contains(query, ignoreCase = true) == true ||
                                        game.developer?.contains(
                                            query,
                                            ignoreCase = true
                                        ) == true ||
                                        game.publisher?.contains(query, ignoreCase = true) == true
                            }
                        }
                        val deduped = list.distinctBy { it.name.lowercase() }
                        if (isHiddenMode) deduped
                        else deduped.filter { hiddenGameKey(it) !in hiddenGames }
                    }

                    val context = LocalContext.current

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(animDurationEnter)) +
                                scaleIn(tween(animDurationEnter), initialScale = 0.92f),
                        exit = fadeOut(tween(animDurationExit)) +
                                scaleOut(tween(animDurationExit), targetScale = 0.92f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = OledBackgroundColor,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                RomResultsGrid(
                                    games = filteredGames,
                                    isLoading = isLoading,
                                    isHiddenMode = isHiddenMode,
                                    modifier = Modifier.fillMaxSize(),
                                    onLaunchGame = { game -> launchGame(game, context) },
                                    onLaunchWithEmulator = { game, pkg ->
                                        launchGameWithEmulator(game, pkg)
                                    },
                                    onSaveEmulator = { game, pkg, cmd ->
                                        esdePrefs.setGameEmulator(gameKey(game), pkg)
                                        cmd?.let {
                                            esdePrefs.setGameLaunchCommand(gameKey(game), it)
                                        }
                                    },
                                    hasSavedEmulator = { game ->
                                        esdePrefs.getGameLaunchCommand(gameKey(game)) != null ||
                                                esdePrefs.getGameEmulator(gameKey(game)) != null
                                    },
                                    onGameFocused = { game ->
                                        viewModel.updateFocusedGame(game)
                                        game?.let {
                                            managers.feature.jinglesManager.onGameSelected(
                                                File(it.path).name
                                            )
                                        }
                                    },
                                    onHideGame = { game ->
                                        esdePrefs.hideGame(hiddenGameKey(game))
                                    },
                                    onUnhideGame = { game ->
                                        esdePrefs.unhideGame(hiddenGameKey(game))
                                    },
                                    onToggleKeyboard = {
                                        romSearchStateHolder.keyboardVisible.value =
                                            !romSearchStateHolder.keyboardVisible.value
                                    },
                                    isRetroArchGame = { game ->
                                        val saved = esdePrefs.getGameEmulator(gameKey(game))
                                        (saved ?: game.emulatorPackage)?.startsWith("com.retroarch") == true
                                    },
                                    hasSavedCore = { game ->
                                        esdePrefs.getGameCore(gameKey(game)) != null
                                    },
                                    onCoreSelected = { game, _, corePath ->
                                        esdePrefs.setGameCore(gameKey(game), corePath)
                                        launchGame(game, context)
                                    },
                                    onChangeFolder = { game ->
                                        pendingFolderChangeSystem = game.systemName
                                        val romPath = resolveRomPath(game)
                                        val hint = romPath?.let {
                                            val dir = java.io.File(it).parent ?: "/storage/emulated/0"
                                            android.net.Uri.parse("content://com.android.externalstorage.documents/document/${android.net.Uri.encode("primary:${dir.removePrefix("/storage/emulated/0/")}")}")
                                        }
                                        safTreeLauncher.launch(hint)
                                    },
                                )
                            }

                            if (isPlatformMode && selectedPlatform == null && platformSuggestions.isNotEmpty()) {
                                PlatformSuggestionsDropdown(
                                    platforms = platformSuggestions,
                                    onPlatformSelected = { platform ->
                                        viewModel.updateQuery("@$platform")
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resolveAndroidAppByLabel(label: String): Intent? {
        val pm = packageManager
        return pm.getInstalledApplications(0).firstOrNull { appInfo ->
            pm.getApplicationLabel(appInfo).toString().equals(label, ignoreCase = true)
        }?.let { appInfo ->
            pm.getLaunchIntentForPackage(appInfo.packageName)
        }
    }

    private fun resolveRomPath(game: GameInfo): String? {
        if (game.romAbsolutePath != null) return game.romAbsolutePath
        val romsPaths = esdePrefs.state.value.romsPaths +
                listOf("/storage/emulated/0/Roms")
        val filename = File(game.path).name  // strips subdirectory prefix e.g. "Games/Title.iso" → "Title.iso"
        for (root in romsPaths) {
            // 1. root / systemName / full-path  (e.g. Roms/ps2/Games/Title.iso)
            File(root, "${game.systemName}/${game.path}").let { if (it.exists()) return it.absolutePath }
            // 2. root / systemName / bare filename  (e.g. Roms/ps2/Title.iso)
            File(root, "${game.systemName}/$filename").let { if (it.exists()) return it.absolutePath }
            // 3. root already contains system folder: root / full-path or bare filename
            File(root, game.path).let { if (it.exists()) return it.absolutePath }
            File(root, filename).let { if (it.exists()) return it.absolutePath }
            // 4. Case-insensitive system folder scan (e.g. "PSP" vs "psp")
            File(root).listFiles()
                ?.firstOrNull { it.isDirectory && it.name.equals(game.systemName, ignoreCase = true) }
                ?.let { systemDir ->
                    File(systemDir, game.path).let { if (it.exists()) return it.absolutePath }
                    File(systemDir, filename).let { if (it.exists()) return it.absolutePath }
                }
        }
        return null
    }

    /**
     * Extracts the SD card volume ID from an absolute path like /storage/3A0D-5000/...
     * Returns null for internal storage (/storage/emulated/...).
     */
    private fun sdCardVolumeId(absolutePath: String): String? {
        val withoutStorage = absolutePath.removePrefix("/storage/")
        val volumeId = withoutStorage.substringBefore('/')
        return if (volumeId == "emulated" || volumeId.isEmpty()) null else volumeId
    }

    /**
     * Builds an authorized SAF document URI using the tree grant stored for [systemName].
     * Returns null if no tree has been granted yet for this system (caller should request one).
     */
    private fun buildSafDocumentUri(absolutePath: String, volumeId: String, systemName: String): Uri? {
        val treeUriString = esdePrefs.getSafTreeUri(systemName) ?: return null
        val treeUri = treeUriString.toUri()
        val relativePath = absolutePath.removePrefix("/storage/$volumeId/")
        val documentId = "$volumeId:$relativePath"
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    }

    private fun launchGame(
        game: GameInfo,
        context: Context
    ) {
        // Android apps have no ROM file — launch the package directly.
        if (game.systemName.equals("androidgames", ignoreCase = true) ||
            game.systemName.equals("androidapps", ignoreCase = true)) {
            // ES-DE stores the package name with a .app extension (e.g. "com.amazon.luna.app").
            // Some entries use a display name instead (e.g. "Amazon Luna.app"), so if a direct
            // package lookup fails we fall back to searching installed apps by label.
            val key = game.path.trimEnd('/').removeSuffix(".app")
            val intent = packageManager.getLaunchIntentForPackage(key)
                ?: resolveAndroidAppByLabel(key)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
                signalGameLaunch()
                startActivity(intent, options.toBundle())
            } else {
                Toast.makeText(context, "App not installed: $key", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }

        val romPath = resolveRomPath(game) ?: run {
            Log.e("RomSearchResults", "ROM path not resolved | system=${game.systemName} path=${game.path}")
            Toast.makeText(context, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Resolve the correct URI type before doing anything emulator-specific.
        // SD card ROMs need an authorized SAF tree document URI; internal storage uses FileProvider.
        val contentUri = resolveContentUri(game, romPath, context) ?: return  // null = SAF picker launched, will retry

        val pkg = esdePrefs.getGameEmulator(gameKey(game))
            ?: game.emulatorPackage
            ?: run {
                Toast.makeText(context, "No emulator configured for this game", Toast.LENGTH_SHORT).show()
                return
            }

        val corePath = if (pkg.startsWith("com.retroarch")) esdePrefs.getGameCore(gameKey(game)) else null

        Log.d("RomSearchResults", "launchGame | pkg=$pkg rom=$romPath uri=$contentUri")

        try {
            val intent = EsdeCommandLauncher.buildRomIntentFromPackage(pkg, romPath, contentUri, context, corePath)
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
            signalGameLaunch()
            startActivity(intent, options.toBundle())
            finish()
        } catch (e: Exception) {
            Log.e("RomSearchResults", "Failed to launch $pkg", e)
            // Fallback: open the emulator app itself
            try {
                packageManager.getLaunchIntentForPackage(pkg)?.let {
                    startActivity(it, ActivityOptions.makeBasic().apply { launchDisplayId = 0 }.toBundle())
                }
            } catch (_: Exception) {}
            signalGameLaunch()
            finish()
        }
    }

    /**
     * Resolves the [android.net.Uri] to pass to the emulator.
     * Returns null if a SAF access prompt was launched (the launch will be retried after the grant).
     */
    private fun resolveContentUri(game: GameInfo, romPath: String, context: Context): Uri? {
        val volumeId = sdCardVolumeId(romPath)
        return if (volumeId != null) {
            val safUri = buildSafDocumentUri(romPath, volumeId, game.systemName)
            if (safUri == null) {
                Log.d("RomSearchResults", "Requesting SAF tree for system=${game.systemName}")
                pendingGameLaunch = game to context
                // Hint at the ROM's parent directory so the picker opens in the right folder.
                val romDir = File(romPath).parent ?: "/storage/$volumeId"
                val relDir = romDir.removePrefix("/storage/$volumeId/")
                val hint = "content://com.android.externalstorage.documents/document/${Uri.encode("$volumeId:$relDir")}".toUri()
                safTreeLauncher.launch(hint)
                null
            } else safUri
        } else {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", File(romPath))
            } catch (e: Exception) {
                Log.w("RomSearchResults", "FileProvider failed, using file URI", e)
                Uri.fromFile(File(romPath))
            }
        }
    }

    private fun signalGameLaunch() {
        romSearchStateHolder.gameLaunchSignal.tryEmit(Unit)
        managers.feature.jinglesManager.onGameLaunched()
    }

    private fun gameKey(game: GameInfo) = game.systemName
    private fun hiddenGameKey(game: GameInfo) = "${game.systemName}/${game.path}"

    private fun launchGameWithEmulator(game: GameInfo, emulatorPackage: String) {
        esdePrefs.setGameEmulator(gameKey(game), emulatorPackage)
        val romPath = resolveRomPath(game) ?: run {
            Log.e("RomSearchResults", "launchGameWithEmulator: ROM path could not be resolved | systemName=${game.systemName} path=${game.path}")
            Toast.makeText(this, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("RomSearchResults", "launchGameWithEmulator | pkg=$emulatorPackage | rom=$romPath")
        try {
            val intent = EsdeCommandLauncher.buildRomIntentFromPackage(
                packageName = emulatorPackage,
                romAbsPath = romPath,
                context = this
            )
            Log.d("RomSearchResults", "  → intent data=${intent.data} extras=${intent.extras}")
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = 0
            signalGameLaunch()
            startActivity(intent, options.toBundle())
            finish()
            return
        } catch (e: Exception) {
            Log.w("RomSearchResults", "Emulator picker launch failed for $emulatorPackage", e)
        }

        // Fallback: open the emulator app itself
        try {
            val fallbackIntent = packageManager.getLaunchIntentForPackage(emulatorPackage)
            if (fallbackIntent != null) {
                val options = ActivityOptions.makeBasic()
                options.launchDisplayId = 0
                startActivity(fallbackIntent, options.toBundle())
            }
        } catch (_: Exception) {
        }
        signalGameLaunch()
        finish()
    }
}
