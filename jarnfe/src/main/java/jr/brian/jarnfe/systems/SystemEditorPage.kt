package jr.brian.jarnfe.systems

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

private const val HUE_MAX = 360f
private const val HUE_STEPS = 72
private const val SATURATION_STEPS = 50
private const val OPACITY_STEPS = 20
private const val PERCENT = 100

private val BackgroundMimeTypes = arrayOf("image/png", "image/jpeg", "image/webp", "image/gif")

@Composable
internal fun SystemEditorPage(
    system: JarnManagedSystem,
    section: SystemSection,
    manager: JarnSystemManager,
    onPage: (SystemsPage) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    SystemsPageFocus(firstFocus, section)
    SystemsPageColumn {
        SystemEditorHeader(system, section) { onPage(SystemsPage.Editor(system.id, it)) }
        val first = Modifier.fillMaxWidth().focusRequester(firstFocus)
        when (section) {
            SystemSection.Appearance -> AppearanceRows(system, manager, first)
            SystemSection.Color -> ColorRows(system, manager, first)
            SystemSection.Storage -> StorageRows(system, manager, first)
            SystemSection.Emulator -> EmulatorRows(system, manager, first) {
                onPage(SystemsPage.Emulators(system.id))
            }
            SystemSection.Actions -> ActionRows(system, manager, first) {
                onPage(SystemsPage.HiddenGames(system.id))
            }
        }
    }
}

@Composable
private fun SystemEditorHeader(
    system: JarnManagedSystem,
    section: SystemSection,
    onSection: (SystemSection) -> Unit,
) {
    val sections = SystemSection.entries
    JarnText(system.title, JarnTheme.baseType.heading, JarnTheme.colors.textHigh, maxLines = 1)
    JarnText(systemCountText(system), JarnTheme.baseType.label, JarnTheme.colors.textMid, maxLines = 1)
    JarnTabs(
        labels = sections.map { stringResource(it.labelRes) },
        selectedIndex = sections.indexOf(section),
        onSelect = { onSection(sections[it]) },
    )
}

/** "10 games", or "10 games, 1 hidden" once a game is hidden. */
@Composable
private fun systemCountText(system: JarnManagedSystem): String {
    val total = pluralStringResource(R.plurals.jarnfe_systems_game_count, system.gameCount, system.gameCount)
    val hidden = system.hiddenGameCount
    if (hidden <= 0) return total
    return pluralStringResource(R.plurals.jarnfe_systems_game_count_with_hidden, hidden, total, hidden)
}

@Composable
private fun AppearanceRows(system: JarnManagedSystem, manager: JarnSystemManager, first: Modifier) {
    val appearance = system.appearance
    val update = { next: JarnSystemAppearance -> manager.setAppearance(system.id, next) }
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_systems_show_in_library),
        checked = !system.isHidden,
        onCheckedChange = { manager.setHidden(system.id, !it) },
        modifier = first,
    )
    BackgroundRows(R.string.jarnfe_systems_tile_background, appearance.backgroundUri) {
        update(appearance.copy(backgroundUri = it))
    }
    BackgroundRows(
        labelRes = R.string.jarnfe_systems_now_background,
        uri = appearance.focusBackgroundUri,
        supporting = nowBackgroundHint(),
    ) {
        update(appearance.copy(focusBackgroundUri = it))
    }
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_systems_show_name),
        supporting = stringResource(R.string.jarnfe_systems_show_name_hint),
        checked = appearance.showName,
        onCheckedChange = { update(appearance.copy(showName = it)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun nowBackgroundHint(): String? {
    val hint = stringResource(R.string.jarnfe_systems_now_background_hint)
    return if (rememberJarnDisplay().systemBackgroundVisible) null else hint
}

@Composable
private fun BackgroundRows(
    @StringRes labelRes: Int,
    uri: String?,
    supporting: String? = null,
    onChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(picked, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onChange(picked.toString())
        }
    }
    JarnValueRow(
        label = stringResource(labelRes),
        value = stringResource(if (uri == null) R.string.jarnfe_systems_choose else R.string.jarnfe_systems_change),
        onClick = {
            runCatching { picker.launch(BackgroundMimeTypes) }
                .onFailure { JarnNotices.show(R.string.jarnfe_systems_picker_unavailable) }
        },
        modifier = Modifier.fillMaxWidth(),
        supporting = supporting,
    )
    if (uri != null) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_systems_remove_image, stringResource(labelRes)),
            onClick = { onChange(null) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColorRows(system: JarnManagedSystem, manager: JarnSystemManager, first: Modifier) {
    val appearance = system.appearance
    val stored = appearance.solidColorArgb
    var held by remember { mutableStateOf(SystemColor.fromArgb(stored)) }
    val color = if (stored != null && held.toArgb() != stored) SystemColor.fromArgb(stored) else held
    val commit = { next: SystemColor ->
        held = next
        manager.setAppearance(system.id, appearance.copy(solidColorArgb = next.toArgb()))
    }
    ColorPresetRow(R.string.jarnfe_systems_color_default, stored == null, first) {
        held = SystemColor()
        manager.setAppearance(system.id, appearance.copy(solidColorArgb = null))
    }
    ColorPresetRow(R.string.jarnfe_systems_color_transparent, stored == 0L, Modifier.fillMaxWidth()) {
        held = SystemColor(alpha = 0f)
        manager.setAppearance(system.id, appearance.copy(solidColorArgb = 0L))
    }
    JarnSliderRow(
        label = stringResource(R.string.jarnfe_systems_color_hue),
        value = color.hue,
        valueText = stringResource(R.string.jarnfe_systems_degrees, color.hue.toInt()),
        onValueChange = { commit(color.copy(hue = it)) },
        valueRange = 0f..HUE_MAX,
        steps = HUE_STEPS,
    )
    ColorPercentRow(R.string.jarnfe_systems_color_saturation, color.saturation, SATURATION_STEPS) {
        commit(color.copy(saturation = it))
    }
    ColorPercentRow(R.string.jarnfe_systems_color_brightness, color.brightness, SATURATION_STEPS) {
        commit(color.copy(brightness = it))
    }
    ColorPercentRow(R.string.jarnfe_systems_color_opacity, color.alpha, OPACITY_STEPS) {
        commit(color.copy(alpha = it))
    }
}

@Composable
private fun ColorPresetRow(
    @StringRes labelRes: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    JarnValueRow(
        label = stringResource(labelRes),
        value = stringResource(if (selected) R.string.jarnfe_art_in_use else R.string.jarnfe_art_use),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ColorPercentRow(@StringRes labelRes: Int, value: Float, steps: Int, onChange: (Float) -> Unit) {
    JarnSliderRow(
        label = stringResource(labelRes),
        value = value,
        valueText = stringResource(R.string.jarnfe_systems_percent, (value * PERCENT).toInt()),
        onValueChange = onChange,
        steps = steps,
    )
}

@Composable
private fun StorageRows(system: JarnManagedSystem, manager: JarnSystemManager, first: Modifier) {
    val scope = rememberCoroutineScope()
    var status by remember(system.id) { mutableStateOf<FolderStatus>(FolderStatus.Idle) }
    var confirmClear by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            status = FolderStatus.Checking
            scope.launch { status = saveSystemFolder(manager, system.id, uri) }
        }
    }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_systems_folder),
        supporting = folderSupportingText(system, status),
        onClick = {
            runCatching { picker.launch(null) }
                .onFailure { JarnNotices.show(R.string.jarnfe_launch_folder_unavailable) }
        },
        modifier = first,
        enabled = !system.folderIsAdded,
        focusableWhenDisabled = true,
    )
    if (system.folderPath != null && !system.folderIsAdded) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_systems_clear_folder),
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (confirmClear) {
        SystemConfirmDialog(
            title = stringResource(R.string.jarnfe_systems_clear_folder),
            message = stringResource(R.string.jarnfe_systems_clear_folder_message),
            confirmLabel = stringResource(R.string.jarnfe_systems_clear),
            onConfirm = {
                manager.clearFolder(system.id)
                status = FolderStatus.Idle
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

private sealed interface FolderStatus {
    data object Idle : FolderStatus
    data object Checking : FolderStatus
    data object Failed : FolderStatus
    data class Checked(val result: JarnFolderCheck) : FolderStatus
}

private suspend fun saveSystemFolder(
    manager: JarnSystemManager,
    systemId: String,
    uri: Uri,
): FolderStatus {
    if (!manager.setFolder(systemId, uri)) return FolderStatus.Failed
    return FolderStatus.Checked(manager.checkFolder(systemId, uri))
}

@Composable
private fun folderSupportingText(system: JarnManagedSystem, status: FolderStatus): String {
    val path = system.folderPath ?: stringResource(R.string.jarnfe_systems_folder_none)
    val detail = when (status) {
        FolderStatus.Idle -> if (system.folderIsAdded) stringResource(R.string.jarnfe_systems_folder_added) else null
        FolderStatus.Checking -> stringResource(R.string.jarnfe_systems_folder_checking)
        FolderStatus.Failed -> stringResource(R.string.jarnfe_systems_folder_failed)
        is FolderStatus.Checked -> folderCheckText(status.result)
    }
    return listOfNotNull(path, detail).joinToString("\n")
}

@Composable
internal fun folderCheckText(result: JarnFolderCheck): String = when (result) {
    is JarnFolderCheck.Found ->
        pluralStringResource(R.plurals.jarnfe_systems_folder_found, result.gameCount, result.gameCount)
    JarnFolderCheck.NoGames -> stringResource(R.string.jarnfe_systems_folder_no_games)
    JarnFolderCheck.AccessLost -> stringResource(R.string.jarnfe_systems_folder_access_lost)
}

@Composable
private fun EmulatorRows(
    system: JarnManagedSystem,
    manager: JarnSystemManager,
    first: Modifier,
    onChoose: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    JarnValueRow(
        label = stringResource(R.string.jarnfe_systems_default_emulator),
        value = system.emulatorLabel ?: stringResource(R.string.jarnfe_game_not_set),
        onClick = onChoose,
        modifier = first,
        supporting = stringResource(R.string.jarnfe_systems_default_emulator_hint),
    )
    if (system.emulatorLabel != null) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_systems_clear_emulator),
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (confirmClear) {
        SystemConfirmDialog(
            title = stringResource(R.string.jarnfe_systems_clear_emulator),
            message = stringResource(R.string.jarnfe_systems_clear_emulator_message),
            confirmLabel = stringResource(R.string.jarnfe_systems_clear),
            onConfirm = {
                manager.clearEmulator(system.id)
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun ActionRows(
    system: JarnManagedSystem,
    manager: JarnSystemManager,
    first: Modifier,
    onHiddenGames: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_systems_reset),
        supporting = stringResource(R.string.jarnfe_systems_reset_hint),
        onClick = { confirmReset = true },
        modifier = first,
    )
    HiddenGamesRow(system.hiddenGameCount, onHiddenGames)
    if (confirmReset) {
        SystemConfirmDialog(
            title = stringResource(R.string.jarnfe_systems_reset),
            message = stringResource(R.string.jarnfe_systems_reset_message, system.title),
            confirmLabel = stringResource(R.string.jarnfe_systems_reset_confirm),
            onConfirm = {
                manager.resetAppearance(system.id)
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Destructive choices open with focus on Cancel. */
@Composable
internal fun SystemConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onDismiss)
        },
    ) {
        JarnDialog(
            visible = true,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            dismissLabel = stringResource(R.string.jarnfe_systems_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )
    }
}
