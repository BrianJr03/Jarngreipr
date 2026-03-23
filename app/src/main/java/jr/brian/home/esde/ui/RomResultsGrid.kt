package jr.brian.home.esde.ui

import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import kotlinx.coroutines.launch

private const val NUM_COLS = 4

@Composable
internal fun RomResultsGrid(
    games: List<GameInfo>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isHiddenMode: Boolean = false,
    onLaunchGame: (GameInfo) -> Unit,
    onSaveEmulator: (GameInfo, String, String?) -> Unit = { _, _, _ -> },
    hasSavedEmulator: (GameInfo) -> Boolean = { false },
    onGameFocused: (GameInfo?) -> Unit = {},
    onHideGame: (GameInfo) -> Unit = {},
    onUnhideGame: (GameInfo) -> Unit = {},
    onToggleKeyboard: () -> Unit = {},
    isRetroArchGame: (GameInfo) -> Boolean = { false },
    hasSavedCore: (GameInfo) -> Boolean = { false },
    onCoreSelected: (GameInfo, String, String) -> Unit = { _, _, _ -> },
    onChangeFolder: (GameInfo) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf<GameInfo?>(null) }
    var showEmulatorPicker by remember { mutableStateOf(false) }
    var showCorePicker by remember { mutableStateOf(false) }

    val listState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var focusedIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }

    LaunchedEffect(focusedIndex) {
        focusRequesters[focusedIndex]?.requestFocus()
    }

    fun moveFocus(delta: Int) {
        val next = (focusedIndex + delta).coerceIn(0, games.size - 1)
        if (next == focusedIndex) return
        focusedIndex = next
        coroutineScope.launch {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == next }
            if (!isVisible) {
                listState.animateScrollToItem(
                    index = next,
                    scrollOffset = 0
                )
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
                    columns = GridCells.Fixed(NUM_COLS),
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OledBackgroundColor)
                        .padding(horizontal = 4.dp)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (keyEvent.nativeKeyEvent.keyCode) {
                               KeyEvent.KEYCODE_DPAD_RIGHT -> { moveFocus(1); true }
                                KeyEvent.KEYCODE_DPAD_LEFT  -> { moveFocus(-1); true }
                                KeyEvent.KEYCODE_DPAD_DOWN  -> { moveFocus(NUM_COLS); true }
                                KeyEvent.KEYCODE_DPAD_UP    -> { moveFocus(-NUM_COLS); true }
                                else -> false
                            }
                        },
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        games,
                        key = { _, game -> "${game.systemName}/${game.path}" }
                    ) { index, game ->
                        val focusRequester = remember { FocusRequester() }

                        DisposableEffect(index) {
                            focusRequesters[index] = focusRequester
                            onDispose { focusRequesters.remove(index) }
                        }

                        RomResultCard(
                            game = game,
                            autoFocus = index == 0 && focusedIndex == 0,
                            focusRequester = focusRequester,
                            onClick = {
                                val isAndroid =
                                    game.systemName.equals("androidgames", ignoreCase = true) ||
                                            game.systemName.equals("androidapps", ignoreCase = true)
                                val needsCore = isRetroArchGame(game) && !hasSavedCore(game)
                                val hasEmulator = hasSavedEmulator(game) ||
                                        game.launchCommand != null ||
                                        game.emulatorPackage != null
                                when {
                                    isAndroid -> onLaunchGame(game)
                                    needsCore -> {
                                        selectedGame = game
                                        showCorePicker = true
                                    }
                                    hasEmulator -> onLaunchGame(game)
                                    else -> {
                                        selectedGame = game
                                        showEmulatorPicker = true
                                    }
                                }
                            },
                            onLongClick = { selectedGame = game },
                            onFocused = {
                                if (focusedIndex != index) {
                                    focusedIndex = index
                                }
                                onGameFocused(game)
                            },
                            onToggleKeyboard = onToggleKeyboard
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedGame != null && !showEmulatorPicker && !showCorePicker,
            enter = slideInHorizontally(tween(240)) { it } + fadeIn(tween(240)),
            exit = slideOutHorizontally(tween(200)) { it } + fadeOut(tween(200))
        ) {
            selectedGame?.let { game ->
                RomDetailScreen(
                    game = game,
                    isHidden = isHiddenMode,
                    onDismiss = { selectedGame = null },
                    onLaunch = {
                        onLaunchGame(game)
                        selectedGame = null
                    },
                    onPickEmulator = { showEmulatorPicker = true },
                    onChangeCore = { showCorePicker = true },
                    onChangeFolder = {
                        onChangeFolder(game)
                        selectedGame = null
                    },
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

    selectedGame?.let { game ->
        if (showCorePicker) {
            RetroArchCorePickerDialog(
                onDismiss = {
                    showCorePicker = false
                    selectedGame = null
                },
                onCoreSelected = { displayName, corePath ->
                    onCoreSelected(game, displayName, corePath)
                    showCorePicker = false
                    selectedGame = null
                }
            )
        } else if (showEmulatorPicker) {
            EmulatorPickerDialog(
                game = game,
                onDismiss = { showEmulatorPicker = false },
                onEmulatorSelected = { pkg, cmd ->
                    onSaveEmulator(game, pkg, cmd)
                    Toast.makeText(context, "Emulator saved", Toast.LENGTH_SHORT).show()
                    showEmulatorPicker = false
                    if (pkg.startsWith("com.retroarch")) {
                        showCorePicker = true
                    } else {
                        selectedGame = null
                    }
                }
            )
        }
    }
}

@Composable
internal fun PlatformSuggestionsDropdown(
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
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
