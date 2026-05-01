package jr.brian.home.esde.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import java.io.File

@Composable
internal fun XrFocusedGamePanel(
    game: GameInfo?,
    currentMediaType: RomSearchCardMediaType?,
    globalMediaType: RomSearchCardMediaType,
    isXrActive: Boolean,
    scrollState: ScrollState,
    onLaunchGame: (GameInfo) -> Unit,
    onSaveEmulator: (GameInfo, String, String?) -> Unit,
    onCoreSelected: (GameInfo, String, String) -> Unit,
    onHide: (GameInfo) -> Unit,
    isRetroArchGame: (GameInfo) -> Boolean,
    hasSavedCore: (GameInfo) -> Boolean,
    hasSavedEmulator: (GameInfo) -> Boolean,
    modifier: Modifier = Modifier
) {
    var showEmulatorPicker by remember(game?.path) { mutableStateOf(false) }
    var showCorePicker by remember(game?.path) { mutableStateOf(false) }
    val context = LocalContext.current

    game?.let { g ->
        if (showCorePicker) {
            RetroArchCorePickerDialog(
                onDismiss = { showCorePicker = false },
                onCoreSelected = { displayName, corePath ->
                    onCoreSelected(g, displayName, corePath)
                    showCorePicker = false
                }
            )
        } else if (showEmulatorPicker) {
            EmulatorPickerDialog(
                game = g,
                onDismiss = { showEmulatorPicker = false },
                onEmulatorSelected = { pkg, cmd ->
                    onSaveEmulator(g, pkg, cmd)
                    Toast.makeText(context, "Emulator saved", Toast.LENGTH_SHORT).show()
                    showEmulatorPicker = false
                    if (pkg.startsWith("com.retroarch")) showCorePicker = true
                }
            )
        }
    }

    Surface(
        color = OledBackgroundColor,
        modifier = modifier
            .fillMaxSize()
            .then(if (isXrActive) Modifier.border(1.dp, ThemeAccentColor.copy(alpha = 0.35f)) else Modifier)
    ) {
        if (game == null) {
            XrNoGameSelectedPlaceholder()
        } else {
            XrGameContent(
                game = game,
                scrollState = scrollState,
                currentMediaType = currentMediaType,
                globalMediaType = globalMediaType,
                onLaunch = {
                    val isAndroid = game.systemName.equals("androidgames", ignoreCase = true) ||
                            game.systemName.equals("androidapps", ignoreCase = true)
                    val needsCore = !isAndroid && isRetroArchGame(game) && !hasSavedCore(game)
                    val hasEmulator = hasSavedEmulator(game) ||
                            game.launchCommand != null || game.emulatorPackage != null
                    when {
                        isAndroid || (hasEmulator && !needsCore) -> onLaunchGame(game)
                        needsCore -> showCorePicker = true
                        else -> showEmulatorPicker = true
                    }
                },
                onPickEmulator = { showEmulatorPicker = true },
                onChangeCore = { showCorePicker = true },
                onHide = { onHide(game) }
            )
        }
    }
}

@Composable
private fun XrNoGameSelectedPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.rom_search_xr_no_game_focused),
            color = Color.White.copy(alpha = 0.35f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun XrGameContent(
    game: GameInfo,
    scrollState: ScrollState,
    currentMediaType: RomSearchCardMediaType?,
    globalMediaType: RomSearchCardMediaType,
    onLaunch: () -> Unit,
    onPickEmulator: () -> Unit,
    onChangeCore: () -> Unit,
    onHide: () -> Unit
) {
    val imageLoader = LocalESDEImageLoader.current
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            XrGameHeader(game)
            effectiveMediaPath(game, currentMediaType ?: globalMediaType)?.let { path ->
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
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }
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
            game.description?.let { desc ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        XrGameActionButtons(
            game = game,
            onLaunch = onLaunch,
            onPickEmulator = onPickEmulator,
            onChangeCore = onChangeCore,
            onHide = onHide
        )
    }
}

@Composable
private fun XrGameHeader(game: GameInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = game.name,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = game.systemName.uppercase(),
            color = ThemeAccentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun XrGameActionButtons(
    game: GameInfo,
    onLaunch: () -> Unit,
    onPickEmulator: () -> Unit,
    onChangeCore: () -> Unit,
    onHide: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.End
    ) {
        if (game.emulatorPackage != null || game.launchCommand != null) {
            TextButton(onClick = onLaunch) {
                Text(stringResource(R.string.rom_detail_launch), color = ThemeAccentColor)
            }
        }
        TextButton(onClick = onChangeCore) {
            Text(stringResource(R.string.rom_detail_change_core), color = ThemeAccentColor)
        }
        TextButton(onClick = onPickEmulator) {
            Text(stringResource(R.string.rom_detail_pick_emulator), color = ThemeAccentColor)
        }
        TextButton(onClick = onHide) {
            Text(stringResource(R.string.rom_detail_hide), color = Color.Red.copy(alpha = 0.7f))
        }
    }
}

@Composable
internal fun XrPlatformListPanel(
    platforms: List<String>,
    showAndroidApps: Boolean,
    selectedPlatform: String?,
    isAndroidMode: Boolean,
    isXrActive: Boolean,
    xrFocusedIndex: Int,
    listState: LazyListState,
    onPlatformSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OledBackgroundColor,
        modifier = modifier
            .fillMaxSize()
            .then(if (isXrActive) Modifier.border(1.dp, ThemeAccentColor.copy(alpha = 0.35f)) else Modifier)
    ) {
        Column(Modifier.fillMaxSize()) {
            XrPlatformPanelHeader()
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    XrPlatformRow(
                        name = stringResource(R.string.rom_search_xr_all_platforms),
                        isSelected = selectedPlatform == null && !isAndroidMode,
                        isXrFocused = isXrActive && xrFocusedIndex == 0,
                        onClick = { onPlatformSelected(null) }
                    )
                }
                if (showAndroidApps) {
                    item {
                        XrPlatformRow(
                            name = "APPS",
                            isSelected = isAndroidMode,
                            isXrFocused = isXrActive && xrFocusedIndex == 1,
                            onClick = { onPlatformSelected("android") }
                        )
                    }
                }
                itemsIndexed(platforms) { index, platform ->
                    val platformOffset = if (showAndroidApps) 2 else 1
                    XrPlatformRow(
                        name = platform,
                        isSelected = selectedPlatform?.equals(platform, ignoreCase = true) == true,
                        isXrFocused = isXrActive && xrFocusedIndex == (index + platformOffset),
                        onClick = { onPlatformSelected(platform) }
                    )
                }
            }
        }
    }
}

@Composable
private fun XrPlatformPanelHeader() {
    Text(
        text = stringResource(R.string.rom_search_xr_platforms_title),
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun XrPlatformRow(
    name: String,
    isSelected: Boolean,
    isXrFocused: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isSelected -> ThemeAccentColor.copy(alpha = 0.15f)
                    isXrFocused -> ThemeAccentColor.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
    ) {
        Text(
            text = name.uppercase(),
            color = when {
                isSelected -> ThemeAccentColor
                isXrFocused -> ThemeAccentColor.copy(alpha = 0.8f)
                else -> ThemeAccentColor.copy(alpha = 0.55f)
            },
            fontWeight = if (isSelected || isXrFocused) FontWeight.ExtraBold else FontWeight.Normal,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
