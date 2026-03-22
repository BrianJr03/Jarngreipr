package jr.brian.home.esde.ui

import android.app.ActivityOptions
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.R
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ManagerCompositionLocalProvider
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.viewmodels.RomSearchResultsViewModel
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RomSearchResultsActivity : ComponentActivity() {

    @Inject
    lateinit var managers: ManagerContainer

    @Inject
    lateinit var esdePrefs: ESDEPreferencesManager

    @Inject
    lateinit var romSearchStateHolder: RomSearchStateHolder

    private val viewModel: RomSearchResultsViewModel by viewModels()

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

                    LaunchedEffect(Unit) {
                        isVisible = true
                    }

                    val query by viewModel.query.collectAsStateWithLifecycle()
                    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
                    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                    val focusedGame by viewModel.focusedGame.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.dismissSignal.collect { dismiss() }
                    }

                    BackHandler { dismiss() }

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
                    val filteredGames = remember(allGames, query, selectedPlatform, isPlatformMode, isHiddenMode, hiddenGames) {
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
                        if (isHiddenMode) deduped else deduped.filter { hiddenGameKey(it) !in hiddenGames }
                    }

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
                                    onLaunchGame = { game -> launchGame(game) },
                                    onLaunchWithEmulator = { game, pkg -> launchGameWithEmulator(game, pkg) },
                                    onSaveEmulator = { game, pkg -> esdePrefs.setGameEmulator(gameKey(game), pkg) },
                                    hasSavedEmulator = { game -> esdePrefs.getGameEmulator(gameKey(game)) != null },
                                    onGameFocused = { game -> viewModel.updateFocusedGame(game) },
                                    onHideGame = { game -> esdePrefs.hideGame(hiddenGameKey(game)) },
                                    onUnhideGame = { game -> esdePrefs.unhideGame(hiddenGameKey(game)) },
                                    onToggleKeyboard = {
                                        romSearchStateHolder.keyboardVisible.value =
                                            !romSearchStateHolder.keyboardVisible.value
                                    }
                                )
                            }

                            if (isPlatformMode && selectedPlatform == null && platformSuggestions.isNotEmpty()) {
                                PlatformSuggestionsDropdown(
                                    platforms = platformSuggestions,
                                    onPlatformSelected = { platform -> viewModel.updateQuery("@$platform") },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchGame(game: GameInfo) {
        val savedPkg = esdePrefs.getGameEmulator(gameKey(game))
        if (savedPkg != null && game.romAbsolutePath != null) {
            launchGameWithEmulator(game, savedPkg)
            return
        }

        val romPath = game.romAbsolutePath
        val command = game.launchCommand

        if (romPath != null && command != null) {
            val customRules = EsdeCommandLauncher.parseCustomRules(
                File(filesDir.parent ?: "", "ES-DE/custom_systems/es_find_rules.xml").let { f ->
                    if (f.exists()) f
                    else File("/storage/emulated/0/ES-DE/custom_systems/es_find_rules.xml")
                }
            )
            val intent = EsdeCommandLauncher.buildIntent(
                launchCommand = command,
                romAbsPath = romPath,
                context = this,
                customRules = customRules
            )
            if (intent != null) {
                try {
                    val options = ActivityOptions.makeBasic()
                    options.launchDisplayId = 0
                    signalGameLaunch()
                    startActivity(intent, options.toBundle())
                    finish()
                    return
                } catch (e: Exception) {
                    Log.w("RomSearchResults", "Direct launch failed, falling back to app open", e)
                }
            }
        }

        val pkg = game.emulatorPackage ?: run { finish(); return }

        // Try direct ROM launch via package-specific intent before falling back to app-open
        if (game.romAbsolutePath != null) {
            try {
                val romIntent = EsdeCommandLauncher.buildRomIntentFromPackage(
                    packageName = pkg,
                    romAbsPath = game.romAbsolutePath,
                    context = this
                )
                val options = ActivityOptions.makeBasic()
                options.launchDisplayId = 0
                signalGameLaunch()
                startActivity(romIntent, options.toBundle())
                finish()
                return
            } catch (e: Exception) {
                Log.w("RomSearchResults", "Package ROM launch failed, falling back to app open", e)
            }
        }

        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                val options = ActivityOptions.makeBasic()
                options.launchDisplayId = 0
                startActivity(intent, options.toBundle())
            }
        } catch (_: Exception) {}
        signalGameLaunch()
        finish()
    }

    private fun signalGameLaunch() {
        romSearchStateHolder.gameLaunchSignal.tryEmit(Unit)
    }

    private fun gameKey(game: GameInfo) = game.systemName
    private fun hiddenGameKey(game: GameInfo) = "${game.systemName}/${game.path}"

    private fun launchGameWithEmulator(game: GameInfo, emulatorPackage: String) {
        esdePrefs.setGameEmulator(gameKey(game), emulatorPackage)
        val romPath = game.romAbsolutePath ?: run { finish(); return }
        try {
            val intent = EsdeCommandLauncher.buildRomIntentFromPackage(
                packageName = emulatorPackage,
                romAbsPath = romPath,
                context = this
            )
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = 0
            signalGameLaunch()
            startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Log.w("RomSearchResults", "Emulator picker launch failed for $emulatorPackage", e)
        }
        finish()
    }
}

@Composable
private fun PlatformSuggestionsDropdown(
    platforms: List<String>,
    onPlatformSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(OledCardColor.copy(alpha = 0.97f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            lazyRowItems(platforms) { platform ->
                TextButton(
                    onClick = { onPlatformSelected(platform) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OledBackgroundColor)
                ) {
                    Text(
                        text = platform.uppercase(),
                        color = ThemeAccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RomResultsGrid(
    games: List<GameInfo>,
    isLoading: Boolean,
    isHiddenMode: Boolean = false,
    modifier: Modifier = Modifier,
    onLaunchGame: (GameInfo) -> Unit,
    onLaunchWithEmulator: (GameInfo, String) -> Unit = { _, _ -> },
    onSaveEmulator: (GameInfo, String) -> Unit = { _, _ -> },
    hasSavedEmulator: (GameInfo) -> Boolean = { false },
    onGameFocused: (GameInfo?) -> Unit = {},
    onHideGame: (GameInfo) -> Unit = {},
    onUnhideGame: (GameInfo) -> Unit = {},
    onToggleKeyboard: () -> Unit = {}
) {
    val columns = 4
    var selectedGame by remember { mutableStateOf<GameInfo?>(null) }
    var showEmulatorPicker by remember { mutableStateOf(false) }
    val focusRequesters = remember(games.size) { List(games.size) { FocusRequester() } }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val scope = rememberCoroutineScope()

    fun navigateToItem(targetIndex: Int) {
        if (targetIndex < 0 || targetIndex >= focusRequesters.size) return
        val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
        if (isVisible) {
            try { focusRequesters[targetIndex].requestFocus() } catch (_: Exception) {}
        } else {
            scope.launch {
                gridState.scrollToItem(targetIndex)
                try { focusRequesters[targetIndex].requestFocus() } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ThemeAccentColor
                )
            }

            games.isEmpty() -> {
                Text(
                    text = stringResource(
                        if (isHiddenMode) R.string.rom_search_hidden_no_results
                        else R.string.rom_search_no_results
                    ),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OledBackgroundColor)
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(games, key = { _, game -> "${game.systemName}/${game.path}" }) { index, game ->
                        RomResultCard(
                            game = game,
                            autoFocus = index == 0,
                            focusRequester = focusRequesters[index],
                            onClick = {
                                if (hasSavedEmulator(game)) {
                                    onLaunchGame(game)
                                } else {
                                    selectedGame = game
                                    showEmulatorPicker = true
                                }
                            },
                            onLongClick = { selectedGame = game },
                            onFocused = { onGameFocused(game) },
                            onNavigateDown = { navigateToItem(index + columns) },
                            onNavigateUp = { navigateToItem(index - columns) },
                            onToggleKeyboard = onToggleKeyboard
                        )
                    }
                }
            }
        }
    }

    selectedGame?.let { game ->
        if (showEmulatorPicker) {
            EmulatorPickerDialog(
                game = game,
                onDismiss = { showEmulatorPicker = false },
                onEmulatorSelected = { pkg ->
                    onSaveEmulator(game, pkg)
                    showEmulatorPicker = false
                    selectedGame = null
                }
            )
        } else {
            RomDetailDialog(
                game = game,
                isHidden = isHiddenMode,
                onDismiss = { selectedGame = null },
                onLaunch = {
                    onLaunchGame(game)
                    selectedGame = null
                },
                onPickEmulator = { showEmulatorPicker = true },
                onHide = {
                    onHideGame(game)
                    selectedGame = null
                },
                onUnhide = {
                    onUnhideGame(game)
                    selectedGame = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RomResultCard(
    game: GameInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit = {},
    autoFocus: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onNavigateDown: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onToggleKeyboard: () -> Unit = {}
) {
    val imageLoader = LocalESDEImageLoader.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val gradient = cardGradient()
    var isFocused by remember { mutableStateOf(false) }
    val scale = animatedFocusedScale(isFocused)

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val imageData = remember(game.physicalMediaPath, game.artworkPath) {
        (game.physicalMediaPath ?: game.artworkPath)?.let { File(it) }
    }
    val hasImage = imageData != null
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .scale(scale)
            .then(
                if (!hasImage) Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), shape)
                else Modifier
            )
            .clip(shape)
            .background(gradient)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onFocused()
                }
            }
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        onNavigateDown()
                        true
                    }
                    keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        onNavigateUp()
                        true
                    }
                    keyEvent.type == KeyEventType.KeyUp &&
                        keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BUTTON_SELECT-> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleKeyboard()
                        true
                    }
                    keyEvent.type == KeyEventType.KeyUp &&
                            keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BUTTON_START -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                        true
                    }
                    else -> false
                }
            }
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (hasImage) 1f else 4f / 3f)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                if (hasImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageData)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = game.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = game.name,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )
                }

                if (game.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ThemeAccentColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }

                if (!hasImage) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = game.systemName.uppercase(),
                            color = ThemeAccentColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmulatorPickerDialog(
    game: GameInfo,
    onDismiss: () -> Unit,
    onEmulatorSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val extension = File(game.romAbsolutePath ?: game.path).extension
    val emulators = remember(extension) {
        EsdeCommandLauncher.getCompatibleEmulators(context, extension)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        title = {
            Text(
                text = stringResource(R.string.rom_emulator_picker_title),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (emulators.isEmpty()) {
                Text(
                    text = stringResource(R.string.rom_emulator_none_found),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column {
                    emulators.forEach { emulator ->
                        TextButton(
                            onClick = { onEmulatorSelected(emulator.packageName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = emulator.displayName,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.rom_detail_close))
            }
        }
    )
}

@Composable
private fun RomDetailDialog(
    game: GameInfo,
    isHidden: Boolean = false,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onPickEmulator: () -> Unit = {},
    onHide: () -> Unit = {},
    onUnhide: () -> Unit = {}
) {
    val imageLoader = LocalESDEImageLoader.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        title = {
            Text(
                text = game.name,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (game.physicalMediaPath ?: game.artworkPath)?.let { path ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(path))
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = game.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                RomDetailRow(stringResource(R.string.rom_detail_system), game.systemName.uppercase())
                game.genre?.let { RomDetailRow(stringResource(R.string.rom_detail_genre), it) }
                game.developer?.let { RomDetailRow(stringResource(R.string.rom_detail_developer), it) }
                game.publisher?.let { RomDetailRow(stringResource(R.string.rom_detail_publisher), it) }
                game.players?.let { RomDetailRow(stringResource(R.string.rom_detail_players), it) }
                if (game.rating > 0f) {
                    RomDetailRow(
                        stringResource(R.string.rom_detail_rating),
                        "%.0f%%".format(game.rating * 100)
                    )
                }
                if (game.playCount > 0) {
                    RomDetailRow(stringResource(R.string.rom_detail_play_count), game.playCount.toString())
                }
                if (game.playTimeMinutes > 0) {
                    val hours = game.playTimeMinutes / 60
                    val minutes = game.playTimeMinutes % 60
                    val timeStr = if (hours > 0) {
                        stringResource(R.string.rom_detail_playtime_hm, hours, minutes)
                    } else {
                        stringResource(R.string.rom_detail_playtime_m, minutes)
                    }
                    RomDetailRow(stringResource(R.string.rom_detail_playtime), timeStr)
                }
                game.description?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            if (game.emulatorPackage != null || game.launchCommand != null) {
                TextButton(onClick = onLaunch) {
                    Text(stringResource(R.string.rom_detail_launch))
                }
            }
        },
        dismissButton = {
            Row {
                if (!isHidden) {
                    TextButton(onClick = onPickEmulator) {
                        Text(
                            text = stringResource(R.string.rom_detail_pick_emulator),
                            color = ThemeAccentColor
                        )
                    }
                }
                if (isHidden) {
                    TextButton(onClick = onUnhide) {
                        Text(
                            text = stringResource(R.string.rom_detail_unhide),
                            color = ThemeAccentColor
                        )
                    }
                } else {
                    TextButton(onClick = onHide) {
                        Text(
                            text = stringResource(R.string.rom_detail_hide),
                            color = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.rom_detail_close))
                }
            }
        }
    )
}

@Composable
private fun FocusedGameInfoBar(
    game: GameInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledCardColor.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = game.name,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoChip(stringResource(R.string.rom_detail_system), game.systemName.uppercase())
            game.genre?.let { InfoChip(stringResource(R.string.rom_detail_genre), it) }
            if (game.playCount > 0) {
                InfoChip(stringResource(R.string.rom_detail_play_count), game.playCount.toString())
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ThemeAccentColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun RomDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ThemeAccentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
    }
}
