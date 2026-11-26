package jr.brian.jarnfe.audio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.shell.JarnCornerToast
import jr.brian.jarnfe.shell.JarnCornerToasts
import jr.brian.jarnfe.shell.JarnToastIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * What L3 and R3 do while "Mute with L3 and R3" is on. They flip the same switches as the Music
 * and Jingles pages, so the choice is saved and backed up, and confirm it in the corner pop-up.
 */
internal object JarnStickMute {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onJingles(data: JarnAudioData?) {
        data ?: return
        scope.launch { toggleJingles(data) }
    }

    fun onMusic(data: JarnAudioData?) {
        data ?: return
        toggleMusic(data)
    }

    /** Returns whether jingles are now muted. */
    suspend fun toggleJingles(data: JarnAudioData): Boolean {
        val muted = !data.jingles.first().muted
        data.setJinglesMuted(muted)
        showJinglesToast(muted)
        return muted
    }

    /** Returns whether music is now muted. */
    fun toggleMusic(data: JarnAudioData): Boolean {
        val muted = !data.music.muted
        data.setMusicMuted(muted)
        JarnAudio.syncMusic()
        showMusicToast(muted)
        return muted
    }

    private fun showJinglesToast(muted: Boolean) = JarnCornerToasts.show(jinglesToast(muted))

    private fun showMusicToast(muted: Boolean) = JarnCornerToasts.show(musicToast(muted))
}

internal const val JARN_MUTE_JINGLES_TOAST_KEY = "mute_jingles"
internal const val JARN_MUTE_MUSIC_TOAST_KEY = "mute_music"

internal fun jinglesToast(muted: Boolean) = JarnCornerToast(
    icon = JarnToastIcon.Res(if (muted) R.drawable.ic_jarn_bell_off else R.drawable.ic_jarn_bell),
    title = if (muted) R.string.jarnfe_audio_toast_jingles_muted else R.string.jarnfe_audio_toast_jingles_on,
    subtitle = R.string.jarnfe_audio_toast_l3,
    key = JARN_MUTE_JINGLES_TOAST_KEY,
)

internal fun musicToast(muted: Boolean) = JarnCornerToast(
    icon = JarnToastIcon.Res(if (muted) R.drawable.ic_jarn_music_off else R.drawable.ic_jarn_music),
    title = if (muted) R.string.jarnfe_audio_toast_music_muted else R.string.jarnfe_audio_toast_music_on,
    subtitle = R.string.jarnfe_audio_toast_r3,
    key = JARN_MUTE_MUSIC_TOAST_KEY,
)

@Composable
internal fun JarnStickMuteSetting() {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val display = store.displayState.collectAsState().value
    JarnStickMuteRow(display.stickMute) { on -> store.updateDisplay { it.copy(stickMute = on) } }
}

@Composable
internal fun JarnStickMuteRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_audio_stick_mute),
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_audio_stick_mute_hint),
    )
}
