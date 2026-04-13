package jr.brian.home.esde.ui

import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.animatedGradientBorder
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.util.animatedColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.itemsIndexed as lazyRowItemsIndexed

private const val NUM_COLS = 4

@Composable
internal fun RomResultsGrid(
    games: List<GameInfo>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isHiddenMode: Boolean = false,
    backgroundTransparent: Boolean = false,
    cardMediaType: RomSearchCardMediaType = RomSearchCardMediaType.PhysicalMedia,
    focusAnimationEnabled: Boolean = false,
    focusAnimationDelayMs: Int = 150,
    isFocusAnimationDisabled: (GameInfo) -> Boolean = { false },
    onToggleGameDiscSpin: (GameInfo) -> Unit = {},
    getGameMediaType: (GameInfo) -> RomSearchCardMediaType? = { null },
    onSetGameMediaType: (GameInfo, RomSearchCardMediaType?) -> Unit = { _, _ -> },
    onLaunchGame: (GameInfo) -> Unit,
    onSaveEmulator: (GameInfo, String, String?) -> Unit = { _, _, _ -> },
    hasSavedEmulator: (GameInfo) -> Boolean = { false },
    onGameFocused: (GameInfo?) -> Unit = {},
    onHideGame: (GameInfo) -> Unit = {},
    onUnhideGame: (GameInfo) -> Unit = {},
    onUnhideAllGames: (List<GameInfo>) -> Unit = {},
    onToggleHintAndKeyboard: () -> Unit = {},
    onAndroidAppInfo: (GameInfo) -> Unit = {},
    isRetroArchGame: (GameInfo) -> Boolean = { false },
    hasSavedCore: (GameInfo) -> Boolean = { false },
    onCoreSelected: (GameInfo, String, String) -> Unit = { _, _, _ -> },
    onChangeFolder: (GameInfo) -> Unit = {},
    focusResetKey: Any? = Unit
) {
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf<GameInfo?>(null) }
    var showEmulatorPicker by remember { mutableStateOf(false) }
    var showCorePicker by remember { mutableStateOf(false) }
    var hiddenPlatformFilter by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val hiddenPlatforms = remember(games, isHiddenMode) {
        if (isHiddenMode) games.map { it.systemName }.distinct().sorted() else emptyList()
    }
    val displayedGames = remember(games, isHiddenMode, hiddenPlatformFilter) {
        if (isHiddenMode && hiddenPlatformFilter != null) {
            games.filter { it.systemName.equals(hiddenPlatformFilter, ignoreCase = true) }
        } else games
    }

    var focusedIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }

    LaunchedEffect(focusedIndex) {
        focusRequesters[focusedIndex]?.requestFocus()
    }

    // Caller-controlled reset: jumps to index 0 when a new filter/search is applied.
    // Does NOT fire when clearing the query (caller keeps the key stable then).
    LaunchedEffect(focusResetKey) {
        if (displayedGames.isNotEmpty()) {
            focusedIndex = 0
            repeat(3) {
                delay(80)
                runCatching { focusRequesters[0]?.requestFocus() }
            }
        }
    }

    fun moveFocus(delta: Int) {
        val next = (focusedIndex + delta).coerceIn(0, displayedGames.size - 1)
        if (next == focusedIndex) return
        focusedIndex = next
        coroutineScope.launch {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            // In hidden mode the grid has a header item at slot 0, so data indices are offset by 1.
            val gridIndex = if (isHiddenMode) next + 1 else next
            val isVisible = visibleItems.any { it.index == gridIndex }
            if (!isVisible) {
                listState.animateScrollToItem(
                    index = gridIndex,
                    scrollOffset = 0
                )
            }
        }
    }

    Box(
        modifier = modifier.then(
            if (selectedGame != null)
                Modifier else
                Modifier.padding(horizontal = 8.dp)
        )
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ThemeAccentColor
                )
            }

//            !isHiddenMode && displayedGames.isEmpty() -> {
//                Text(
//                    text = stringResource(R.string.rom_search_no_results),
//                    modifier = Modifier.align(Alignment.Center),
//                    color = Color.White.copy(alpha = 0.5f),
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }

            else -> {
                LaunchedEffect(Unit) {
                    moveFocus(1)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(NUM_COLS),
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (backgroundTransparent) Color.Transparent else OledBackgroundColor)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    moveFocus(1); true
                                }

                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    moveFocus(-1); true
                                }

                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    moveFocus(NUM_COLS); true
                                }

                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    moveFocus(-NUM_COLS); true
                                }

                                else -> false
                            }
                        },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isHiddenMode) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HiddenModeHeader(
                                platforms = hiddenPlatforms,
                                activePlatform = hiddenPlatformFilter,
                                visibleCount = displayedGames.size,
                                onPlatformToggle = { platform ->
                                    hiddenPlatformFilter =
                                        if (hiddenPlatformFilter == platform) null else platform
                                },
                                onUnhideAll = { onUnhideAllGames(displayedGames) }
                            )
                        }
                    }
                    itemsIndexed(
                        displayedGames,
                        key = { _, game -> "${game.systemName}/${game.path}" }
                    ) { index, game ->
                        val focusRequester = remember { FocusRequester() }

                        DisposableEffect(index) {
                            focusRequesters[index] = focusRequester
                            onDispose { focusRequesters.remove(index) }
                        }

                        RomResultCard(
                            game = game,
                            focusRequester = focusRequester,
                            mediaType = getGameMediaType(game) ?: cardMediaType,
                            focusAnimationEnabled = focusAnimationEnabled,
                            focusAnimationDelayMs = focusAnimationDelayMs,
                            isFocusAnimationDisabled = isFocusAnimationDisabled(game),
                            flipEnabled = focusAnimationEnabled,
                            flipDisabledForGame = isFocusAnimationDisabled(game),
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
                            onLongClick = {
                                val isAndroid =
                                    game.systemName.equals("androidgames", ignoreCase = true) ||
                                            game.systemName.equals(
                                                "androidapps",
                                                ignoreCase = true
                                            ) || game.systemName.equals("games", ignoreCase = true)
                                if (isAndroid) onAndroidAppInfo(game)
                                else selectedGame = game
                            },
                            onFocused = {
                                if (focusedIndex != index) {
                                    focusedIndex = index
                                }
                                onGameFocused(game)
                            },
                            onToggleKeyboard = onToggleHintAndKeyboard
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
                    currentMediaType = getGameMediaType(game),
                    globalMediaType = cardMediaType,
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
                    },
                    onSetMediaType = { type -> onSetGameMediaType(game, type) },
                    discSpinEnabled = focusAnimationEnabled,
                    discSpinDisabled = isFocusAnimationDisabled(game),
                    onToggleDiscSpin = { onToggleGameDiscSpin(game) }
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
private fun HiddenModeHeader(
    platforms: List<String>,
    activePlatform: String?,
    visibleCount: Int,
    onPlatformToggle: (String) -> Unit,
    onUnhideAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (platforms.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                lazyRowItems(platforms) { platform ->
                    val selected = activePlatform == platform
                    TextButton(
                        onClick = { onPlatformToggle(platform) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) ThemeAccentColor.copy(alpha = 0.2f) else OledBackgroundColor)
                    ) {
                        Text(
                            text = platform.uppercase(),
                            color = if (selected) ThemeAccentColor else ThemeAccentColor.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (visibleCount > 0) {
            TextButton(
                onClick = onUnhideAll,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Red.copy(alpha = 0.08f))
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Unhide All${if (activePlatform != null) " ($activePlatform)" else ""} ($visibleCount)",
                    color = Color.Red.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = stringResource(R.string.rom_search_hidden_no_results),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun PlatformSuggestionsDropdown(
    platforms: List<String>,
    onPlatformSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAllApps: Boolean = false,
    focusedIndex: Int = -1,
    autoFilter: Boolean = false,
    onPlatformFocused: (String) -> Unit = {},
    platformImagesEnabled: Boolean = false,
    getPlatformImage: (String) -> Uri? = { null }
) {
    val allItems = remember(showAllApps, platforms) {
        buildList { if (showAllApps) add("android"); addAll(platforms) }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(focusedIndex) {
        if (focusedIndex >= 0 && focusedIndex < allItems.size) {
            coroutineScope.launch { listState.animateScrollToItem(focusedIndex) }
            if (autoFilter) onPlatformFocused(allItems[focusedIndex])
        }
    }

    var rowHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val verticalPadding by remember(rowHeightPx) {
        derivedStateOf { with(density) { (rowHeightPx / 2).toDp() } }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(OledCardColor.copy(alpha = 0.97f))
            .padding(horizontal = 8.dp)
            .padding(vertical = verticalPadding / 2)
    ) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .onSizeChanged { rowHeightPx = it.height }
                .graphicsLayer { clip = false }
        ) {
            lazyRowItemsIndexed(allItems) { index, item ->
                val isFocused = index == focusedIndex
                val imageUri =
                    if (platformImagesEnabled && item != "android") getPlatformImage(item) else null

                val sharedModifier = Modifier
                    .size(width = 64.dp, height = 64.dp)
                    .scale(animatedFocusedScale(isFocused))
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isFocused && imageUri == null) Modifier.background(
                            ThemeAccentColor.copy(alpha = 0.18f)
                        ) else Modifier
                    )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (imageUri) {
                        null -> {
                            TextButton(
                                onClick = { onPlatformSelected(item) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = sharedModifier
                            ) {
                                Text(
                                    text = when (item) {
                                        "android" -> "APPS"
                                        "androidgames" -> "GAMES"
                                        else -> item.uppercase()
                                    },
                                    color = if (isFocused) ThemeAccentColor
                                    else ThemeAccentColor.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            }
                        }

                        else -> {
                            val context = LocalContext.current
                            val gifImageLoader = remember(context) {
                                ImageLoader.Builder(context)
                                    .components {
                                        add(ImageDecoderDecoder.Factory())
                                    }
                                    .build()
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUri)
                                    .crossfade(true)
                                    .build(),
                                imageLoader = gifImageLoader,
                                contentDescription = item,
                                contentScale = ContentScale.FillBounds,
                                modifier = sharedModifier.clickable { onPlatformSelected(item) }
                            )
                        }
                    }

                    if (isFocused) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            color = animatedColor(),
                            modifier = Modifier
                                .width(64.dp)
                                .scale(animatedFocusedScale(true))
                        )
                    }
                }
            }
        }
    }
}