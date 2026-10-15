package jr.brian.frontend.library.systems

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lookup over the shipped `assets/systems/[id].json` catalog. Replaces the
 * ad-hoc split between `EmulatorRegistry` (extensions), `SystemIdMap`
 * (ScreenScraper), and `ESDEMediaConstants.SYSTEM_MEDIA_ALIASES` (folder
 * aliases) with one indexed source for every system Jarngreipr knows
 * about.
 *
 * The scanner (Phase 2c) consults this catalog before deciding to walk a
 * directory; a directory named after an id OR any of its [SystemDef.folderAliases]
 * belongs to the matching [SystemDef]. Systems absent from the catalog
 * fall through to whatever legacy mechanism `:app` still uses — adding
 * a new system is a one-file PR under `frontend/src/main/assets/systems/`.
 */
interface SystemCatalog {
    /** All systems in display-name order. */
    fun all(): List<SystemDef>

    /** O(1) lookup by canonical id. Case-insensitive. */
    fun findById(id: String): SystemDef?

    /**
     * Lookup by a folder name encountered during a scan. Checks each
     * system's [SystemDef.id] first (the implicit alias) then every
     * entry in [SystemDef.folderAliases]. First match wins, so the
     * authored JSON files must keep aliases non-overlapping across
     * systems — the per-file unit test is where that invariant is
     * actually enforced.
     */
    fun findByFolderAlias(folderName: String): SystemDef?

    /** The ROM-extension set for one id, empty if the system is unknown. */
    fun extensionsFor(id: String): Set<String>
}

/**
 * [SystemCatalog] backed by `/android_asset/systems/[id].json`. Loads lazily
 * on first access, caches the parsed map for the life of the process.
 *
 * The asset directory is traversed once; individual files that fail to
 * parse are logged and skipped so one broken JSON doesn't block the
 * entire catalog from loading. The scanner's "unknown system" path is
 * the correct fallback for any system whose JSON didn't decode.
 */
@Singleton
class AssetSystemCatalog @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SystemCatalog {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var cached: Map<String, SystemDef>? = null

    private fun load(): Map<String, SystemDef> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val built = buildCatalog()
            cached = built
            return built
        }
    }

    private fun buildCatalog(): Map<String, SystemDef> {
        val assets = context.assets
        val files = try {
            assets.list(ASSETS_DIR)?.filter { it.endsWith(".json") }.orEmpty()
        } catch (e: IOException) {
            Log.w(TAG, "No systems asset directory present; catalog is empty", e)
            return emptyMap()
        }

        val out = HashMap<String, SystemDef>(files.size)
        for (fileName in files) {
            val def = decodeOrNull("$ASSETS_DIR/$fileName") ?: continue
            val expectedId = fileName.removeSuffix(".json")
            if (!def.id.equals(expectedId, ignoreCase = true)) {
                Log.w(
                    TAG,
                    "Catalog file $fileName declares id='${def.id}' which does not match the " +
                        "filename stem '$expectedId' — skipping; rename the file or fix the id",
                )
                continue
            }
            out[def.id.lowercase()] = def
        }
        return out
    }

    private fun decodeOrNull(assetPath: String): SystemDef? = try {
        context.assets.open(assetPath).use { stream ->
            json.decodeFromString(SystemDef.serializer(), stream.bufferedReader().readText())
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decode system catalog file $assetPath", e)
        null
    }

    override fun all(): List<SystemDef> =
        load().values.sortedBy { it.displayName.lowercase() }

    override fun findById(id: String): SystemDef? = load()[id.lowercase()]

    override fun findByFolderAlias(folderName: String): SystemDef? {
        val normalized = folderName.lowercase()
        val catalog = load()
        catalog[normalized]?.let { return it }
        return catalog.values.firstOrNull { def ->
            def.folderAliases.any { it.equals(normalized, ignoreCase = true) }
        }
    }

    override fun extensionsFor(id: String): Set<String> =
        findById(id)?.extensions.orEmpty()

    private companion object {
        const val TAG = "AssetSystemCatalog"
        const val ASSETS_DIR = "systems"
    }
}

/**
 * Provides the shipped [AssetSystemCatalog] as the default
 * [SystemCatalog] implementation. Lives in its own module so a test or
 * alternate build can swap in a stub without touching the library DB
 * wiring.
 */
@Module
@InstallIn(SingletonComponent::class)
object SystemCatalogModule {
    @Provides
    @Singleton
    fun provideSystemCatalog(impl: AssetSystemCatalog): SystemCatalog = impl
}
