package jr.brian.home.viewmodels

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.R
import jr.brian.home.data.ThemeShareRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_NEARBY_WALLPAPERS = "nearby_wallpapers"
private const val RECORD_SEP = "|||"
private const val FIELD_SEP = ":::"

@HiltViewModel
class ThemeShareViewModel @Inject constructor(
    private val repository: ThemeShareRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val receivedThemes = repository.receivedThemes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    private val _receivedWallpapers = MutableStateFlow(loadReceivedWallpapers())
    val receivedWallpapers: StateFlow<Map<String, String>> = _receivedWallpapers.asStateFlow()

    fun deleteSharedTheme(sender: String, themeId: String) {
        repository.deleteSharedTheme(sender, themeId)
    }

    fun deleteReceivedWallpaper(key: String) {
        _receivedWallpapers.update { currentMap ->
            val updated = currentMap - key
            saveReceivedWallpapers(updated)
            updated
        }
    }

    fun saveReceivedWallpaper(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val nearbyDir = File(context.filesDir, "nearby_wallpapers").also { it.mkdirs() }
            val destFile = File(nearbyDir, "wp_${System.currentTimeMillis()}.jpg")
            try {
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                val key = System.currentTimeMillis().toString()
                val fileUri = destFile.toUri().toString()
                _receivedWallpapers.update { currentMap ->
                    val updated = currentMap + (key to fileUri)
                    saveReceivedWallpapers(updated)
                    updated
                }
                sendWallpaperNotification()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadReceivedWallpapers(): Map<String, String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NEARBY_WALLPAPERS, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split(RECORD_SEP).mapNotNull { record ->
            val parts = record.split(FIELD_SEP, limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    private fun saveReceivedWallpapers(map: Map<String, String>) {
        val raw = map.entries.joinToString(RECORD_SEP) { "${it.key}$FIELD_SEP${it.value}" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_NEARBY_WALLPAPERS, raw) }
    }

    init {
        createNotificationChannel()
        observeForNotifications()
    }

    private fun observeForNotifications() {
        var previousMap = repository.receivedThemes.value
        repository.receivedThemes.onEach { currentMap ->
            currentMap.forEach { (sender, themes) ->
                val previousCount = previousMap[sender]?.size ?: 0
                val addedCount = themes.size - previousCount
                if (addedCount > 0) {
                    sendThemeNotification(sender, addedCount)
                }
            }
            previousMap = currentMap
        }.launchIn(viewModelScope)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Theme Sharing",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies when custom themes are received via Ping"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun sendThemeNotification(senderName: String, count: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val text = if (count == 1) context.getString(R.string.theme_sharing_notification_single, senderName)
                   else context.getString(R.string.theme_sharing_notification_multiple, count, senderName)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.theme_sharing_notification_title))
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendWallpaperNotification() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.wallpaper_sharing_notification_title))
            .setContentText(context.getString(R.string.wallpaper_sharing_notification_text))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }


    companion object {
        private const val CHANNEL_ID = "ping_received_themes"
    }
}
