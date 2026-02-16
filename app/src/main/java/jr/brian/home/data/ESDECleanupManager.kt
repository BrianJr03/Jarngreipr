package jr.brian.home.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.esdeCleanupDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "esde_cleanup_preferences"
)

@Singleton
class ESDECleanupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.esdeCleanupDataStore

    private object PreferencesKeys {
        val FOLDER_PATHS = stringSetPreferencesKey("folder_paths")
    }

    val folderPaths: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FOLDER_PATHS] ?: emptySet()
    }

    suspend fun addFolderPath(path: String) {
        dataStore.edit { preferences ->
            val currentPaths =
                preferences[PreferencesKeys.FOLDER_PATHS]?.toMutableSet() ?: mutableSetOf()
            currentPaths.add(path)
            preferences[PreferencesKeys.FOLDER_PATHS] = currentPaths
        }
    }

    suspend fun removeFolderPath(path: String) {
        dataStore.edit { preferences ->
            val currentPaths =
                preferences[PreferencesKeys.FOLDER_PATHS]?.toMutableSet() ?: mutableSetOf()
            currentPaths.remove(path)
            preferences[PreferencesKeys.FOLDER_PATHS] = currentPaths
        }
    }
}
