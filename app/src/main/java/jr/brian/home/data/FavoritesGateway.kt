package jr.brian.home.data

import android.util.Log
import jr.brian.frontend.library.db.GameDao
import jr.brian.home.esde.model.GameInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges legacy's [GameInfo] model to the Room-backed favorite flag.
 * Resolves the matching [jr.brian.frontend.library.db.GameEntity] by
 * `romAbsolutePath` (falling back to `path`), writes the new value via
 * [GameDao.setFavorite], and returns the resolved state.
 *
 * The write persists through [LibraryGameInfoAdapter]'s combine flow —
 * any UI collecting from `store.allGames` sees the updated row on the
 * next emission.
 *
 * Idempotent across reimports: [RomIndexImporter] OR-merges Room's
 * existing `isFavorite` against the legacy value, so a legacy scan
 * can't clear a favorite the user set here.
 */
@Singleton
class FavoritesGateway @Inject constructor(
    private val gameDao: GameDao,
) {
    /**
     * Flip the favorite flag for [game]. Returns the resolved new value,
     * or `null` if no matching row exists in Room (which usually means
     * the migration hasn't finished — the UI should fall through and
     * try again later).
     */
    suspend fun toggleFavorite(game: GameInfo): Boolean? {
        val absolute = game.romAbsolutePath ?: game.path
        val entity = gameDao.findByAbsolutePath(absolute) ?: run {
            Log.w(TAG, "No GameEntity for path=$absolute; favorite toggle ignored")
            return null
        }
        val next = !entity.isFavorite
        gameDao.setFavorite(entity.id, next)
        return next
    }

    private companion object {
        const val TAG = "FavoritesGateway"
    }
}
