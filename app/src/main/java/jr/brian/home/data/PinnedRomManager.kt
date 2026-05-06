package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinnedRomManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _pinnedRomsByPage = MutableStateFlow<Map<String, List<PinnedRomInfo>>>(emptyMap())

    init {
        loadAll()
    }

    fun getPinnedRoms(pageIndex: Int, tabType: String = TAB_TYPE_APPS): Flow<List<PinnedRomInfo>> =
        _pinnedRomsByPage.map { it[compositeKey(tabType, pageIndex)] ?: emptyList() }

    fun addPinnedRom(pageIndex: Int, romInfo: PinnedRomInfo, tabType: String = TAB_TYPE_APPS) {
        val ck = compositeKey(tabType, pageIndex)
        val current = _pinnedRomsByPage.value.toMutableMap()
        val roms = (current[ck] ?: emptyList()).toMutableList()
        if (roms.none { it.key == romInfo.key }) {
            roms.add(romInfo)
            current[ck] = roms
            _pinnedRomsByPage.value = current
            savePage(pageIndex, roms, tabType)
        }
    }

    fun updatePinnedRom(pageIndex: Int, updatedRom: PinnedRomInfo, tabType: String = TAB_TYPE_APPS) {
        val ck = compositeKey(tabType, pageIndex)
        val current = _pinnedRomsByPage.value.toMutableMap()
        val roms = (current[ck] ?: emptyList()).map {
            if (it.key == updatedRom.key) updatedRom else it
        }
        current[ck] = roms
        _pinnedRomsByPage.value = current
        savePage(pageIndex, roms, tabType)
    }

    fun removePinnedRom(pageIndex: Int, key: String, tabType: String = TAB_TYPE_APPS) {
        val ck = compositeKey(tabType, pageIndex)
        val current = _pinnedRomsByPage.value.toMutableMap()
        val roms = (current[ck] ?: emptyList()).filter { it.key != key }
        current[ck] = roms
        _pinnedRomsByPage.value = current
        savePage(pageIndex, roms, tabType)
    }

    fun removePage(pageIndex: Int, tabType: String = TAB_TYPE_APPS) {
        val current = _pinnedRomsByPage.value.toMutableMap()
        val reindexed = mutableMapOf<String, List<PinnedRomInfo>>()
        current.forEach { (ck, roms) ->
            val (entryTabType, entryPage) = splitCompositeKey(ck) ?: return@forEach
            if (entryTabType != tabType) {
                reindexed[ck] = roms
                return@forEach
            }
            when {
                entryPage < pageIndex -> reindexed[compositeKey(entryTabType, entryPage)] = roms
                entryPage > pageIndex -> reindexed[compositeKey(entryTabType, entryPage - 1)] = roms
            }
        }
        _pinnedRomsByPage.value = reindexed
        prefs.edit {
            remove(pageKey(pageIndex, tabType))
            reindexed.forEach { (ck, roms) ->
                val (t, p) = splitCompositeKey(ck) ?: return@forEach
                putString(pageKey(p, t), json.encodeToString(roms))
            }
        }
    }

    fun reorderPages(oldIndicesInNewOrder: Map<Int, Int>, tabType: String = TAB_TYPE_APPS) {
        val current = _pinnedRomsByPage.value
        val reindexed = mutableMapOf<String, List<PinnedRomInfo>>()
        current.forEach { (ck, roms) ->
            val (entryTabType, _) = splitCompositeKey(ck) ?: return@forEach
            if (entryTabType != tabType) reindexed[ck] = roms
        }
        oldIndicesInNewOrder.forEach { (newIndex, oldIndex) ->
            current[compositeKey(tabType, oldIndex)]?.let {
                reindexed[compositeKey(tabType, newIndex)] = it
            }
        }
        _pinnedRomsByPage.value = reindexed
        prefs.edit {
            current.keys.forEach { ck ->
                val (t, p) = splitCompositeKey(ck) ?: return@forEach
                if (t == tabType) remove(pageKey(p, t))
            }
            reindexed.forEach { (ck, roms) ->
                val (t, p) = splitCompositeKey(ck) ?: return@forEach
                if (t == tabType) putString(pageKey(p, t), json.encodeToString(roms))
            }
        }
    }

    private fun loadAll() {
        val all = mutableMapOf<String, List<PinnedRomInfo>>()
        prefs.all.keys.forEach { key ->
            val tabType: String
            val pageIndex: Int
            when {
                key.startsWith(PAGE_KEY_PREFIX) -> {
                    pageIndex = key.removePrefix(PAGE_KEY_PREFIX).toIntOrNull() ?: return@forEach
                    tabType = TAB_TYPE_APPS
                }
                else -> {
                    val match = TAB_PAGE_KEY_REGEX.matchEntire(key) ?: return@forEach
                    tabType = match.groupValues[1]
                    pageIndex = match.groupValues[2].toIntOrNull() ?: return@forEach
                }
            }
            val stored = prefs.getString(key, null) ?: return@forEach
            runCatching {
                all[compositeKey(tabType, pageIndex)] = json.decodeFromString<List<PinnedRomInfo>>(stored)
            }
        }
        _pinnedRomsByPage.value = all
    }

    private fun savePage(pageIndex: Int, roms: List<PinnedRomInfo>, tabType: String) {
        prefs.edit { putString(pageKey(pageIndex, tabType), json.encodeToString(roms)) }
    }

    private fun pageKey(pageIndex: Int, tabType: String) =
        if (tabType == TAB_TYPE_APPS) "$PAGE_KEY_PREFIX$pageIndex"
        else "${PAGE_KEY_BASE}${tabType}_page_$pageIndex"

    private fun compositeKey(tabType: String, pageIndex: Int) = "${tabType}_$pageIndex"

    private fun splitCompositeKey(ck: String): Pair<String, Int>? {
        val idx = ck.lastIndexOf('_')
        if (idx < 0) return null
        val pageIndex = ck.substring(idx + 1).toIntOrNull() ?: return null
        return ck.substring(0, idx) to pageIndex
    }

    companion object {
        const val TAB_TYPE_APPS = "apps"
        const val TAB_TYPE_WIDGETS = "widgets"

        private const val PREFS_NAME = "pinned_roms_prefs"
        private const val PAGE_KEY_BASE = "pinned_roms_"
        private const val PAGE_KEY_PREFIX = "pinned_roms_page_"

        private val TAB_PAGE_KEY_REGEX = Regex("^pinned_roms_(.+)_page_(\\d+)$")
    }
}
