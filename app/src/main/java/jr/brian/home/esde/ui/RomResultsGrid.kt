package jr.brian.home.esde.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private const val NUM_COLS = 4

@Composable
internal fun RomResultsGrid(
    games: List<GameInfo>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isHiddenMode: Boolean = false,
    onLaunchGame: (GameInfo) -> Unit,
    onLaunchWithEmulator: (GameInfo, String) -> Unit = { _, _ -> },
    onSaveEmulator: (GameInfo, String, String?) -> Unit = { _, _, _ -> },
    hasSavedEmulator: (GameInfo) -> Boolean = { false },
    onGameFocused: (GameInfo?) -> Unit = {},
    onHideGame: (GameInfo) -> Unit = {},
    onUnhideGame: (GameInfo) -> Unit = {},
    onToggleKeyboard: () -> Unit = {},
    isRetroArchGame: (GameInfo) -> Boolean = { false },
    hasSavedCore: (GameInfo) -> Boolean = { false },
    onCoreSelected: (GameInfo, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf<GameInfo?>(null) }
    var showEmulatorPicker by remember { mutableStateOf(false) }
    var showCorePicker by remember { mutableStateOf(false) }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OledBackgroundColor)
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        games,
                        key = { _, game -> "${game.systemName}/${game.path}" }
                    ) { index, game ->
                        RomResultCard(
                            game = game,
                            autoFocus = index == 0,
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
                            onFocused = { onGameFocused(game) },
                            onToggleKeyboard = onToggleKeyboard
                        )
                    }
                }
            }
        }
    }

    selectedGame?.let { game ->
        when {
            showCorePicker -> RetroArchCorePickerDialog(
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
            showEmulatorPicker -> EmulatorPickerDialog(
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
            else -> RomDetailDialog(
                game = game,
                isHidden = isHiddenMode,
                isRetroArch = isRetroArchGame(game),
                onDismiss = { selectedGame = null },
                onLaunch = {
                    onLaunchGame(game)
                    selectedGame = null
                },
                onPickEmulator = { showEmulatorPicker = true },
                onChangeCore = { showCorePicker = true },
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
