package jr.brian.frontend.library.art

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

internal fun decodeGameSorts(stored: String?): Map<String, String> {
    if (stored.isNullOrBlank()) return emptyMap()
    return try {
        val json = JSONObject(stored)
        json.keys().asSequence().associateWith { json.optString(it) }.cleanedSorts()
    } catch (_: JSONException) {
        emptyMap()
    }
}

private fun Map<String, String>.cleanedSorts(): Map<String, String> =
    filter { (system, sort) -> system.isNotBlank() && sort.isNotBlank() }

/** The sort picked for each system's games, by system id. A system with no entry uses name order. */
@Singleton
class GameSortStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sorts = MutableStateFlow(decodeGameSorts(prefs.getString(KEY_SORTS, null)))
    val state: StateFlow<Map<String, String>> = _sorts.asStateFlow()

    fun set(systemId: String, sort: String) = change { it + (systemId to sort) }

    fun restore(sorts: Map<String, String>) = change { sorts.cleanedSorts() }

    private fun change(transform: (Map<String, String>) -> Map<String, String>) {
        _sorts.update(transform)
        prefs.edit {
            if (_sorts.value.isEmpty()) remove(KEY_SORTS) else putString(KEY_SORTS, JSONObject(_sorts.value).toString())
        }
    }

    private companion object {
        const val PREFS_NAME = "jarnfe_game_sort_prefs"
        const val KEY_SORTS = "system_sorts"
    }
}
