package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.wallpaper.jarnDisabledRowBacking

/** Previews only play on the bottom screen, so the group needs two screens. */
@Composable
internal fun JarnVideoPreviewSection() {
    if (!JarnSession.state.deckDisplayAvailable) return
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val display by store.displayState.collectAsState()
    VideoPreviewGroup(display, store::updateDisplay)
}

@Composable
internal fun VideoPreviewGroup(
    display: JarnDisplayPrefs,
    onUpdate: ((JarnDisplayPrefs) -> JarnDisplayPrefs) -> Unit,
) {
    SettingsGroup(R.string.jarnfe_video_previews_title) {
        VideoPreviewsRow(display.videoPreviews) { on -> onUpdate { it.copy(videoPreviews = on) } }
        PreviewSoundRow(display.videoPreviewSound, enabled = display.videoPreviews) { on ->
            onUpdate { it.copy(videoPreviewSound = on) }
        }
    }
}

@Composable
private fun VideoPreviewsRow(on: Boolean, onChange: (Boolean) -> Unit) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_video_previews_play),
        checked = on,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_video_previews_play_hint),
    )
}

/** Stays focusable while previews are off, so the D-pad walk doesn't skip it. */
@Composable
private fun PreviewSoundRow(on: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_video_previews_sound),
        checked = on,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth().jarnDisabledRowBacking(enabled),
        supporting = stringResource(R.string.jarnfe_video_previews_sound_hint),
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}
