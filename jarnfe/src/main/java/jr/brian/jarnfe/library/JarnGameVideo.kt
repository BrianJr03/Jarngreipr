package jr.brian.jarnfe.library

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.StateFlow

/** [Ready] means the first frame is on screen, so art behind it can go. */
enum class JarnGameVideoState { Loading, Ready, Failed }

/**
 * One looping game preview, muted until [setMuted] says otherwise. It starts playing as soon as
 * it can. The screen that made it owns it and must call [release] when it leaves.
 */
interface JarnGameVideo {
    /** Supports alpha and clipping, so it can fade in over art. */
    val view: View
    val state: StateFlow<JarnGameVideoState>

    fun play()

    fun pause()

    fun setMuted(muted: Boolean)

    fun release()
}

/** Host player for game previews, so :jarnfe never depends on Media3. */
fun interface JarnGameVideos {
    /** [path] is a file path or a content URI. [crop] fills the view instead of fitting inside it. */
    fun create(context: Context, path: String, crop: Boolean): JarnGameVideo
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnGameVideosProvider {
    val jarnGameVideos: JarnGameVideos
}

internal fun Context.jarnGameVideosOrNull(): JarnGameVideos? =
    (applicationContext as? JarnGameVideosProvider)?.jarnGameVideos

/** Lets a test or preview stand in for the host player. */
internal val LocalJarnGameVideos: ProvidableCompositionLocal<JarnGameVideos?> =
    staticCompositionLocalOf { null }

/** Null when nothing can play video, so the caller keeps showing art. */
@Composable
internal fun rememberJarnGameVideos(): JarnGameVideos? {
    val override = LocalJarnGameVideos.current
    val context = LocalContext.current
    return override ?: remember(context) { context.jarnGameVideosOrNull() }
}
