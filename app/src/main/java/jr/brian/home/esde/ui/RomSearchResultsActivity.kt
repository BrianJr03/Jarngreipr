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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.content.edit

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
        val systemName = pendingFolderChangeSystem ?: pendingGameLaunch?.first?.systemName

        // Detect if this is internal storage (primary:...) and extract the real path.
        // If so, update romsPaths so resolveRomPath picks up the correct root without SAF.
        val treeDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Exception) {
            null
        }
        if (treeDocId?.startsWith("primary:") == true) {
            val rel = treeDocId.removePrefix("primary:")  // e.g. "Roms/psp" or "Roms"
            val pickedDir = "/storage/emulated/0/$rel"
            val romsRoot = if (systemName != null &&
                File(pickedDir).name.equals(systemName, ignoreCase = true)
            ) {
                File(pickedDir).parent ?: pickedDir
            } else {
                pickedDir
            }
            esdePrefs.addRomsPath(romsRoot)
            // Also store the tree URI so we can build document URIs for emulators
            // (e.g. NetherSX2) that access files via SAF rather than raw paths.
            if (systemName != null) {
                esdePrefs.setSafTreeUri(systemName, treeUri.toString())
            }
            Log.d("RomSearchResults", "Internal folder | romsRoot=$romsRoot treeUri=$treeUri")
        } else if (systemName != null) {
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

                    var showExperimentalDialog by remember {
                        val prefs = getSharedPreferences("rom_search_prefs", Context.MODE_PRIVATE)
                        mutableStateOf(!prefs.getBoolean("experimental_shown", false))
                    }

                    if (showExperimentalDialog) {
                        fun markShown() {
                            showExperimentalDialog = false
                            getSharedPreferences("rom_search_prefs", Context.MODE_PRIVATE)
                                .edit { putBoolean("experimental_shown", true) }
                        }
                        AlertDialog(
                            onDismissRequest = { markShown() },
                            containerColor = OledCardColor,
                            title = {
                                Text(
                                    text = stringResource(R.string.rom_search_experimental_title),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.rom_search_experimental_message),
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { markShown() }) {
                                    Text(
                                        text = stringResource(R.string.rom_search_experimental_got_it),
                                        color = ThemeAccentColor
                                    )
                                }
                            },
                            dismissButton = {}
                        )
                    }

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
                            romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
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
                                    onLaunchGame = { game ->
                                        romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                                        launchGame(game, context)
                                    },
                                    onLaunchWithEmulator = { game, pkg ->
                                        romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
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
                                        (saved
                                            ?: game.emulatorPackage)?.startsWith("com.retroarch") == true
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
                                            val dir =
                                                java.io.File(it).parent ?: "/storage/emulated/0"
                                            "content://com.android.externalstorage.documents/document/${
                                                Uri.encode(
                                                    "primary:${dir.removePrefix("/storage/emulated/0/")}"
                                                )
                                            }".toUri()
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
        val filename =
            File(game.path).name  // strips subdirectory prefix e.g. "Games/Title.iso" → "Title.iso"

        // PSP and PS2: build path directly — no existence check, since File.exists() can
        // return false for these paths even when the file is there (permission timing).
        if (game.systemName.equals("psp", ignoreCase = true) ||
            game.systemName.equals("ps2", ignoreCase = true)
        ) {
            val root = romsPaths.firstOrNull() ?: "/storage/emulated/0/Roms"
            return File(root, "${game.systemName}/$filename").absolutePath
        }

        for (root in romsPaths) {
            // 1. root / systemName / full-path  (e.g. Roms/ps2/Games/Title.iso)
            File(
                root,
                "${game.systemName}/${game.path}"
            ).let { if (it.exists()) return it.absolutePath }
            // 2. root / systemName / bare filename  (e.g. Roms/ps2/Title.iso)
            File(
                root,
                "${game.systemName}/$filename"
            ).let { if (it.exists()) return it.absolutePath }
            // 3. root already contains system folder: root / full-path or bare filename
            File(root, game.path).let { if (it.exists()) return it.absolutePath }
            File(root, filename).let { if (it.exists()) return it.absolutePath }
            // 4. Case-insensitive system folder scan (e.g. "PSP" vs "psp")
            File(root).listFiles()
                ?.firstOrNull {
                    it.isDirectory && it.name.equals(
                        game.systemName,
                        ignoreCase = true
                    )
                }
                ?.let { systemDir ->
                    File(systemDir, game.path).let { if (it.exists()) return it.absolutePath }
                    File(systemDir, filename).let { if (it.exists()) return it.absolutePath }
                }
        }
        return null
    }

    /**
     * Builds a tree-based SAF document URI for AetherSX2/NetherSX2 using our stored tree grant.
     * This mirrors ES-DE's approach: send a tree document URI with FLAG_GRANT_READ_URI_PERMISSION
     * so the emulator gets one-time read access without needing All Files Access.
     * Returns null if no tree grant has been stored yet for [systemName].
     */
    private fun buildAetherDocUri(systemName: String, romAbsPath: String): android.net.Uri? {
        val treeUriStr = esdePrefs.getSafTreeUri(systemName) ?: return null
        val treeUri = treeUriStr.toUri()
        val documentId = when {
            romAbsPath.startsWith("/storage/emulated/0/") ->
                "primary:${romAbsPath.removePrefix("/storage/emulated/0/")}"

            else -> {
                val volId = sdCardVolumeId(romAbsPath) ?: return null
                "$volId:${romAbsPath.removePrefix("/storage/$volId/")}"
            }
        }
        return try {
            DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        } catch (_: Exception) {
            null
        }
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
    private fun buildSafDocumentUri(
        absolutePath: String,
        volumeId: String,
        systemName: String
    ): Uri? {
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
            game.systemName.equals("androidapps", ignoreCase = true)
        ) {
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
            Log.e(
                "RomSearchResults",
                "ROM path not resolved | system=${game.systemName} path=${game.path}"
            )
            Toast.makeText(context, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }

        val pkg = esdePrefs.getGameEmulator(gameKey(game))
            ?: game.emulatorPackage
            ?: run {
                // No emulator configured — for known emulators just open the app directly.
                val knownFallbacks = listOf(
                    "org.ppsspp.ppsspp",
                    "org.ppsspp.ppssppgold",
                    "xyz.aethersx2.android"
                )
                val fallbackPkg = knownFallbacks.firstOrNull { p ->
                    packageManager.getLaunchIntentForPackage(p) != null
                }
                if (fallbackPkg != null) {
                    val intent = packageManager.getLaunchIntentForPackage(fallbackPkg)!!
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
                    signalGameLaunch()
                    startActivity(intent, options.toBundle())
                    finish()
                } else {
                    Toast.makeText(
                        context,
                        "No emulator configured for this game",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

        // For PPSSPP and NetherSX2/AetherSX2, just open the emulator directly.
        if (pkg == "org.ppsspp.ppsspp" || pkg == "org.ppsspp.ppssppgold" || pkg == "xyz.aethersx2.android") {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
                signalGameLaunch()
                startActivity(intent, options.toBundle())
                finish()
            } else {
                Toast.makeText(context, "Emulator not installed: $pkg", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // If a launch command was saved (e.g. GameNative, GameHub, Winlator), use it directly
        val savedCommand = esdePrefs.getGameLaunchCommand(gameKey(game))
        if (savedCommand != null) {
            val findRulesFile =
                File(filesDir.parent ?: "", "ES-DE/custom_systems/es_find_rules.xml").let { f ->
                    if (f.exists()) f else File("/storage/emulated/0/ES-DE/custom_systems/es_find_rules.xml")
                }
            val customRules = EsdeCommandLauncher.parseCustomRules(findRulesFile)
            val intent = EsdeCommandLauncher.buildIntent(savedCommand, romPath, context, customRules)
                ?: run {
                    Toast.makeText(context, "Failed to build launch intent", Toast.LENGTH_SHORT).show()
                    return
                }
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
            Log.d("RomSearchResults", "launchGame (command) | cmd=$savedCommand rom=$romPath")
            signalGameLaunch()
            startActivity(intent, options.toBundle())
            finish()
            return
        }

        val corePath =
            if (pkg.startsWith("com.retroarch")) esdePrefs.getGameCore(gameKey(game)) else null
        val effectiveContentUri: Uri = resolveContentUri(game, romPath, context) ?: return

        Log.d("RomSearchResults", "launchGame | pkg=$pkg rom=$romPath uri=$effectiveContentUri")

        try {
            val romIntent = EsdeCommandLauncher.buildRomIntentFromPackage(
                pkg,
                romPath,
                effectiveContentUri,
                context,
                corePath
            )
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
            val canHandleRom = packageManager.resolveActivity(romIntent, 0) != null
            val intent = if (canHandleRom) {
                romIntent
            } else {
                // App doesn't handle ROM intents (e.g. chosen via "Choose App") — plain launch
                packageManager.getLaunchIntentForPackage(pkg)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ?: run {
                        Toast.makeText(context, "App not installed: $pkg", Toast.LENGTH_SHORT)
                            .show()
                        return
                    }
            }
            signalGameLaunch()
            startActivity(intent, options.toBundle())
            finish()
        } catch (e: Exception) {
            Log.e("RomSearchResults", "Failed to launch $pkg", e)
            Toast.makeText(context, "Failed to launch: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val hint =
                    "content://com.android.externalstorage.documents/document/${Uri.encode("$volumeId:$relDir")}".toUri()
                safTreeLauncher.launch(hint)
                null
            } else safUri
        } else {
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(romPath)
                )
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

    private fun gameKey(game: GameInfo) = "${game.systemName}/${game.path}"
    private fun hiddenGameKey(game: GameInfo) = "${game.systemName}/${game.path}"

    private fun launchGameWithEmulator(game: GameInfo, emulatorPackage: String) {
        esdePrefs.setGameEmulator(gameKey(game), emulatorPackage)
        val romPath = resolveRomPath(game) ?: run {
            Log.e(
                "RomSearchResults",
                "launchGameWithEmulator: ROM path could not be resolved | systemName=${game.systemName} path=${game.path}"
            )
            Toast.makeText(this, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("RomSearchResults", "launchGameWithEmulator | pkg=$emulatorPackage | rom=$romPath")
        val contentUri: Uri
        if (emulatorPackage == "xyz.aethersx2.android") {
            val aetherSafUri = buildAetherDocUri(game.systemName, romPath)
            if (aetherSafUri == null) {
                pendingGameLaunch = game to this
                val romDir = File(romPath).parent ?: "/storage/emulated/0"
                val rel = romDir.removePrefix("/storage/emulated/0/")
                val hint =
                    "content://com.android.externalstorage.documents/document/${Uri.encode("primary:$rel")}".toUri()
                safTreeLauncher.launch(hint)
                return
            }
            contentUri = aetherSafUri
        } else {
            contentUri = resolveContentUri(game, romPath, this) ?: return
        }
        try {
            val romIntent = EsdeCommandLauncher.buildRomIntentFromPackage(
                packageName = emulatorPackage,
                romAbsPath = romPath,
                contentUri = contentUri,
                context = this
            )
            Log.d(
                "RomSearchResults",
                "  → intent data=${romIntent.data} extras=${romIntent.extras}"
            )
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = 0
            val canHandleRom = packageManager.resolveActivity(romIntent, 0) != null
            val intent = if (canHandleRom) {
                romIntent
            } else {
                packageManager.getLaunchIntentForPackage(emulatorPackage)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ?: run {
                        Toast.makeText(
                            this,
                            "App not installed: $emulatorPackage",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
            }
            signalGameLaunch()
            startActivity(intent, options.toBundle())
            finish()
        } catch (e: Exception) {
            Log.e("RomSearchResults", "Emulator picker launch failed for $emulatorPackage", e)
            Toast.makeText(this, "Failed to launch: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
