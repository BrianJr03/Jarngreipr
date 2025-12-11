package jr.brian.home.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.customIconDataStore by preferencesDataStore(name = "custom_icons")

@Singleton
class CustomIconManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun getKey(packageName: String) = stringPreferencesKey("icon_$packageName")

    fun getCustomIconUri(packageName: String): Flow<String?> {
        return context.customIconDataStore.data.map { preferences ->
            preferences[getKey(packageName)]
        }
    }

    suspend fun setCustomIcon(packageName: String, uri: Uri) {
        context.customIconDataStore.edit { preferences ->
            preferences[getKey(packageName)] = uri.toString()
        }
    }

    suspend fun clearCustomIcon(packageName: String) {
        context.customIconDataStore.edit { preferences ->
            preferences.remove(getKey(packageName))
        }
    }

    fun hasCustomIcon(packageName: String): Flow<Boolean> {
        return getCustomIconUri(packageName).map { it != null }
    }
}
