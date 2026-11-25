package jr.brian.jarnfe.launch

import android.content.Context
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.frontend.library.play.PlaySessionRecorder
import jr.brian.jarnfe.deck.JarnNowPlaying
import jr.brian.jarnfe.deck.gameHostOn
import jr.brian.jarnfe.deck.toNowPlayingGame
import jr.brian.jarnfe.shell.JarnHomeEntry
import jr.brian.jarnfe.shell.resolveDeckDisplayId

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnPlaySessionsProvider {
    val jarnPlaySessions: PlaySessionRecorder
}

internal fun Context.jarnPlaySessionsOrNull(): PlaySessionRecorder? =
    (applicationContext as? JarnPlaySessionsProvider)?.jarnPlaySessions

/** [displayId] is where the launcher sent the game, when it knows. */
internal fun Context.recordGameLaunched(ref: JarnGameRef, displayId: Int? = null) {
    JarnHomeEntry.onGameLaunched()
    val expected = gameHostOn(displayId, PRIMARY_DISPLAY_ID, resolveDeckDisplayId(this))
    JarnNowPlaying.onGameLaunched(ref.toNowPlayingGame(), expected)
    jarnPlaySessionsOrNull()?.onGameLaunched(ref.systemName, ref.path)
}
