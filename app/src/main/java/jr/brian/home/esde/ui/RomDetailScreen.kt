package jr.brian.home.esde.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import jr.brian.home.esde.util.ESDEMediaConstants
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import java.io.File

@Composable
internal fun RomDetailScreen(
    game: GameInfo,
    isHidden: Boolean = false,
    currentMediaType: RomSearchCardMediaType? = null,
    globalMediaType: RomSearchCardMediaType = RomSearchCardMediaType.PhysicalMedia,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onPickEmulator: () -> Unit = {},
    onChangeCore: () -> Unit = {},
    onChangeFolder: () -> Unit = {},
    onHide: () -> Unit = {},
    onUnhide: () -> Unit = {},
    onSetMediaType: (RomSearchCardMediaType?) -> Unit = {},
    discSpinEnabled: Boolean = false,
    discSpinDisabled: Boolean = false,
    onToggleDiscSpin: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val imageLoader = LocalESDEImageLoader.current
    val context = LocalContext.current
    var showMediaTypePicker by remember { mutableStateOf(false) }

    if (showMediaTypePicker) {
        MediaTypePickerDialog(
            currentType = currentMediaType,
            onDismiss = { showMediaTypePicker = false },
            onSelected = { type ->
                onSetMediaType(type)
                showMediaTypePicker = false
            }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler(onBack = onDismiss)

    Surface(
        color = OledBackgroundColor,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.rom_detail_close),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = game.name,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = game.systemName.uppercase(),
                    color = ThemeAccentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                game.genre?.let { RomDetailRow(stringResource(R.string.rom_detail_genre), it) }
                game.developer?.let {
                    RomDetailRow(
                        stringResource(R.string.rom_detail_developer),
                        it
                    )
                }
                game.publisher?.let {
                    RomDetailRow(
                        stringResource(R.string.rom_detail_publisher),
                        it
                    )
                }
                game.players?.let { RomDetailRow(stringResource(R.string.rom_detail_players), it) }
                if (game.rating > 0f) {
                    RomDetailRow(
                        stringResource(R.string.rom_detail_rating),
                        "%.0f%%".format(game.rating * 100)
                    )
                }
                if (game.playCount > 0) {
                    RomDetailRow(
                        stringResource(R.string.rom_detail_play_count),
                        game.playCount.toString()
                    )
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

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHidden) {
                    TextButton(onClick = onPickEmulator) {
                        Text(
                            stringResource(R.string.rom_detail_pick_emulator),
                            color = ThemeAccentColor
                        )
                    }
                    TextButton(onClick = onChangeCore) {
                        Text("Change Core", color = ThemeAccentColor)
                    }
                    TextButton(onClick = onChangeFolder) {
                        Text("Change Folder", color = ThemeAccentColor)
                    }
                    TextButton(onClick = { showMediaTypePicker = true }) {
                        Text(
                            text = "Media: ${currentMediaType?.displayName ?: "Default"}",
                            color = ThemeAccentColor
                        )
                    }
                    if (discSpinEnabled && game.systemName.lowercase() in ESDEMediaConstants.DISC_PLATFORMS) {
                        TextButton(onClick = onToggleDiscSpin) {
                            Text(
                                text = "Spin: ${if (discSpinDisabled) "Off" else "On"}",
                                color = ThemeAccentColor
                            )
                        }
                    }
                }
                if (isHidden) {
                    TextButton(onClick = onUnhide) {
                        Text(stringResource(R.string.rom_detail_unhide), color = ThemeAccentColor)
                    }
                } else {
                    TextButton(onClick = onHide) {
                        Text(
                            stringResource(R.string.rom_detail_hide),
                            color = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
                if (game.emulatorPackage != null || game.launchCommand != null) {
                    TextButton(onClick = onLaunch) {
                        Text(stringResource(R.string.rom_detail_launch), color = ThemeAccentColor)
                    }
                }
            }
        }
    }
}