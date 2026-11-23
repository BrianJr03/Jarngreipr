package jr.brian.jarnfe.launch

import android.content.Context
import jr.brian.frontend.library.play.PlaySessionRecorder

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnPlaySessionsProvider {
    val jarnPlaySessions: PlaySessionRecorder
}

internal fun Context.jarnPlaySessionsOrNull(): PlaySessionRecorder? =
    (applicationContext as? JarnPlaySessionsProvider)?.jarnPlaySessions

internal fun Context.recordGameLaunched(ref: JarnGameRef) {
    jarnPlaySessionsOrNull()?.onGameLaunched(ref.systemName, ref.path)
}
