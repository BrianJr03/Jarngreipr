package jr.brian.frontend.library.play

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.db.PlayHistoryDao
import jr.brian.frontend.library.db.PlayHistoryEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlaySessionRecorder"
private const val PREFS_NAME = "jarn_play_session"
private const val KEY_SESSION = "session_id"
private const val KEY_STARTED = "started_uptime_ms"
private const val KEY_BOOT = "boot_count"
private const val KEY_COVERED = "covered_screen"
private const val NO_BOOT_COUNT = -1

internal interface PlayAnchorStore {
    fun read(): PlayAnchor?
    fun write(anchor: PlayAnchor)
    fun clear()
}

internal interface PlayClock {
    fun uptimeMs(): Long
    fun wallMs(): Long
    fun bootCount(): Int?
}

/** Uses commit: callers are on the worker, and a lost anchor would lose the session. */
private class PrefsPlayAnchorStore(context: Context) : PlayAnchorStore {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override fun read(): PlayAnchor? {
        if (!prefs.contains(KEY_SESSION)) return null
        return PlayAnchor(
            sessionId = prefs.getLong(KEY_SESSION, 0L),
            startedUptimeMs = prefs.getLong(KEY_STARTED, 0L),
            bootCount = prefs.getInt(KEY_BOOT, NO_BOOT_COUNT).takeIf { it != NO_BOOT_COUNT },
            coveredScreen = prefs.getString(KEY_COVERED, null),
        )
    }

    override fun write(anchor: PlayAnchor) {
        prefs.edit()
            .putLong(KEY_SESSION, anchor.sessionId)
            .putLong(KEY_STARTED, anchor.startedUptimeMs)
            .putInt(KEY_BOOT, anchor.bootCount ?: NO_BOOT_COUNT)
            .putString(KEY_COVERED, anchor.coveredScreen)
            .commit()
    }

    override fun clear() {
        prefs.edit().clear().commit()
    }
}

private class AndroidPlayClock(private val context: Context) : PlayClock {
    override fun uptimeMs(): Long = SystemClock.uptimeMillis()

    override fun wallMs(): Long = System.currentTimeMillis()

    override fun bootCount(): Int? =
        runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }
            .getOrNull()
}

/**
 * Adds plays and play time for games launched from Jarn.
 *
 * Android has no "game exited" signal, so a session ends when the Jarn window
 * the game covered comes back. The open `play_history` row is the guard against
 * double counting: closing it and adding to the game's totals is one transaction
 * that can only succeed once, even if the process dies around it.
 *
 * Callers are on the main thread. Events are queued and handled in order on IO.
 */
@Singleton
class PlaySessionRecorder internal constructor(
    private val dao: PlayHistoryDao,
    private val anchors: PlayAnchorStore,
    private val clock: PlayClock,
    private val statsBackup: PlayStatsBackup,
    dispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        dao: PlayHistoryDao,
        statsBackup: PlayStatsBackup,
    ) : this(dao, PrefsPlayAnchorStore(context), AndroidPlayClock(context), statsBackup, Dispatchers.IO)

    private val events = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        CoroutineScope(SupervisorJob() + dispatcher).launch {
            for (event in events) {
                try {
                    event()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update the play session", e)
                }
            }
        }
    }

    fun onGameLaunched(systemId: String, path: String) {
        val at = clock.uptimeMs()
        events.trySend { handleLaunch(systemId, path, at) }
    }

    fun onWindowStopped(screen: String) {
        val at = clock.uptimeMs()
        events.trySend { handleStopped(screen, at) }
    }

    fun onWindowResumed(screen: String) {
        val at = clock.uptimeMs()
        events.trySend { handleResumed(screen, at) }
    }

    internal suspend fun handleLaunch(systemId: String, path: String, atUptimeMs: Long) {
        val boot = clock.bootCount()
        anchors.read()?.let { apply(it, it.onNextLaunch(atUptimeMs, boot)) }
        dao.deleteOpenSessions()
        val gameId = dao.findGameId(systemId, path) ?: return
        val sessionId = dao.insert(PlayHistoryEntity(gameId = gameId, startedAt = clock.wallMs()))
        anchors.write(PlayAnchor(sessionId, atUptimeMs, boot))
    }

    internal fun handleStopped(screen: String, atUptimeMs: Long) {
        val anchor = anchors.read() ?: return
        anchor.onWindowStopped(screen, atUptimeMs, clock.bootCount())?.let(anchors::write)
    }

    internal suspend fun handleResumed(screen: String, atUptimeMs: Long) {
        statsBackup.applyPending()
        val anchor = anchors.read() ?: return
        apply(anchor, anchor.onWindowResumed(screen, atUptimeMs, clock.bootCount()))
    }

    private suspend fun apply(anchor: PlayAnchor, step: PlayStep) {
        when (step) {
            PlayStep.Wait -> return
            PlayStep.Discard -> dao.deleteOpenSession(anchor.sessionId)
            is PlayStep.Credit -> dao.settleSession(anchor.sessionId, step.seconds)
        }
        anchors.clear()
    }
}
