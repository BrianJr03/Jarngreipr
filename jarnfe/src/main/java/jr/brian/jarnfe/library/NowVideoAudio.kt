package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import jr.brian.jarnfe.audio.JarnAudio

/** Says Preview sound is on for this window, so jingles wait to see whether a preview plays. */
@Composable
internal fun NowVideoSoundMode(preview: NowVideoPreview) {
    if (!preview.enabled || preview.muted) return
    DisposableEffect(Unit) {
        JarnAudio.setPreviewSoundOn(true)
        onDispose { JarnAudio.setPreviewSoundOn(false) }
    }
}

/**
 * Claims Jarn FE audio for [gameId] as soon as its preview is wanted with sound, so music has
 * settled and the jingle has yielded before the first frame. A muted preview never claims.
 */
@Composable
internal fun NowVideoSoundClaim(gameId: String, claims: Boolean) {
    if (!claims) return
    DisposableEffect(gameId) {
        JarnAudio.claimPreview(gameId)
        onDispose { JarnAudio.releasePreview(gameId) }
    }
}

/** Audible only while unmuted, on screen with a frame drawn, and nothing else has the speakers. */
@Composable
internal fun rememberNowVideoAudible(muted: Boolean, showing: Boolean): Boolean {
    val gate by JarnAudio.gateState.collectAsState()
    return !muted && showing && gate.previewMayPlay
}
