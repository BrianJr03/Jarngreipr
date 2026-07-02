package jr.brian.home.esde.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.ui.components.dialog.DimmedBottomSheet
import jr.brian.home.ui.theme.OledBackgroundColor
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

    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.rom_emulator_picker_title),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.rom_detail_close))
            }
        }
    }
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

    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.rom_emulator_choose_app_title),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.rom_detail_close))
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

    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Select RetroArch Core",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (cores.isEmpty()) {
                Text(
                    text = "No cores found. Download cores in RetroArch → Online Updater → Core Downloader.",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.rom_detail_close))
            }
        }
    }
}

fun effectiveMediaPath(game: GameInfo, type: RomSearchCardMediaType): String? = when (type) {
    RomSearchCardMediaType.PhysicalMedia -> game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.Covers -> game.artworkPath ?: game.physicalMediaPath
    RomSearchCardMediaType.Screenshots -> game.screenshotPath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.Fanart -> game.fanartPath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.TitleScreens -> game.titlescreenPath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.Marquee -> game.marqueeImagePath ?: game.physicalMediaPath ?: game.artworkPath
    RomSearchCardMediaType.MixImages -> game.miximagePath ?: game.physicalMediaPath ?: game.artworkPath
}

@Composable
fun MediaTypePickerDialog(
    currentType: RomSearchCardMediaType?,
    onDismiss: () -> Unit,
    onSelected: (RomSearchCardMediaType?) -> Unit,
    systemName: String? = null,
    onSelectedForSystem: (RomSearchCardMediaType?) -> Unit = {}
) {
    var scopeAllSystem by remember { mutableStateOf(false) }
    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.rom_detail_media_type_dialog_title),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!systemName.isNullOrBlank()) {
                    MediaTypeScopeToggle(
                        systemName = systemName,
                        scopeAllSystem = scopeAllSystem,
                        onScopeChange = { scopeAllSystem = it }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    )
                }
                val selectType: (RomSearchCardMediaType?) -> Unit = { type ->
                    if (scopeAllSystem) onSelectedForSystem(type) else onSelected(type)
                }
                TextButton(
                    onClick = { selectType(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val defaultLabelRes = when {
                        scopeAllSystem && currentType == null ->
                            R.string.rom_detail_media_type_default_option_system_selected
                        scopeAllSystem ->
                            R.string.rom_detail_media_type_default_option_system
                        currentType == null ->
                            R.string.rom_detail_media_type_default_option_selected
                        else ->
                            R.string.rom_detail_media_type_default_option
                    }
                    Text(
                        text = stringResource(defaultLabelRes),
                        color = if (!scopeAllSystem && currentType == null) Color.White
                        else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )
                RomSearchCardMediaType.entries.forEach { type ->
                    TextButton(
                        onClick = { selectType(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isSelected = !scopeAllSystem && currentType == type
                        Text(
                            text = if (isSelected)
                                stringResource(R.string.rom_detail_media_type_option_selected, type.displayName)
                            else type.displayName,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.rom_detail_close))
            }
        }
    }
}

@Composable
private fun MediaTypeScopeToggle(
    systemName: String,
    scopeAllSystem: Boolean,
    onScopeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaTypeScopeChip(
            label = stringResource(R.string.rom_detail_media_scope_this_game),
            selected = !scopeAllSystem,
            onClick = { onScopeChange(false) },
            modifier = Modifier.weight(1f)
        )
        MediaTypeScopeChip(
            label = stringResource(R.string.rom_detail_media_scope_all_system, systemName.uppercase()),
            selected = scopeAllSystem,
            onClick = { onScopeChange(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MediaTypeScopeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (selected) Modifier.background(ThemeAccentColor.copy(alpha = 0.2f))
                else Modifier.background(OledBackgroundColor)
            )
    ) {
        Text(
            text = label,
            color = if (selected) ThemeAccentColor else ThemeAccentColor.copy(alpha = 0.6f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RomDetailRow(label: String, value: String) {
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
