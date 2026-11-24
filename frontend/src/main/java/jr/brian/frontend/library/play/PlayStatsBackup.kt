package jr.brian.frontend.library.play

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.db.PlayHistoryDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlayStatsBackup"
private const val PREFS_NAME = "jarn_play_stats_pending"
private const val KEY_PENDING = "pending"

/** One game's totals in a backup, keyed so it can match after a rescan on another device. */
@Serializable
data class BackedUpPlayStat(
    val systemId: String,
    val relativePath: String,
    val playCount: Int,
    val playTimeMinutes: Int,
)

internal fun interface PendingPlayStats {
    fun update(transform: (List<BackedUpPlayStat>) -> List<BackedUpPlayStat>)
}

/** Keeps the highest totals per game and drops entries that carry nothing. */
internal fun mergePlayStats(stats: List<BackedUpPlayStat>): List<BackedUpPlayStat> =
    stats.asSequence()
        .filter { it.systemId.isNotBlank() && it.relativePath.isNotBlank() }
        .map { it.copy(playCount = it.playCount.coerceAtLeast(0), playTimeMinutes = it.playTimeMinutes.coerceAtLeast(0)) }
        .filter { it.playCount > 0 || it.playTimeMinutes > 0 }
        .groupBy { it.systemId to it.relativePath }
        .values
        .map { same ->
            same.first().copy(
                playCount = same.maxOf { it.playCount },
                playTimeMinutes = same.maxOf { it.playTimeMinutes },
            )
        }

/**
 * Saves and restores play totals. A restore only ever raises a total. Games not
 * in the library yet, as on a fresh install, wait and are applied once scanned.
 */
@Singleton
class PlayStatsBackup internal constructor(
    private val dao: PlayHistoryDao,
    private val readPending: () -> List<BackedUpPlayStat>,
    private val writePending: (List<BackedUpPlayStat>) -> Unit,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        dao: PlayHistoryDao,
    ) : this(dao, PendingPrefs(context)::read, PendingPrefs(context)::write)

    private val lock = Mutex()

    /** Null when totals could not be read, so a restore leaves the device's alone. */
    suspend fun export(): List<BackedUpPlayStat>? = guarded(fallback = null) {
        lock.withLock {
            val pending = withContext(Dispatchers.IO) { readPending() }
            mergePlayStats(dao.playStats() + pending)
        }
    }

    /** A failure never fails the wider import. */
    suspend fun restore(stats: List<BackedUpPlayStat>) {
        guarded(fallback = Unit) {
            lock.withLock {
                withContext(Dispatchers.IO) {
                    applyAndKeepUnmatched(mergePlayStats(readPending() + stats))
                }
            }
        }
    }

    /** Guarded so a failure here can never block a play session from being recorded. */
    internal suspend fun applyPending() {
        guarded(fallback = Unit) {
            lock.withLock {
                withContext(Dispatchers.IO) {
                    val pending = readPending()
                    if (pending.isNotEmpty()) applyAndKeepUnmatched(pending)
                }
            }
        }
    }

    private suspend fun applyAndKeepUnmatched(stats: List<BackedUpPlayStat>) {
        val known = dao.gameKeys().mapTo(HashSet()) { it.systemId to it.relativePath }
        val (matched, waiting) = stats.partition { (it.systemId to it.relativePath) in known }
        if (matched.isNotEmpty()) dao.raiseStats(matched)
        writePending(waiting)
    }

    private suspend fun <T> guarded(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Play totals backup step failed", e)
        fallback
    }
}

private class PendingPrefs(context: Context) {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val serializer = ListSerializer(BackedUpPlayStat.serializer())

    fun read(): List<BackedUpPlayStat> {
        val text = prefs.getString(KEY_PENDING, null) ?: return emptyList()
        return runCatching { Json.decodeFromString(serializer, text) }.getOrDefault(emptyList())
    }

    fun write(stats: List<BackedUpPlayStat>) {
        val editor = prefs.edit()
        if (stats.isEmpty()) {
            editor.remove(KEY_PENDING)
        } else {
            editor.putString(KEY_PENDING, Json.encodeToString(serializer, stats))
        }
        editor.commit()
    }
}
