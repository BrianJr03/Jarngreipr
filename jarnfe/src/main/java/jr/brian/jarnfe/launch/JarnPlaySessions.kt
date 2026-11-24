package jr.brian.jarnfe.launch

import android.content.Context
import jr.brian.frontend.library.play.PlaySessionRecorder
import jr.brian.jarnfe.shell.JarnHomeEntry

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnPlaySessionsProvider {
    val jarnPlaySessions: PlaySessionRecorder
}

internal fun Context.jarnPlaySessionsOrNull(): PlaySessionRecorder? =
    (applicationContext as? JarnPlaySessionsProvider)?.jarnPlaySessions

internal fun Context.recordGameLaunched(ref: JarnGameRef) {
    JarnHomeEntry.onGameLaunched()
    jarnPlaySessionsOrNull()?.onGameLaunched(ref.systemName, ref.path)
}
