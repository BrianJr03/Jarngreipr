package jr.brian.home.ui.components.settings.sections

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.theme.managers.LocalBgMusicManager

@Composable
fun MusicSection(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val bgMusicManager = LocalBgMusicManager.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { bgMusicManager.setFolderUri(it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { bgMusicManager.setSingleFileUri(it) }
    }

    val folderDescription = bgMusicManager.folderUri
        ?.let { uri ->
            uri.toUri().lastPathSegment?.substringAfterLast('/')
                ?.let { stringResource(R.string.music_section_select_folder_selected, it) }
        }
        ?: stringResource(R.string.music_section_select_folder_description)

    val fileDescription = bgMusicManager.singleFileUri
        ?.let { uri ->
            uri.toUri().lastPathSegment?.substringAfterLast('/')
                ?.let { stringResource(R.string.music_section_select_file_selected, it) }
        }
        ?: stringResource(R.string.music_section_select_file_description)

    CollapsibleSettingsSection(
        title = stringResource(R.string.music_section_title),
        icon = Icons.Default.MusicNote,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingItem(
                title = stringResource(R.string.music_section_select_folder_title),
                description = folderDescription,
                icon = Icons.Default.Folder,
                onClick = { folderPickerLauncher.launch(null) }
            )

            SettingItem(
                title = stringResource(R.string.music_section_select_file_title),
                description = fileDescription,
                icon = Icons.Default.MusicNote,
                onClick = { filePickerLauncher.launch(arrayOf("audio/*")) }
            )

            SliderSetting(
                title = stringResource(R.string.music_section_volume_title),
                value = bgMusicManager.vol,
                valueRange = 0f..1f,
                steps = 19,
                valueText = "${(bgMusicManager.vol * 100).toInt()}%",
                onValueChange = { bgMusicManager.setVolume(it) },
                description = stringResource(R.string.music_section_volume_description)
            )
        }
    }
}
