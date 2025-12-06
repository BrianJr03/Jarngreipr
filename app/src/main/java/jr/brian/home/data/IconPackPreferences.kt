package jr.brian.home.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.iconPackDataStore by preferencesDataStore(name = "icon_pack_preferences")

/**
 * Manages icon pack preferences using DataStore
 */
@Singleton
class IconPackPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.iconPackDataStore

    companion object {
        private val SELECTED_ICON_PACK_KEY = stringPreferencesKey("selected_icon_pack")
    }

    /**
     * Get the selected icon pack package name as a Flow
     */
    val selectedIconPackFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SELECTED_ICON_PACK_KEY]
    }

    /**
     * Get the currently selected icon pack package name (blocking)
     */
    fun getSelectedIconPack(): String? = runBlocking {
        selectedIconPackFlow.first()
    }

    /**
     * Set the selected icon pack package name
     * Pass null to clear the selection (use default icons)
     */
    suspend fun setSelectedIconPack(packageName: String?) {
        dataStore.edit { preferences ->
            if (packageName != null) {
                preferences[SELECTED_ICON_PACK_KEY] = packageName
            } else {
                preferences.remove(SELECTED_ICON_PACK_KEY)
            }
        }
    }
}
