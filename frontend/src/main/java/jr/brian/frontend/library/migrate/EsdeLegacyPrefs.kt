package jr.brian.frontend.library.migrate

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only accessor for the legacy `esde_prefs` SharedPreferences
 * blob that `:app`'s ESDEPreferencesManager writes.
 *
 * The migration (Phase 2d) reads these to populate the new Room
 * schema; after that, the engine owns its own storage. This class
 * exists so `:frontend` doesn't have to import `:app`'s
 * ESDEPreferencesManager class (would be a layer violation per plan
 * §3.1), while still being able to migrate users' data.
 *
 * Keys are hard-coded as string literals — a Kotlin const dependency
 * on `:app`'s ESDEPreferencesConstants would be another layer
 * violation, and the two strings-have-to-match contract is covered
 * by the unit test that cross-checks against the live constants.
 *
 * All reads are forgiving: a missing key or malformed JSON produces
 * empty results rather than throws, so one corrupted field never
 * blocks the whole migration.
 */
@Singleton
class EsdeLegacyPrefs @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** ROMs root paths the user added via file-path (non-SAF). */
    fun romsPaths(): List<String> {
        val raw = prefs.getString("roms_paths", null) ?: return emptyList()
        return decodeStringArray(raw)
    }

    /**
     * Per-system SAF tree URIs. Key is the system name; value is
     * the persisted `content://` tree URI.
     */
    fun safTreeUris(): Map<String, String> {
        val raw = prefs.getString("saf_tree_uris", null) ?: return emptyMap()
        return decodeStringMap(raw)
    }

    /**
     * User-declared "this folder on disk is system X" mappings. One
     * entry per mapping. Entries are `kotlinx.serialization`-encoded
     * objects with `systemName`, `treeUri`, `displayPath`; we only
     * need the first two so we extract them with raw JSONObject
     * parsing rather than depending on the SystemFolderMapping class
     * from `:app`.
     */
    fun systemFolderMappings(): List<Mapping> {
        val raw = prefs.getString("system_folder_mappings", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val sys = obj.optString("systemName").ifBlank { continue }
                    val uri = obj.optString("treeUri").ifBlank { continue }
                    val display = obj.optString("displayPath", uri)
                    add(Mapping(systemName = sys, treeUri = uri, displayPath = display))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode system_folder_mappings; treating as empty", e)
            emptyList()
        }
    }

    /** User-hidden system names (lowercased by legacy writer). */
    fun hiddenSystems(): Set<String> {
        val raw = prefs.getString("hidden_systems", null) ?: return emptySet()
        return decodeStringArray(raw).toSet()
    }

    /** User-hidden game keys (format: `systemName|basename`). */
    fun hiddenGames(): Set<String> {
        val raw = prefs.getString("hidden_games", null) ?: return emptySet()
        return decodeStringArray(raw).toSet()
    }

    /** Per-game emulator overrides, keyed by `gameKey` (`systemName|basename`). */
    fun gameEmulatorMap(): Map<String, String> =
        decodeStringMap(prefs.getString("game_emulator_map", null) ?: return emptyMap())

    /** Per-game RetroArch core overrides, keyed by `gameKey`. */
    fun gameCoreMap(): Map<String, String> =
        decodeStringMap(prefs.getString("game_core_map", null) ?: return emptyMap())

    /** Per-game companion-app packages, keyed by `gameKey`. */
    fun gameCompanionMap(): Map<String, String> =
        decodeStringMap(prefs.getString("game_companion_map", null) ?: return emptyMap())

    /** Default emulator per system name. */
    fun systemAppMap(): Map<String, String?> {
        val raw = prefs.getString("system_app_map", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    if (obj.isNull(key)) put(key, null) else put(key, obj.optString(key))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode system_app_map; treating as empty", e)
            emptyMap()
        }
    }

    /** Persisted system display order. */
    fun systemOrder(): List<String> {
        val raw = prefs.getString("system_order", null) ?: return emptyList()
        return decodeStringArray(raw)
    }

    private fun decodeStringArray(raw: String): List<String> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "")
                if (s.isNotEmpty()) add(s)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decode string array from $raw", e)
        emptyList()
    }

    private fun decodeStringMap(raw: String): Map<String, String> = try {
        val obj = JSONObject(raw)
        buildMap {
            obj.keys().forEach { key ->
                val value = obj.optString(key)
                if (value.isNotEmpty()) put(key, value)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decode string map from $raw", e)
        emptyMap()
    }

    data class Mapping(
        val systemName: String,
        val treeUri: String,
        val displayPath: String,
    )

    private companion object {
        const val TAG = "EsdeLegacyPrefs"
        const val PREFS_NAME = "esde_prefs"
    }
}
