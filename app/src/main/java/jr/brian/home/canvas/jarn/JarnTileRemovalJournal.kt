package jr.brian.home.canvas.jarn

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ApplicationScope
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val PREFS_NAME = "jarn_tile_removals"
private const val KEY_PENDING = "pending"

@Serializable
internal data class PendingTileRemoval(val pageIndex: Int, val item: CanvasItem)

internal fun isFolderReferenced(layouts: Collection<CanvasLayout>, folderId: String): Boolean =
    layouts.any { layout -> layout.items.any { it is CanvasItem.FolderItem && it.folderId == folderId } }

/**
 * Tiles taken off the layout whose folder, widget id, photo or pin may still need releasing.
 * Written before the tile goes, so a process death in between is finished on the next start.
 * Device-local and short-lived, so it is never part of a backup.
 */
@Singleton
class JarnTileRemovalJournal @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun hasPending(): Boolean = !prefs.getString(KEY_PENDING, null).isNullOrEmpty()

    internal fun pending(): List<PendingTileRemoval> {
        val stored = prefs.getString(KEY_PENDING, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<PendingTileRemoval>>(stored) }.getOrDefault(emptyList())
    }

    internal suspend fun record(removals: List<PendingTileRemoval>) = write(pending() + removals)

    internal suspend fun clear(removal: PendingTileRemoval) = write(pending() - removal)

    private suspend fun write(removals: List<PendingTileRemoval>) = withContext(Dispatchers.IO) {
        val editor = prefs.edit()
        if (removals.isEmpty()) {
            editor.remove(KEY_PENDING)
        } else {
            editor.putString(KEY_PENDING, json.encodeToString(removals))
        }
        editor.commit()
    }
}

/** Builds Jarn FE's tile bridge only when a removal was cut short, so Classic stays untouched. */
@Singleton
class JarnTileRemovalRecovery @Inject constructor(
    private val journal: JarnTileRemovalJournal,
    private val tiles: Provider<AppJarnCanvasTiles>,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            if (journal.hasPending()) runCatching { tiles.get().finishPendingRemovals() }
        }
    }
}
