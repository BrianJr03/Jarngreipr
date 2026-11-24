package jr.brian.jarnfe.audio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.jarnfe.R

@Composable
internal fun JarnMusicPage(
    data: JarnAudioData,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val music = data.music
    var muted by remember { mutableStateOf(music.muted) }
    val pickFolder = rememberAudioFolderPicker { data.setMusicFolder(it.toString()) }
    val pickFile = rememberAudioFilePicker { data.setMusicFile(it.toString()) }
    AudioPage(stringResource(R.string.jarnfe_audio_music), onBack, modifier) {
        JarnToggleRow(
            label = stringResource(R.string.jarnfe_audio_music_play),
            checked = !muted,
            onCheckedChange = { play ->
                muted = !play
                data.setMusicMuted(!play)
                JarnAudio.syncMusic()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        MusicSourceRow(
            label = R.string.jarnfe_audio_music_folder,
            active = music.source == JarnMusicSource.Folder,
            name = music.folderName,
            empty = R.string.jarnfe_audio_music_folder_empty,
            onClick = pickFolder,
        )
        MusicSourceRow(
            label = R.string.jarnfe_audio_music_file,
            active = music.source == JarnMusicSource.File,
            name = music.fileName,
            empty = R.string.jarnfe_audio_music_file_empty,
            onClick = pickFile,
        )
        AudioVolumeRow(stringResource(R.string.jarnfe_audio_volume), music.volume, !muted) { level ->
            val wasSilent = music.volume == 0f
            data.setMusicVolume(level)
            if (wasSilent) JarnAudio.syncMusic()
        }
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_audio_volume_help_title),
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MusicSourceRow(label: Int, active: Boolean, name: String?, empty: Int, onClick: () -> Unit) {
    val supporting = when {
        name == null -> stringResource(empty)
        active -> stringResource(R.string.jarnfe_audio_music_playing_from, name)
        else -> name
    }
    JarnNavigateRow(
        label = stringResource(label),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = supporting,
    )
}

@Composable
internal fun JarnJinglesPage(
    data: JarnAudioData,
    onBack: () -> Unit,
    onRepos: () -> Unit,
    onFolders: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flow = remember(data) { data.jingles }
    val settings by flow.collectAsState(initial = JarnJingleSettings())
    AudioPage(stringResource(R.string.jarnfe_audio_jingles), onBack, modifier) {
        JingleToggle(R.string.jarnfe_audio_jingles_enable, R.string.jarnfe_audio_jingles_enable_hint, settings.enabled, data::setJinglesEnabled)
        JingleToggle(R.string.jarnfe_audio_jingles_mute, R.string.jarnfe_audio_jingles_mute_hint, settings.muted, data::setJinglesMuted)
        AudioVolumeRow(stringResource(R.string.jarnfe_audio_volume), settings.volume, !settings.muted, data::setJingleVolume)
        JingleToggle(R.string.jarnfe_audio_jingles_regex, R.string.jarnfe_audio_jingles_regex_hint, settings.regexPriority, data::setRegexPriority)
        JingleToggle(R.string.jarnfe_audio_jingles_normalize, R.string.jarnfe_audio_jingles_normalize_hint, settings.normalize, data::setNormalize)
        JingleLink(R.string.jarnfe_audio_repos, R.string.jarnfe_audio_repos_hint, onRepos)
        JingleLink(R.string.jarnfe_audio_folders, R.string.jarnfe_audio_folders_hint, onFolders)
        JingleLink(R.string.jarnfe_audio_about_title, R.string.jarnfe_audio_about_hint, onAbout)
    }
}

@Composable
private fun JingleToggle(label: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    JarnToggleRow(
        label = stringResource(label),
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(hint),
    )
}

@Composable
private fun JingleLink(label: Int, hint: Int, onClick: () -> Unit) {
    JarnNavigateRow(
        label = stringResource(label),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(hint),
    )
}
