package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.shell.JarnNotices

/** Once ever, for a game with a video while previews are off and could otherwise play. */
internal fun shouldShowVideoHint(
    hasVideo: Boolean,
    previewsOn: Boolean,
    canPlay: Boolean,
    alreadyShown: Boolean,
): Boolean = hasVideo && !previewsOn && canPlay && !alreadyShown

/** Tells someone with previews off that [videoPath]'s game has one, the first time it opens. */
@Composable
internal fun JarnVideoPreviewHintEffect(videoPath: String?) {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val player = rememberJarnGameVideos()
    LaunchedEffect(videoPath) {
        val show = shouldShowVideoHint(
            hasVideo = videoPath != null,
            previewsOn = store.display.videoPreviews,
            canPlay = player != null && JarnSession.state.deckDisplayAvailable,
            alreadyShown = store.videoHintShown,
        )
        if (!show) return@LaunchedEffect
        JarnNotices.show(R.string.jarnfe_video_previews_hint)
        store.markVideoHintShown()
    }
}
