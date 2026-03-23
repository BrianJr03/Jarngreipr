package jr.brian.home.esde.ui

import android.content.Context
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.R
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.hiddenGameKey
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RomSearchResultsActivity : ComponentActivity() {
    @Inject lateinit var managers: ManagerContainer
    @Inject lateinit var esdePrefs: ESDEPreferencesManager
    @Inject lateinit var romSearchStateHolder: RomSearchStateHolder

    private val viewModel: RomSearchResultsViewModel by viewModels()

    private var pendingFolderChangeSystem: String? = null
    private lateinit var romLauncher: RomGameLauncher

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
        val systemName = pendingFolderChangeSystem ?: romLauncher.pendingGameLaunch?.first?.systemName

        val treeDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Exception) { null }

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

        romLauncher.pendingGameLaunch?.let { (game, ctx) -> romLauncher.launchGame(game, ctx) }
        romLauncher.pendingGameLaunch = null
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
        romLauncher = RomGameLauncher(
            activity = this,
            esdePrefs = esdePrefs,
            onSignalGameLaunch = ::signalGameLaunch,
            onLaunchSafPicker = { uri -> safTreeLauncher.launch(uri) }
        )
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
                    val romSearchUseWallpaper = esdeState.romSearchUseWallpaper

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
                        allGames, query, selectedPlatform, isPlatformMode, isHiddenMode, hiddenGames
                    ) {
                        val list = when {
                            isHiddenMode ->
                                allGames.filter { hiddenGameKey(it) in hiddenGames }
                            selectedPlatform != null ->
                                allGames.filter { it.systemName.equals(selectedPlatform, ignoreCase = true) }
                            isPlatformMode && platformSearch != null ->
                                allGames.filter { it.systemName.contains(platformSearch, ignoreCase = true) }
                            query.isBlank() -> allGames
                            else -> allGames.filter { game ->
                                game.name.contains(query, ignoreCase = true) ||
                                        game.systemName.contains(query, ignoreCase = true) ||
                                        game.genre?.contains(query, ignoreCase = true) == true ||
                                        game.developer?.contains(query, ignoreCase = true) == true ||
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
                                color = if (romSearchUseWallpaper) Color.Transparent else OledBackgroundColor,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                RomResultsGrid(
                                    games = filteredGames,
                                    isLoading = isLoading,
                                    isHiddenMode = isHiddenMode,
                                    backgroundTransparent = romSearchUseWallpaper,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    onLaunchGame = { game ->
                                        romSearchStateHolder.screenDismissSignal.tryEmit(Unit)
                                        romLauncher.launchGame(game, context)
                                    },
                                    onSaveEmulator = { game, pkg, cmd ->
                                        esdePrefs.setGameEmulator(gameKey(game), pkg)
                                        cmd?.let { esdePrefs.setGameLaunchCommand(gameKey(game), it) }
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
                                        romLauncher.launchGame(game, context)
                                    },
                                    onChangeFolder = { game ->
                                        pendingFolderChangeSystem = game.systemName
                                        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths)
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

    private fun signalGameLaunch() {
        romSearchStateHolder.gameLaunchSignal.tryEmit(Unit)
        managers.feature.jinglesManager.onGameLaunched()
    }
}
