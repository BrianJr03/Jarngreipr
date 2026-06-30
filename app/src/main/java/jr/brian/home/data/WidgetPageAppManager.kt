package jr.brian.home.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetPageAppDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_page_app_preferences"
)

class WidgetPageAppManager(private val context: Context) {
    companion object {
        private fun visibleAppsKey(pageIndex: Int) =
            stringPreferencesKey("visible_apps_page_$pageIndex")

        private fun sectionOrderKey(pageIndex: Int) =
            booleanPreferencesKey("apps_first_page_$pageIndex")
    }

    fun getVisibleApps(pageIndex: Int): Flow<Set<String>> {
        return context.widgetPageAppDataStore.data.map { preferences ->
            val appsString = preferences[visibleAppsKey(pageIndex)] ?: ""
            if (appsString.isEmpty()) {
                emptySet()
            } else {
                appsString.split(",").toSet()
            }
        }
    }

    fun getAppsFirstOrder(pageIndex: Int): Flow<Boolean> {
        return context.widgetPageAppDataStore.data.map { preferences ->
            preferences[sectionOrderKey(pageIndex)] ?: false
        }
    }

    suspend fun addVisibleApp(pageIndex: Int, packageName: String) {
        context.widgetPageAppDataStore.edit { preferences ->
            val current = preferences[visibleAppsKey(pageIndex)] ?: ""
            val currentSet = if (current.isEmpty()) emptySet() else current.split(",").toSet()
            val updated = currentSet + packageName
            preferences[visibleAppsKey(pageIndex)] = updated.joinToString(",")
        }
    }

    suspend fun removeVisibleApp(pageIndex: Int, packageName: String) {
        context.widgetPageAppDataStore.edit { preferences ->
            val current = preferences[visibleAppsKey(pageIndex)] ?: ""
            val currentSet = if (current.isEmpty()) emptySet() else current.split(",").toSet()
            val updated = currentSet - packageName
            preferences[visibleAppsKey(pageIndex)] = updated.joinToString(",")
        }
    }

    suspend fun toggleSectionOrder(pageIndex: Int) {
        context.widgetPageAppDataStore.edit { preferences ->
            val current = preferences[sectionOrderKey(pageIndex)] ?: false
            preferences[sectionOrderKey(pageIndex)] = !current
        }
    }

    suspend fun clearPageData(pageIndex: Int) {
        context.widgetPageAppDataStore.edit { preferences ->
            preferences.remove(visibleAppsKey(pageIndex))
            preferences.remove(sectionOrderKey(pageIndex))
        }
    }

    suspend fun reindexPages(deletedPageIndex: Int, totalPagesAfterDeletion: Int) {
        context.widgetPageAppDataStore.edit { preferences ->
            val tempData = mutableMapOf<Int, Pair<String, Boolean>>()

            for (i in 0 until (totalPagesAfterDeletion + 1)) {
                val visibleApps = preferences[visibleAppsKey(i)] ?: ""
                val appsFirst = preferences[sectionOrderKey(i)] ?: false
                if (visibleApps.isNotEmpty() || appsFirst) {
                    tempData[i] = Pair(visibleApps, appsFirst)
                }
            }

            for (i in 0 until (totalPagesAfterDeletion + 1)) {
                preferences.remove(visibleAppsKey(i))
                preferences.remove(sectionOrderKey(i))
            }

            tempData.forEach { (oldIndex, data) ->
            val newIndex = if (oldIndex > deletedPageIndex) oldIndex - 1 else oldIndex
            if (oldIndex != deletedPageIndex) {
                preferences[visibleAppsKey(newIndex)] = data.first
                preferences[sectionOrderKey(newIndex)] = data.second
            }
        }
        }
    }

    suspend fun reorderPages(oldIndicesInNewOrder: Map<Int, Int>) {
        context.widgetPageAppDataStore.edit { preferences ->
            val oldVisible = mutableMapOf<Int, String>()
            val oldAppsFirst = mutableMapOf<Int, Boolean>()
            preferences.asMap().forEach { (key, value) ->
                extractIndex(key.name, VISIBLE_APPS_PREFIX)?.let { idx ->
                    (value as? String)?.let { oldVisible[idx] = it }
                }
                extractIndex(key.name, APPS_FIRST_PREFIX)?.let { idx ->
                    (value as? Boolean)?.let { oldAppsFirst[idx] = it }
                }
            }
            (oldVisible.keys + oldAppsFirst.keys).forEach { idx ->
                preferences.remove(visibleAppsKey(idx))
                preferences.remove(sectionOrderKey(idx))
            }
            oldIndicesInNewOrder.forEach { (newIndex, oldIndex) ->
                oldVisible[oldIndex]?.let { preferences[visibleAppsKey(newIndex)] = it }
                oldAppsFirst[oldIndex]?.let { preferences[sectionOrderKey(newIndex)] = it }
            }
        }
    }

    suspend fun removePage(pageIndex: Int) {
        context.widgetPageAppDataStore.edit { preferences ->
            val oldVisible = mutableMapOf<Int, String>()
            val oldAppsFirst = mutableMapOf<Int, Boolean>()
            preferences.asMap().forEach { (key, value) ->
                extractIndex(key.name, VISIBLE_APPS_PREFIX)?.let { idx ->
                    (value as? String)?.let { oldVisible[idx] = it }
                }
                extractIndex(key.name, APPS_FIRST_PREFIX)?.let { idx ->
                    (value as? Boolean)?.let { oldAppsFirst[idx] = it }
                }
            }
            (oldVisible.keys + oldAppsFirst.keys).forEach { idx ->
                preferences.remove(visibleAppsKey(idx))
                preferences.remove(sectionOrderKey(idx))
            }
            fun newIndexFor(oldIdx: Int): Int? = when {
                oldIdx < pageIndex -> oldIdx
                oldIdx > pageIndex -> oldIdx - 1
                else -> null
            }
            oldVisible.forEach { (oldIdx, v) ->
                newIndexFor(oldIdx)?.let { preferences[visibleAppsKey(it)] = v }
            }
            oldAppsFirst.forEach { (oldIdx, v) ->
                newIndexFor(oldIdx)?.let { preferences[sectionOrderKey(it)] = v }
            }
        }
    }

    private fun extractIndex(keyName: String, prefix: String): Int? {
        if (!keyName.startsWith(prefix)) return null
        return keyName.removePrefix(prefix).toIntOrNull()
    }
}

private const val VISIBLE_APPS_PREFIX = "visible_apps_page_"
private const val APPS_FIRST_PREFIX = "apps_first_page_"
