package jr.brian.frontend.library.favorites

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.LibraryState
import jr.brian.frontend.library.db.GameDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FavoritesBackup"
private const val PREFS_NAME = "jarn_favorites_pending"
private const val KEY_PENDING = "pending"

/** One favorite in a backup, keyed so it matches after a rescan on another device. */
@Serializable
data class BackedUpFavorite(val systemId: String, val relativePath: String)

/** Drops blank keys and repeats, in a stable order so backups compare equal. */
internal fun cleanFavorites(favorites: List<BackedUpFavorite>): List<BackedUpFavorite> =
    favorites.asSequence()
        .filter { it.systemId.isNotBlank() && it.relativePath.isNotBlank() }
        .distinct()
        .sortedWith(compareBy({ it.systemId }, { it.relativePath }))
        .toList()

/** Emits once at the start and again each time a scan ends, so pending favorites can be tried. */
fun Flow<LibraryState>.scanEnds(): Flow<Unit> =
    map { it is LibraryState.Scanning }
        .distinctUntilChanged()
        .filter { scanning -> !scanning }
        .map { }

/**
 * Saves and restores favorites. A restore only ever adds one. Games not in the library yet, as on
 * a fresh install, wait on this device and are applied once a scan lists them.
 */
@Singleton
class FavoritesBackup internal constructor(
    private val dao: GameDao,
    private val readPending: () -> List<BackedUpFavorite>,
    private val writePending: (List<BackedUpFavorite>) -> Unit,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        dao: GameDao,
    ) : this(dao, PendingFavorites(context)::read, PendingFavorites(context)::write)

    private val lock = Mutex()

    /** Waiting favorites are included, so a backup taken before the scan still has them. */
    suspend fun export(): List<BackedUpFavorite>? = guarded(fallback = null) {
        lock.withLock {
            withContext(Dispatchers.IO) {
                val saved = dao.favoriteKeys().map { BackedUpFavorite(it.systemId, it.relativePath) }
                cleanFavorites(saved + readPending())
            }
        }
    }

    /** A failure never fails the wider import. */
    suspend fun restore(favorites: List<BackedUpFavorite>) {
        guarded(fallback = Unit) {
            lock.withLock {
                withContext(Dispatchers.IO) { applyAndKeepUnmatched(cleanFavorites(readPending() + favorites)) }
            }
        }
    }

    /** Called when a scan ends. Does nothing when no favorite is waiting. */
    suspend fun applyPending() {
        guarded(fallback = Unit) {
            lock.withLock {
                withContext(Dispatchers.IO) {
                    val pending = readPending()
                    if (pending.isNotEmpty()) applyAndKeepUnmatched(pending)
                }
            }
        }
    }

    private suspend fun applyAndKeepUnmatched(favorites: List<BackedUpFavorite>) {
        val waiting = favorites.filter { dao.favoriteByKey(it.systemId, it.relativePath) == 0 }
        writePending(waiting)
    }

    private suspend fun <T> guarded(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Favorites backup step failed", e)
        fallback
    }
}

private class PendingFavorites(context: Context) {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val serializer = ListSerializer(BackedUpFavorite.serializer())

    fun read(): List<BackedUpFavorite> {
        val text = prefs.getString(KEY_PENDING, null) ?: return emptyList()
        return runCatching { Json.decodeFromString(serializer, text) }.getOrDefault(emptyList())
    }

    fun write(favorites: List<BackedUpFavorite>) {
        val editor = prefs.edit()
        if (favorites.isEmpty()) {
            editor.remove(KEY_PENDING)
        } else {
            editor.putString(KEY_PENDING, Json.encodeToString(serializer, favorites))
        }
        editor.commit()
    }
}
