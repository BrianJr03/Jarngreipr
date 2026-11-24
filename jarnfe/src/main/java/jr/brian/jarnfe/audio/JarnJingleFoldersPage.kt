package jr.brian.jarnfe.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasModalSheet
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.utilities.UtilityConfirmDialog
import jr.brian.jarnfe.utilities.UtilityNote
import kotlinx.coroutines.launch

private sealed interface FolderSheet {
    data class Actions(val folder: JarnJingleFolder) : FolderSheet
    data class ConfirmRemove(val folder: JarnJingleFolder) : FolderSheet
}

@Composable
internal fun JarnJingleFoldersPage(
    data: JarnAudioData,
    onBack: () -> Unit,
    onEditPack: (JarnJingleFolder?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val foldersFlow = remember(data) { data.folders }
    val folders by foldersFlow.collectAsState(initial = emptyList())
    var sheet by remember { mutableStateOf<FolderSheet?>(null) }
    val pickFolder = rememberAudioFolderPicker { uri -> JarnNotices.show(data.addFolder(uri.toString()).noticeRes()) }
    Box(modifier.fillMaxSize()) {
        AudioPage(stringResource(R.string.jarnfe_audio_folders), onBack) {
            FolderLink(R.string.jarnfe_audio_folder_add, R.string.jarnfe_audio_folder_add_hint, pickFolder)
            FolderLink(R.string.jarnfe_audio_pack_create, R.string.jarnfe_audio_pack_create_hint) { onEditPack(null) }
            FolderLink(R.string.jarnfe_audio_folder_refresh, R.string.jarnfe_audio_folder_refresh_hint) {
                data.refreshFolders()
                JarnNotices.show(R.string.jarnfe_audio_folder_refreshed)
            }
            if (folders.isEmpty()) UtilityNote(stringResource(R.string.jarnfe_audio_folder_empty))
            folders.forEach { folder ->
                JarnNavigateRow(
                    label = folder.name,
                    onClick = { sheet = FolderSheet.Actions(folder) },
                    modifier = Modifier.fillMaxWidth(),
                    supporting = folder.entryCount?.let {
                        stringResource(R.string.jarnfe_audio_repo_online, it)
                    } ?: stringResource(R.string.jarnfe_audio_folder_unreadable),
                )
            }
        }
        FolderSheetHost(sheet, data, onEditPack, { sheet = it }) { sheet = null }
    }
}

private fun JarnJingleFolderResult.noticeRes(): Int = when (this) {
    JarnJingleFolderResult.Added -> R.string.jarnfe_audio_folder_added
    JarnJingleFolderResult.Duplicate -> R.string.jarnfe_audio_folder_duplicate
    JarnJingleFolderResult.Invalid -> R.string.jarnfe_audio_folder_invalid
}

@Composable
private fun FolderLink(label: Int, hint: Int, onClick: () -> Unit) {
    JarnNavigateRow(
        label = stringResource(label),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(hint),
    )
}

@Composable
private fun FolderSheetHost(
    sheet: FolderSheet?,
    data: JarnAudioData,
    onEditPack: (JarnJingleFolder?) -> Unit,
    onSheet: (FolderSheet) -> Unit,
    close: () -> Unit,
) {
    when (sheet) {
        null -> Unit
        is FolderSheet.Actions -> {
            val first = remember { FocusRequester() }
            CanvasModalSheet(title = sheet.folder.name, initialFocus = first, onClose = close) {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_audio_pack_update),
                    onClick = {
                        close()
                        onEditPack(sheet.folder)
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(first),
                )
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_audio_remove),
                    onClick = { onSheet(FolderSheet.ConfirmRemove(sheet.folder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        is FolderSheet.ConfirmRemove -> UtilityConfirmDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_audio_folder_remove_title),
            message = stringResource(R.string.jarnfe_audio_folder_remove_message, sheet.folder.name),
            confirmLabel = stringResource(R.string.jarnfe_audio_remove),
            onConfirm = {
                data.removeFolder(sheet.folder.path)
                close()
            },
            onDismiss = close,
        )
    }
}

private enum class PackField { Pack, Game, Platform, File }

/** Adds one jingle to a new pack, or to [existingPath] when updating one. */
@Composable
internal fun JarnJinglePackPage(
    data: JarnAudioData,
    existingPath: String?,
    existingName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var packName by remember { mutableStateOf(existingName) }
    var gameName by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var audioUri by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<PackField?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pickAudio = rememberAudioFilePicker { uri ->
        audioUri = uri.toString()
        if (fileName.isBlank()) fileName = uri.lastPathSegment.orEmpty().substringAfterLast('/').substringBeforeLast('.')
    }
    val draft = JarnJinglePackDraft(packName.trim(), existingPath, gameName.trim(), platform.trim(), fileName.trim(), audioUri.orEmpty())
    val save: (String) -> Unit = { parent ->
        saving = true
        scope.launch {
            val saved = data.savePackEntry(parent, draft)
            saving = false
            JarnNotices.show(if (saved) R.string.jarnfe_audio_pack_saved else R.string.jarnfe_audio_pack_failed)
            if (saved) onBack()
        }
    }
    val pickParent = rememberAudioFolderPicker { uri ->
        data.setPackParent(uri.toString())
        data.packParent()?.let(save) ?: JarnNotices.show(R.string.jarnfe_audio_pack_failed)
    }
    Box(modifier.fillMaxSize()) {
        AudioPage(stringResource(if (existingPath == null) R.string.jarnfe_audio_pack_create else R.string.jarnfe_audio_pack_update), onBack) {
            if (existingPath == null) PackValueRow(R.string.jarnfe_audio_pack_name, packName) { editing = PackField.Pack }
            PackValueRow(R.string.jarnfe_audio_pack_game, gameName) { editing = PackField.Game }
            PackValueRow(R.string.jarnfe_audio_pack_platform, platform) { editing = PackField.Platform }
            PackValueRow(R.string.jarnfe_audio_pack_file, fileName) { editing = PackField.File }
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_audio_pack_audio),
                onClick = pickAudio,
                modifier = Modifier.fillMaxWidth(),
                supporting = stringResource(
                    if (audioUri == null) R.string.jarnfe_audio_pack_audio_empty else R.string.jarnfe_audio_pack_audio_ready,
                ),
            )
            JarnButton(
                label = stringResource(R.string.jarnfe_audio_pack_save),
                onClick = {
                    when {
                        !jarnJinglePackDraftValid(draft) -> JarnNotices.show(R.string.jarnfe_audio_pack_invalid)
                        existingPath != null -> save(existingPath)
                        else -> data.packParent()?.let(save) ?: pickParent()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                loading = saving,
                focusableWhenDisabled = true,
            )
        }
        editing?.let { field ->
            val close = { editing = null }
            val current = when (field) {
                PackField.Pack -> packName
                PackField.Game -> gameName
                PackField.Platform -> platform
                PackField.File -> fileName
            }
            AudioKeyboardSheet(
                label = stringResource(field.labelRes()),
                initialText = current,
                onClose = close,
                onDone = { text ->
                    when (field) {
                        PackField.Pack -> packName = text
                        PackField.Game -> gameName = text
                        PackField.Platform -> platform = text
                        PackField.File -> fileName = text
                    }
                    close()
                },
            )
        }
    }
}

private fun PackField.labelRes(): Int = when (this) {
    PackField.Pack -> R.string.jarnfe_audio_pack_name
    PackField.Game -> R.string.jarnfe_audio_pack_game
    PackField.Platform -> R.string.jarnfe_audio_pack_platform
    PackField.File -> R.string.jarnfe_audio_pack_file
}

internal fun jarnJinglePackDraftValid(draft: JarnJinglePackDraft): Boolean =
    jarnJinglePackFieldValid(draft.packName) &&
        draft.gameName.isNotBlank() && draft.gameName.length <= AUDIO_TEXT_MAX &&
        jarnJinglePlatformValid(draft.platform) &&
        jarnJingleFileNameValid(draft.fileName) &&
        draft.audioUri.isNotBlank()

@Composable
private fun PackValueRow(label: Int, value: String, onClick: () -> Unit) {
    JarnValueRow(
        label = stringResource(label),
        value = value.ifBlank { stringResource(R.string.jarnfe_audio_pack_unset) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
