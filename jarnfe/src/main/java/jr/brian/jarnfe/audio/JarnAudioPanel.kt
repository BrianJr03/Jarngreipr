package jr.brian.jarnfe.audio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.utilities.JarnFaqSnippet
import jr.brian.jarnfe.utilities.UtilityNote
import jr.brian.jarnfe.utilities.UtilityReader

private enum class AudioRoute { Menu, Music, MusicHelp, Jingles, JinglesAbout, Repos, Folders, Pack }

/** The row that opens [JarnAudioPanel] from Settings. */
@Composable
internal fun JarnAudioSettingsRow(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_audio_title),
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_audio_settings_hint),
    )
}

/** The entry point for music and jingles, ready to host from Settings. */
@Composable
internal fun JarnAudioPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnAudioDataOrNull() }
    var routeName by rememberSaveable { mutableStateOf(AudioRoute.Menu.name) }
    var packPath by rememberSaveable { mutableStateOf<String?>(null) }
    var packName by rememberSaveable { mutableStateOf("") }
    val go: (AudioRoute) -> Unit = { routeName = it.name }
    if (data == null) {
        AudioUnavailable(onClose, modifier)
        return
    }
    when (AudioRoute.valueOf(routeName)) {
        AudioRoute.Menu -> AudioMenu(go, onClose, modifier)
        AudioRoute.Music -> JarnMusicPage(data, { go(AudioRoute.Menu) }, { go(AudioRoute.MusicHelp) }, modifier)
        AudioRoute.MusicHelp -> AudioVolumeHelp({ go(AudioRoute.Music) }, modifier)
        AudioRoute.Jingles -> JarnJinglesPage(
            data = data,
            onBack = { go(AudioRoute.Menu) },
            onRepos = { go(AudioRoute.Repos) },
            onFolders = { go(AudioRoute.Folders) },
            onAbout = { go(AudioRoute.JinglesAbout) },
            modifier = modifier,
        )
        AudioRoute.JinglesAbout -> AudioJinglesAbout({ go(AudioRoute.Jingles) }, modifier)
        AudioRoute.Repos -> JarnJingleReposPage(data, { go(AudioRoute.Jingles) }, modifier)
        AudioRoute.Folders -> JarnJingleFoldersPage(
            data = data,
            onBack = { go(AudioRoute.Jingles) },
            onEditPack = { folder ->
                packPath = folder?.path
                packName = folder?.name.orEmpty()
                go(AudioRoute.Pack)
            },
            modifier = modifier,
        )
        AudioRoute.Pack -> JarnJinglePackPage(data, packPath, packName, { go(AudioRoute.Folders) }, modifier)
    }
}

@Composable
private fun AudioMenu(go: (AudioRoute) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    AudioPage(stringResource(R.string.jarnfe_audio_title), onClose, modifier) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_audio_music),
            onClick = { go(AudioRoute.Music) },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_audio_music_hint),
        )
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_audio_jingles),
            onClick = { go(AudioRoute.Jingles) },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_audio_jingles_hint),
        )
    }
}

@Composable
private fun AudioUnavailable(onClose: () -> Unit, modifier: Modifier) {
    AudioPage(stringResource(R.string.jarnfe_audio_title), onClose, modifier) {
        UtilityNote(stringResource(R.string.jarnfe_audio_unavailable))
    }
}

@Composable
private fun AudioVolumeHelp(onBack: () -> Unit, modifier: Modifier) {
    val packageName = LocalContext.current.packageName
    UtilityReader(
        title = stringResource(R.string.jarnfe_audio_volume_help_title),
        body = stringResource(R.string.jarnfe_audio_volume_help_body),
        onBack = onBack,
        modifier = modifier,
        snippets = listOf(
            JarnFaqSnippet(
                label = stringResource(R.string.jarnfe_audio_volume_help_command),
                text = stringResource(R.string.jarnfe_audio_volume_help_adb, packageName),
            ),
        ),
    )
}

@Composable
private fun AudioJinglesAbout(onBack: () -> Unit, modifier: Modifier) {
    UtilityReader(
        title = stringResource(R.string.jarnfe_audio_about_title),
        body = stringResource(R.string.jarnfe_audio_about_body),
        onBack = onBack,
        modifier = modifier,
    )
}
