package jr.brian.home.esde.ui

import android.app.ActivityOptions
import android.content.ComponentName
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

    private val safTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) {
            Toast.makeText(this, "SD card access denied", Toast.LENGTH_SHORT).show()
            pendingGameLaunch = null
            return@registerForActivityResult
        }
        // Persist read permission so we don't need to prompt again next time.
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Extract the volume ID from the tree URI.
        // URI path looks like /tree/3A0D-5000%3A or /tree/3A0D-5000%3AAyn+Thor%2Fn3ds
        // DocumentsContract.getTreeDocumentId() returns the decoded document ID e.g. "3A0D-5000:"
        val volumeId = try {
            DocumentsContract.getTreeDocumentId(treeUri)?.substringBefore(':')
        } catch (_: Exception) {
            treeUri.lastPathSegment?.substringBefore(':')
        }
        if (volumeId != null) {
            esdePrefs.setSafTreeUri(volumeId, treeUri.toString())
            Log.d("RomSearchResults", "SAF tree granted | volumeId=$volumeId | treeUri=$treeUri")
        }
        // Retry the pending launch now that we have access.
        pendingGameLaunch?.let { (game, ctx) -> launchGame(game, ctx) }
        pendingGameLaunch = null
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
                                    }
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

    private fun resolveRomPath(game: GameInfo): String? {
        if (game.romAbsolutePath != null) return game.romAbsolutePath
        // Fallback: try user-configured ROM roots (internal storage only)
        val romsPaths = esdePrefs.state.value.romsPaths
        for (root in romsPaths) {
            val candidate = File(root, "${game.systemName}/${game.path}")
            if (candidate.exists()) return candidate.absolutePath
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
     * Builds an authorized SAF document URI using a persisted tree grant.
     * Returns null if no tree has been granted yet for this volume (caller should request one).
     */
    private fun buildSafDocumentUri(absolutePath: String, volumeId: String): Uri? {
        val treeUriString = esdePrefs.getSafTreeUri(volumeId) ?: return null
        val treeUri = treeUriString.toUri()
        val relativePath = absolutePath.removePrefix("/storage/$volumeId/")
        val documentId = "$volumeId:$relativePath"
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    }

    private fun launchGame(
        game: GameInfo,
        context: Context
    ) {
        val romPath = resolveRomPath(game) ?: run {
            Log.e("RomSearchResults", "ROM path could not be resolved | systemName=${game.systemName} path=${game.path} romAbsolutePath=${game.romAbsolutePath} romsPaths=${esdePrefs.state.value.romsPaths}")
            Toast.makeText(context, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }

        // AzaharPlus — focused first. Add other platforms once this is confirmed working.
        val packageName = "io.github.lime3ds.android"
        val activityName = "org.citra.citra_emu.activities.EmulationActivity"
        val romFile = File(romPath)

        // For SD card ROMs, we need an authorized SAF tree document URI.
        // Simply constructing a com.android.externalstorage.documents URI without a prior
        // ACTION_OPEN_DOCUMENT_TREE grant causes a SecurityException.
        val volumeId = sdCardVolumeId(romPath)
        val contentUri: Uri = if (volumeId != null) {
            val safUri = buildSafDocumentUri(romPath, volumeId)
            if (safUri == null) {
                // No tree grant yet — ask the user to pick the SD card root, then retry.
                Log.d("RomSearchResults", "No SAF tree for volumeId=$volumeId, requesting access")
                pendingGameLaunch = game to context
                val hint = "content://com.android.externalstorage.documents/document/${Uri.encode("$volumeId:")}".toUri()
                safTreeLauncher.launch(hint)
                return
            }
            safUri
        } else {
            // Internal storage — use FileProvider.
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", romFile)
            } catch (e: Exception) {
                Log.w("RomSearchResults", "FileProvider failed, falling back to file URI", e)
                Uri.fromFile(romFile)
            }.also { uri ->
                try { context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(packageName, activityName)
            setDataAndType(contentUri, "*/*")
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        Log.d("RomSearchResults", "launchGame(AzaharPlus) | rom=$romPath | uri=$contentUri | uriType=${contentUri.authority}")

        try {
            signalGameLaunch()
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch game", Toast.LENGTH_SHORT).show()
            Log.e("RomSearchResults", "Failed to launch AzaharPlus", e)
        }
    }

//    private fun launchGame(game: GameInfo) {
//        Log.d(
//            "RomSearchResults",
//            "launchGame called | name=${game.name} system=${game.systemName} " +
//            "path=${game.path} romAbsPath=${game.romAbsolutePath} " +
//            "emulatorPkg=${game.emulatorPackage} launchCmd=${game.launchCommand}"
//        )
//        val findRulesFile =
//            File(filesDir.parent ?: "", "ES-DE/custom_systems/es_find_rules.xml").let { f ->
//                if (f.exists()) f
//                else File("/storage/emulated/0/ES-DE/custom_systems/es_find_rules.xml")
//            }
//        val customRules = EsdeCommandLauncher.parseCustomRules(findRulesFile)
//
//        val savedCommand = esdePrefs.getGameLaunchCommand(gameKey(game))
//        if (savedCommand != null && game.romAbsolutePath != null) {
//            Log.d("RomSearchResults", "Launching via saved command | rom=${game.romAbsolutePath} | cmd=$savedCommand")
//            val intent = EsdeCommandLauncher.buildIntent(
//                launchCommand = savedCommand,
//                romAbsPath = game.romAbsolutePath,
//                context = this,
//                customRules = customRules
//            )
//            if (intent != null) {
//                Log.d("RomSearchResults", "  → intent data=${intent.data} extras=${intent.extras}")
//                try {
//                    val options = ActivityOptions.makeBasic()
//                    options.launchDisplayId = 0
//                    signalGameLaunch()
//                    startActivity(intent, options.toBundle())
//                    finish()
//                    return
//                } catch (e: Exception) {
//                    Log.w("RomSearchResults", "Saved-command launch failed, trying package fallback", e)
//                }
//            }
//        }
//
//        val savedPkg = esdePrefs.getGameEmulator(gameKey(game))
//        if (savedPkg != null && game.romAbsolutePath != null) {
//            Log.d("RomSearchResults", "Launching via saved emulator pkg=$savedPkg | rom=${game.romAbsolutePath}")
//            launchGameWithEmulator(game, savedPkg)
//            return
//        }
//
//        val romPath = game.romAbsolutePath
//        val command = game.launchCommand
//
//        if (romPath != null && command != null) {
//            Log.d("RomSearchResults", "Launching via game launch command | rom=$romPath | cmd=$command")
//            val intent = EsdeCommandLauncher.buildIntent(
//                launchCommand = command,
//                romAbsPath = romPath,
//                context = this,
//                customRules = customRules
//            )
//            if (intent != null) {
//                Log.d("RomSearchResults", "  → intent data=${intent.data} extras=${intent.extras}")
//                try {
//                    val options = ActivityOptions.makeBasic()
//                    options.launchDisplayId = 0
//                    signalGameLaunch()
//                    startActivity(intent, options.toBundle())
//                    finish()
//                    return
//                } catch (e: Exception) {
//                    Log.w("RomSearchResults", "Direct launch failed, falling back to app open", e)
//                }
//            }
//        }
//
//        val pkg = game.emulatorPackage ?: run { finish(); return }
//
//        if (game.romAbsolutePath != null) {
//            Log.d("RomSearchResults", "Launching via buildRomIntentFromPackage | pkg=$pkg | rom=${game.romAbsolutePath}")
//            try {
//                val romIntent = EsdeCommandLauncher.buildRomIntentFromPackage(
//                    packageName = pkg,
//                    romAbsPath = game.romAbsolutePath,
//                    context = this
//                )
//                Log.d("RomSearchResults", "  → intent data=${romIntent.data} extras=${romIntent.extras}")
//                val options = ActivityOptions.makeBasic()
//                options.launchDisplayId = 0
//                signalGameLaunch()
//                startActivity(romIntent, options.toBundle())
//                finish()
//                return
//            } catch (e: Exception) {
//                Log.w("RomSearchResults", "Package ROM launch failed, falling back to app open", e)
//            }
//        }
//
//        // Fallback: open the emulator app itself
//        try {
//            val intent = packageManager.getLaunchIntentForPackage(pkg)
//            if (intent != null) {
//                val options = ActivityOptions.makeBasic()
//                options.launchDisplayId = 0
//                startActivity(intent, options.toBundle())
//            }
//        } catch (_: Exception) {}
//        signalGameLaunch()
//        finish()
//    }

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
