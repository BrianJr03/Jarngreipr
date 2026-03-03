package jr.brian.home.viewmodels

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.R
import jr.brian.home.data.ThemeShareRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeShareViewModel @Inject constructor(
    private val repository: ThemeShareRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val receivedThemes = repository.receivedThemes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

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
            "Received Themes",
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
        val text = if (count == 1) context.getString(R.string.received_themes_notification_single, senderName)
                   else context.getString(R.string.received_themes_notification_multiple, count, senderName)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.received_themes_notification_title))
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "ping_received_themes"
    }
}
