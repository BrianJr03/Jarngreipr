package jr.brian.home.esde.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import java.io.File

@Composable
internal fun EmulatorPickerDialog(
    game: GameInfo,
    onDismiss: () -> Unit,
    onEmulatorSelected: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }

    val emulators = remember(game.systemName) {
        val findRulesFile =
            File(context.filesDir.parent ?: "", "ES-DE/custom_systems/es_find_rules.xml").let { f ->
                if (f.exists()) f
                else File("/storage/emulated/0/ES-DE/custom_systems/es_find_rules.xml")
            }
        val customRules = EsdeCommandLauncher.parseCustomRules(findRulesFile)
        val esSystemsFile =
            File(context.filesDir.parent ?: "", "ES-DE/custom_systems/es_systems.xml").let { f ->
                if (f.exists()) f
                else File("/storage/emulated/0/ES-DE/custom_systems/es_systems.xml")
            }
        val fromSystem = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, game.systemName, esSystemsFile, customRules
        )
        val fromExtension = EsdeCommandLauncher.getCompatibleEmulators(
            context,
            File(game.romAbsolutePath ?: game.path).extension
        )
        val seenPackages = fromSystem.map { it.packageName }.toHashSet()
        fromSystem + fromExtension.filter { it.packageName !in seenPackages }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { pkg ->
                onEmulatorSelected(pkg, null)
                showAppPicker = false
                onDismiss()
            }
        )
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f),
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (emulators.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rom_emulator_none_found),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    emulators.forEach { emulator ->
                        TextButton(
                            onClick = {
                                onEmulatorSelected(emulator.packageName, emulator.command)
                                onDismiss()
                            },
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )
                TextButton(
                    onClick = { showAppPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.rom_emulator_choose_app),
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    )
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
private fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val installedApps = remember {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0)
            .map { info ->
                val label = info.loadLabel(pm).toString()
                val pkg = info.activityInfo.packageName
                label to pkg
            }
            .sortedBy { it.first.lowercase() }
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f),
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        title = {
            Text(
                text = stringResource(R.string.rom_emulator_choose_app_title),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                installedApps.forEach { (label, pkg) ->
                    TextButton(
                        onClick = { onAppSelected(pkg) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
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
        modifier = Modifier.fillMaxSize().focusRequester(focusRequester)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hero image — use per-game override, fall back to global setting
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

            // Bottom action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHidden) {
                    TextButton(onClick = onPickEmulator) {
                        Text(stringResource(R.string.rom_detail_pick_emulator), color = ThemeAccentColor)
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
                        Text(stringResource(R.string.rom_detail_hide), color = Color.Red.copy(alpha = 0.7f))
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

@Composable
internal fun RetroArchCorePickerDialog(
    onDismiss: () -> Unit,
    onCoreSelected: (displayName: String, corePath: String) -> Unit
) {
    val context = LocalContext.current
    val cores = remember {
        EsdeCommandLauncher.getInstalledCores(context)
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f),
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        title = {
            Text(
                text = "Select RetroArch Core",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (cores.isEmpty()) {
                Text(
                    text = "No cores found. Download cores in RetroArch → Online Updater → Core Downloader.",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    cores.forEach { (displayName, corePath) ->
                        TextButton(
                            onClick = {
                                onCoreSelected(displayName, corePath)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayName,
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

private fun effectiveMediaPath(game: GameInfo, type: RomSearchCardMediaType): String? = when (type) {
    RomSearchCardMediaType.PhysicalMedia -> game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.Covers -> game.artworkPath ?: game.physicalMediaPath
    RomSearchCardMediaType.Screenshots -> game.screenshotPath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.Fanart -> game.fanartPath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.TitleScreens -> game.titlescreenPath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.Marquee -> game.marqueeImagePath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.MixImages -> game.miximagePath ?: game.physicalMediaPath ?: game.artworkPath
}

@Composable
private fun MediaTypePickerDialog(
    currentType: RomSearchCardMediaType?,
    onDismiss: () -> Unit,
    onSelected: (RomSearchCardMediaType?) -> Unit
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f),
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        title = {
            Text(
                text = "Card Media Type",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextButton(
                    onClick = { onSelected(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (currentType == null) "✓ Default (use global setting)" else "Default (use global setting)",
                        color = if (currentType == null) Color.White else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )
                RomSearchCardMediaType.entries.forEach { type ->
                    TextButton(
                        onClick = { onSelected(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentType == type) "✓ ${type.displayName}" else type.displayName,
                            color = if (currentType == type) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )
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
