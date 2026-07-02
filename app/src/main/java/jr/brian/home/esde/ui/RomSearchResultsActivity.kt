package jr.brian.home.esde.ui

import jr.brian.home.esde.data.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.R
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.data.ManagerContainer
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.PlatformImageFolderType
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.hiddenGameKey
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.home.model.rom.toGameInfo
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.viewmodels.MainViewModel
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.util.launchFrontend
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RomSearchResultsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_FROM_FRONTEND = "from_frontend"
    }

    @Inject
    lateinit var managers: ManagerContainer

    @Inject
    lateinit var esdePrefs: ESDEPreferencesManager

    @Inject
    lateinit var romSearchStateHolder: RomSearchStateHolder

    private val viewModel: RomSearchResultsViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private var pendingFolderChangeSystem: String? = null
    private lateinit var romLauncher: RomGameLauncher
    private var fromFrontend = false

    private val safTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) {
            Toast.makeText(this, "Folder access denied", Toast.LENGTH_SHORT).show()
            romLauncher.pendingGameLaunch = null
            pendingFolderChangeSystem = null
            return@registerForActivityResult
        }
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val systemName =
            pendingFolderChangeSystem ?: romLauncher.pendingGameLaunch?.first?.systemName

        val treeDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Exception) {
            null
        }

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
            romLauncher.launchGame(game, ctx, managers.ui.appDisplayPreferenceManager.getAppDisplayPreference(pkg))
        }
        romLauncher.pendingGameLaunch = null
        pendingFolderChangeSystem = null
    }

    override fun finish() {
        if (fromFrontend && !gameLaunched) {
            launchFrontend(this)
        }
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private var gameLaunched = false

    override fun onDestroy() {
        super.onDestroy()
        if (gameLaunched) {
            romSearchStateHolder.gameLaunchSignal.tryEmit(Unit)
        }
        viewModel.clearState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fromFrontend = intent.getBooleanExtra(EXTRA_FROM_FRONTEND, false)
        romLauncher = RomGameLauncher(
            activity = this,
            esdePrefs = esdePrefs,
            onSignalGameLaunch = ::signalGameLaunch,
            onLaunchSafPicker = { uri -> safTreeLauncher.launch(uri) }
        )
        romSearchStateHolder.hintAndKbVisible.value = esdePrefs.state.value.romSearchHintsKbVisible
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
                        val prefs = getSharedPreferences("rom_search_prefs", MODE_PRIVATE)
                        mutableStateOf(!prefs.getBoolean("experimental_shown", false))
                    }

                    val context = LocalContext.current

                    if (showExperimentalDialog) {
                        fun markShown() {
                            showExperimentalDialog = false
                            getSharedPreferences("rom_search_prefs", MODE_PRIVATE)
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

                    val localAppDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current

                    LaunchedEffect(Unit) {
                        val pending = romSearchStateHolder.pendingRomToLaunch.value
                        if (pending != null) {
                            romSearchStateHolder.pendingRomToLaunch.value = null
                            val game = pending.toGameInfo()
                            val pkg = esdePrefs.getGameEmulator(gameKey(game))
                                ?: game.emulatorPackage ?: game.path
                            romLauncher.launchGame(
                                game, context,
                                localAppDisplayPreferenceManager.getAppDisplayPreference(pending.key)
                            )
                            signalGameLaunch()
                            finish()
                        } else {
                            isVisible = true
                        }
                    }

                    val query by viewModel.query.collectAsStateWithLifecycle()
                    val queryTrimmed = query.trim()

                    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
                    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                    val homeUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.dismissSignal.collect { dismiss() }
                    }

                    BackHandler {
                        if (queryTrimmed.isNotEmpty()) {
                            viewModel.clearState()
                        } else {
                            romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                            dismiss()
                        }
                    }

                    val esdeState by esdePrefs.state.collectAsStateWithLifecycle()
                    val hiddenGames = esdeState.hiddenGames
                    val romSearchUseWallpaper = esdeState.romSearchUseWallpaper
                    val cardMediaType = esdeState.romSearchCardMediaType
                    val gameMediaMap = esdeState.romSearchGameMediaMap
                    val systemMediaMap = esdeState.systemMediaMap
                    val hideNoMetadata = esdeState.romSearchHideNoMetadata
                    val hideNoImage = esdeState.romSearchHideNoImage
                    val focusAnimationEnabled = esdeState.romSearchDiscSpin
                    val focusAnimationDisabledGames = esdeState.romSearchFocusAnimationDisabledGames

                    val romSearchShowAllAndroidApps = esdeState.romSearchShowAllAndroidApps
                    val platformImagesEnabled = esdeState.romSearchPlatformImagesEnabled
                    val platformImagesFolderUri = esdeState.romSearchPlatformImagesFolderUri
                    val platformImagesFolderType = esdeState.romSearchPlatformImagesFolderType

                    var platformImageMap by remember { mutableStateOf(emptyMap<String, Uri>()) }
                    LaunchedEffect(platformImagesEnabled, platformImagesFolderUri, platformImagesFolderType) {
                        if (!platformImagesEnabled || platformImagesFolderUri == null) {
                            platformImageMap = emptyMap()
                            return@LaunchedEffect
                        }
                        val map = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching {
                                val treeUri = platformImagesFolderUri.toUri()
                                val rootDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                                    ?: return@runCatching emptyMap()
                                when (platformImagesFolderType) {
                                    jr.brian.home.esde.model.PlatformImageFolderType.Smart ->
                                        rootDoc.listFiles()
                                            .filter { it.isDirectory }
                                            .mapNotNull { dir ->
                                                val image = dir.listFiles().firstOrNull { file ->
                                                    file.isFile && file.name?.let { n ->
                                                        val ext = n.substringAfterLast(".", "").lowercase()
                                                        ext in setOf("png", "jpg", "jpeg", "gif", "webp")
                                                    } == true
                                                }
                                                val name = dir.name?.lowercase()
                                                if (image != null && name != null) name to image.uri else null
                                            }
                                            .toMap()
                                    PlatformImageFolderType.Default ->
                                        rootDoc.listFiles()
                                            .filter { it.isFile }
                                            .mapNotNull { file ->
                                                val name = file.name
                                                    ?.substringBeforeLast(".")
                                                    ?.lowercase()
                                                if (name != null) name to file.uri else null
                                            }
                                            .toMap()
                                }
                            }.getOrDefault(emptyMap())
                        }
                        platformImageMap = map
                    }

                    LaunchedEffect(romSearchShowAllAndroidApps) {
                        if (romSearchShowAllAndroidApps) mainViewModel.loadAllApps(context)
                    }

                    val isHiddenMode = queryTrimmed.equals("@hidden", ignoreCase = true)
                    val isAndroidMode = romSearchShowAllAndroidApps && (
                            queryTrimmed.equals("@android", ignoreCase = true) ||
                                    queryTrimmed.startsWith("@android ", ignoreCase = true)
                            )
                    val androidModeFilter =
                        if (isAndroidMode && queryTrimmed.length > "@android ".length - 1)
                            queryTrimmed.drop("@android ".length).trim()
                        else ""
                    val isPlatformMode =
                        !isHiddenMode && !isAndroidMode && queryTrimmed.startsWith("@")
                    val platformSearch =
                        if (isPlatformMode) queryTrimmed.removePrefix("@") else null
                    val allPlatforms = remember(allGames) {
                        allGames.map { it.systemName }.distinct().sorted()
                    }
                    val platformSuggestions = remember(platformSearch, allPlatforms, allGames, hiddenGames) {
                        platformSearch?.let { text ->
                            val candidates = if (text.isBlank()) allPlatforms
                            else allPlatforms.filter { it.contains(text, ignoreCase = true) }
                            candidates.filter { platform ->
                                allGames.any { game ->
                                    game.systemName.equals(platform, ignoreCase = true) &&
                                            hiddenGameKey(game) !in hiddenGames
                                }
                            }
                        } ?: emptyList()
                    }
                    val selectedPlatform = remember(platformSearch, allPlatforms) {
                        allPlatforms.firstOrNull { it.equals(platformSearch, ignoreCase = true) }
                    }
                    val allAndroidApps =
                        remember(romSearchShowAllAndroidApps, homeUiState.allApps) {
                            if (!romSearchShowAllAndroidApps) emptyList()
                            else homeUiState.allApps.map { appInfo ->
                                jr.brian.home.esde.model.GameInfo(
                                    path = appInfo.packageName,
                                    name = appInfo.label,
                                    systemName = "androidapps"
                                )
                            }
                        }
                    var autoFilterPlatform by remember { mutableStateOf<String?>(null) }

                    val filteredGames = remember(
                        allGames,
                        queryTrimmed,
                        selectedPlatform,
                        isPlatformMode,
                        isHiddenMode,
                        isAndroidMode,
                        androidModeFilter,
                        allAndroidApps,
                        hiddenGames,
                        hideNoMetadata,
                        hideNoImage,
                        cardMediaType,
                        autoFilterPlatform
                    ) {
                        if (isAndroidMode) return@remember if (androidModeFilter.isBlank()) allAndroidApps
                        else allAndroidApps.filter {
                            it.name.contains(
                                androidModeFilter,
                                ignoreCase = true
                            )
                        }
                        val list = when {
                            isHiddenMode ->
                                allGames.filter { hiddenGameKey(it) in hiddenGames }

                            autoFilterPlatform != null ->
                                allGames.filter {
                                    it.systemName.equals(autoFilterPlatform, ignoreCase = true)
                                }

                            selectedPlatform != null ->
                                allGames.filter {
                                    it.systemName.equals(
                                        selectedPlatform,
                                        ignoreCase = true
                                    )
                                }

                            isPlatformMode && platformSearch != null ->
                                allGames.filter {
                                    it.systemName.contains(
                                        platformSearch,
                                        ignoreCase = true
                                    )
                                }

                            queryTrimmed.isBlank() -> allGames
                            else -> {
                                val esdeMatches = allGames.filter { game ->
                                    game.name.contains(queryTrimmed, ignoreCase = true) ||
                                            game.systemName.contains(
                                                queryTrimmed,
                                                ignoreCase = true
                                            ) ||
                                            game.genre?.contains(
                                                queryTrimmed,
                                                ignoreCase = true
                                            ) == true ||
                                            game.developer?.contains(
                                                queryTrimmed,
                                                ignoreCase = true
                                            ) == true ||
                                            game.publisher?.contains(
                                                queryTrimmed,
                                                ignoreCase = true
                                            ) == true
                                }
                                val androidMatches = allAndroidApps.filter { app ->
                                    app.name.contains(queryTrimmed, ignoreCase = true)
                                }
                                (esdeMatches + androidMatches)
                            }
                        }
                        val deduped = list.distinctBy { it.name.lowercase() }
                        val visibleList = if (isHiddenMode) deduped
                        else deduped.filter { hiddenGameKey(it) !in hiddenGames }
                        var result = visibleList
                        if (hideNoImage && !isHiddenMode) {
                            result = result.filter { game ->
                                if (game.systemName.equals(
                                        "androidapps",
                                        ignoreCase = true
                                    )
                                ) return@filter true
                                val resolvedPath = when (cardMediaType) {
                                    RomSearchCardMediaType.PhysicalMedia -> game.physicalMediaPath
                                        ?: game.artworkPath

                                    RomSearchCardMediaType.Covers -> game.artworkPath
                                        ?: game.physicalMediaPath

                                    RomSearchCardMediaType.Screenshots -> game.screenshotPath
                                        ?: game.physicalMediaPath ?: game.artworkPath

                                    RomSearchCardMediaType.Fanart -> game.fanartPath
                                        ?: game.physicalMediaPath ?: game.artworkPath

                                    RomSearchCardMediaType.TitleScreens -> game.titlescreenPath
                                        ?: game.physicalMediaPath ?: game.artworkPath

                                    RomSearchCardMediaType.Marquee -> game.marqueeImagePath
                                        ?: game.physicalMediaPath ?: game.artworkPath

                                    RomSearchCardMediaType.MixImages -> game.miximagePath
                                        ?: game.physicalMediaPath ?: game.artworkPath
                                }
                                resolvedPath != null
                            }
                        }
                        if (hideNoMetadata && !isHiddenMode) {
                            result = result.filter { game ->
                                game.systemName.equals("androidapps", ignoreCase = true) ||
                                        game.description != null || game.genre != null ||
                                        game.developer != null || game.publisher != null ||
                                        game.rating > 0f
                            }
                        }
                        result
                    }

                    val dropdownVisible = isPlatformMode && selectedPlatform == null &&
                            (platformSuggestions.isNotEmpty() || romSearchShowAllAndroidApps)
                    var dropdownFocusedIndex by remember { mutableIntStateOf(-1) }

                    // Reset dropdown focus whenever the dropdown hides
                    LaunchedEffect(dropdownVisible) {
                        if (!dropdownVisible) dropdownFocusedIndex = -1
                    }

                    // Clear autoFilter preview when focus leaves the dropdown
                    LaunchedEffect(dropdownFocusedIndex) {
                        if (dropdownFocusedIndex < 0) autoFilterPlatform = null
                    }

                    val dropdownItems = remember(romSearchShowAllAndroidApps, platformSuggestions) {
                        buildList {
                            if (romSearchShowAllAndroidApps) add("android"); addAll(
                            platformSuggestions
                        )
                        }
                    }

                    // Increments only when a non-blank query is typed, so the grid resets to
                    // index 0 on filter changes but preserves scroll when clearing the text.
                    var focusResetCounter by remember { mutableIntStateOf(0) }
                    LaunchedEffect(queryTrimmed) {
                        if (queryTrimmed.isNotBlank()) focusResetCounter++
                    }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(animDurationEnter)) +
                                scaleIn(tween(animDurationEnter), initialScale = 0.92f),
                        exit = fadeOut(tween(animDurationExit)) +
                                scaleOut(tween(animDurationExit), targetScale = 0.92f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.keyCode ==
                                        AndroidKeyEvent.KEYCODE_BUTTON_Y
                                    ) {
                                        return@onPreviewKeyEvent true
                                    }
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    // Hack: catch back at the preview stage so neither the focus
                                    // animation nor any descendant can swallow the first press.
                                    if (keyEvent.nativeKeyEvent.keyCode ==
                                        AndroidKeyEvent.KEYCODE_BUTTON_B ||
                                        keyEvent.nativeKeyEvent.keyCode ==
                                        AndroidKeyEvent.KEYCODE_BACK
                                    ) {
                                        if (queryTrimmed.isNotEmpty()) {
                                            viewModel.clearState()
                                        } else {
                                            romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                                            dismiss()
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                    // When navigating the dropdown, a DPAD press returns focus to the grid
                                    // without consuming — let the grid handle the actual movement.
                                    val isDpad = keyEvent.nativeKeyEvent.keyCode in listOf(
                                        AndroidKeyEvent.KEYCODE_DPAD_UP,
                                        AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT
                                    )
                                    if (isDpad && dropdownFocusedIndex >= 0) {
                                        dropdownFocusedIndex = -1
                                        false // don't consume — grid handles the move
                                    } else false
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                                    val isShoulderOrTrigger =
                                        keyEvent.nativeKeyEvent.keyCode in listOf(
                                            AndroidKeyEvent.KEYCODE_BUTTON_L1,
                                            AndroidKeyEvent.KEYCODE_BUTTON_L2,
                                            AndroidKeyEvent.KEYCODE_BUTTON_R1,
                                            AndroidKeyEvent.KEYCODE_BUTTON_R2
                                        )
                                    // Shoulder/trigger always opens the platform dropdown,
                                    // overwriting whatever is in the text field.
                                    if (isShoulderOrTrigger && !dropdownVisible) {
                                        viewModel.updateQuery("@")
                                        return@onKeyEvent true
                                    }
                                    if (!dropdownVisible) return@onKeyEvent false
                                    val maxIdx = dropdownItems.lastIndex
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        AndroidKeyEvent.KEYCODE_BUTTON_L1,
                                        AndroidKeyEvent.KEYCODE_BUTTON_L2 -> {
                                            dropdownFocusedIndex = if (dropdownFocusedIndex < 0) 0
                                            else (dropdownFocusedIndex - 1).coerceAtLeast(0)
                                            true
                                        }

                                        AndroidKeyEvent.KEYCODE_BUTTON_R1,
                                        AndroidKeyEvent.KEYCODE_BUTTON_R2 -> {
                                            dropdownFocusedIndex = if (dropdownFocusedIndex < 0) 0
                                            else (dropdownFocusedIndex + 1).coerceAtMost(maxIdx)
                                            true
                                        }

                                        AndroidKeyEvent.KEYCODE_BUTTON_A -> {
                                            val idx = dropdownFocusedIndex
                                            if (idx in dropdownItems.indices) {
                                                viewModel.updateQuery("@${dropdownItems[idx]} ")
                                                dropdownFocusedIndex = -1
                                                true
                                            } else false
                                        }

                                        else -> false
                                    }
                                }
                        ) {
                            Surface(
                                color = if (romSearchUseWallpaper) Color.Transparent else OledBackgroundColor,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LaunchedEffect(Unit) {
                                    filteredGames.firstOrNull()?.let { game ->
                                        viewModel.updateFocusedGame(game)
                                    }
                                }
                                RomResultsGrid(
                                    games = filteredGames,
                                    isLoading = isLoading,
                                    focusResetKey = focusResetCounter,
                                    isHiddenMode = isHiddenMode,
                                    backgroundTransparent = romSearchUseWallpaper,
                                    cardMediaType = cardMediaType,
                                    focusAnimationEnabled = focusAnimationEnabled,
                                    focusAnimationDelayMs = esdeState.romSearchFocusAnimationDelayMs,
                                    isFocusAnimationDisabled = { game -> gameKey(game) in focusAnimationDisabledGames },
                                    onToggleGameDiscSpin = { game ->
                                        val key = gameKey(game)
                                        if (key in focusAnimationDisabledGames) esdePrefs.enableFocusAnimation(
                                            key
                                        )
                                        else esdePrefs.disableFocusAnimation(key)
                                    },
                                    getGameMediaType = { game ->
                                        gameMediaMap[gameKey(game)]
                                            ?.let { runCatching { RomSearchCardMediaType.valueOf(it) }.getOrNull() }
                                            ?: systemMediaMap[game.systemName]
                                                ?.let { runCatching { RomSearchCardMediaType.valueOf(it) }.getOrNull() }
                                    },
                                    onSetGameMediaType = { game, type ->
                                        if (type == null) esdePrefs.clearGameMediaType(gameKey(game))
                                        else esdePrefs.setGameMediaType(gameKey(game), type)
                                    },
                                    onSetMediaTypeForSystem = { game, type ->
                                        if (type == null) esdePrefs.clearSystemMediaType(game.systemName)
                                        else esdePrefs.setSystemMediaType(game.systemName, type)
                                        filteredGames.filter { it.systemName == game.systemName }
                                            .forEach { esdePrefs.clearGameMediaType(gameKey(it)) }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    onLaunchGame = { game ->
                                        if (romSearchStateHolder.isSelectMode.value) {
                                            romSearchStateHolder.pendingRomForPin.value =
                                                romSearchStateHolder.pendingSelectPageIndex.value to game
                                            romSearchStateHolder.isSelectMode.value = false
                                            romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                                            dismiss()
                                        } else {
                                            romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                                            val pkg = esdePrefs.getGameEmulator(gameKey(game))
                                                ?: game.emulatorPackage ?: game.path
                                            romLauncher.launchGame(game, context, localAppDisplayPreferenceManager.getAppDisplayPreference(pkg))
                                        }
                                    },
                                    onSaveEmulator = { game, pkg, cmd ->
                                        esdePrefs.setGameEmulator(gameKey(game), pkg)
                                        cmd?.let {
                                            esdePrefs.setGameLaunchCommand(
                                                gameKey(game),
                                                it
                                            )
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
                                    onHideGame = { game -> esdePrefs.hideGame(hiddenGameKey(game)) },
                                    onUnhideGame = { game -> esdePrefs.unhideGame(hiddenGameKey(game)) },
                                    onUnhideAllGames = { games ->
                                        esdePrefs.unhideAllGames(games.map { hiddenGameKey(it) })
                                    },
                                    onToggleHintAndKeyboard = {
                                        val newVisible = !romSearchStateHolder.hintAndKbVisible.value
                                        romSearchStateHolder.hintAndKbVisible.value = newVisible
                                        esdePrefs.setRomSearchHintsKbVisible(newVisible)
                                    },
                                    onAndroidAppInfo = { game ->
                                        val pkg = game.path.trimEnd('/').removeSuffix(".app")
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = "package:$pkg".toUri()
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        context.startActivity(intent)
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
                                        val pkg = esdePrefs.getGameEmulator(gameKey(game)) ?: game.emulatorPackage ?: game.path
                                        romLauncher.launchGame(game, context, localAppDisplayPreferenceManager.getAppDisplayPreference(pkg))
                                    },
                                    onChangeFolder = { game ->
                                        pendingFolderChangeSystem = game.systemName
                                        val romPath =
                                            resolveRomPath(game, esdePrefs.state.value.romsPaths)
                                        val hint = romPath?.let {
                                            val dir = File(it).parent ?: "/storage/emulated/0"
                                            "content://com.android.externalstorage.documents/document/${
                                                Uri.encode("primary:${dir.removePrefix("/storage/emulated/0/")}")
                                            }".toUri()
                                        }
                                        safTreeLauncher.launch(hint)
                                    },
                                )
                            }

                            if (dropdownVisible) {
                                PlatformSuggestionsDropdown(
                                    platforms = platformSuggestions,
                                    showAllApps = romSearchShowAllAndroidApps,
                                    focusedIndex = dropdownFocusedIndex,
                                    autoFilter = esdeState.romSearchPlatformAutoFilter,
                                    onPlatformSelected = { platform ->
                                        viewModel.updateQuery("@$platform ")
                                        if (!esdeState.romSearchPlatformAutoFilter) {
                                            dropdownFocusedIndex = -1
                                        }
                                    },
                                    onPlatformFocused = { platform -> autoFilterPlatform = platform },
                                    platformImagesEnabled = platformImagesEnabled,
                                    getPlatformImage = { platform -> platformImageMap[platform.lowercase()] },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun signalGameLaunch() {
        gameLaunched = true
        managers.feature.jinglesManager.onGameLaunched()
    }
}
