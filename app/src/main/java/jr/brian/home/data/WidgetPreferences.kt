package jr.brian.home.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import jr.brian.home.model.widget.WidgetConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_preferences")

class WidgetPreferences(private val context: Context) {
    companion object {
        private val WIDGET_CONFIGS_KEY = stringPreferencesKey("widget_configs")
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    val widgetConfigs: Flow<List<WidgetConfig>> = context.dataStore.data
        .map { preferences ->
            val configsJson = preferences[WIDGET_CONFIGS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<WidgetConfig>>(configsJson)
            } catch (_: Exception) {
                emptyList()
            }
        }

    @Suppress("unused")
    suspend fun saveWidgetConfigs(configs: List<WidgetConfig>) {
        context.dataStore.edit { preferences ->
            preferences[WIDGET_CONFIGS_KEY] = json.encodeToString(configs)
        }
    }

    suspend fun addWidgetConfig(config: WidgetConfig) {
        context.dataStore.edit { preferences ->
            val currentConfigsJson = preferences[WIDGET_CONFIGS_KEY] ?: "[]"
            val currentConfigs = try {
                json.decodeFromString<List<WidgetConfig>>(currentConfigsJson)
            } catch (_: Exception) {
                emptyList()
            }

            val filteredConfigs = currentConfigs.filter { it.widgetId != config.widgetId }
            val updatedConfigs = filteredConfigs + config
            preferences[WIDGET_CONFIGS_KEY] = json.encodeToString(updatedConfigs)
        }
    }

    suspend fun removeWidgetConfig(widgetId: Int) {
        context.dataStore.edit { preferences ->
            val currentConfigsJson = preferences[WIDGET_CONFIGS_KEY] ?: "[]"
            val currentConfigs = try {
                json.decodeFromString<List<WidgetConfig>>(currentConfigsJson)
            } catch (_: Exception) {
                emptyList()
            }

            val updatedConfigs = currentConfigs.filter { it.widgetId != widgetId }
            preferences[WIDGET_CONFIGS_KEY] = json.encodeToString(updatedConfigs)
        }
    }

    @Suppress("unused")
    suspend fun clearAllWidgetConfigs() {
        context.dataStore.edit { preferences ->
            preferences.remove(WIDGET_CONFIGS_KEY)
        }
    }
}
