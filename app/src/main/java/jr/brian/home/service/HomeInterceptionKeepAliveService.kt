package jr.brian.home.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.MainActivity
import jr.brian.home.R

/**
 * Minimal foreground service whose only job is to keep the app process alive so
 * [HomeInterceptorService] is less likely to be killed by aggressive OEM battery
 * management on these dual-screen handhelds.
 *
 * Lifecycle is tied 1:1 to the "Home Button Interception" toggle:
 * [start] when the toggle flips on, [stop] when it flips off. This service does
 * NOT own the toggle state — it only reacts to it.
 */
@AndroidEntryPoint
class HomeInterceptionKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        val notification = buildNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun buildNotification(context: Context): Notification {
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.home_interception_notification_title))
            .setContentText(context.getString(R.string.home_interception_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(tapIntent)
            .build()
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.home_interception_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description =
                    context.getString(R.string.home_interception_channel_description)
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "home_interception_keepalive"
        const val NOTIFICATION_ID = 8801

        /** Start the keep-alive when the interception toggle is turned on. */
        fun start(context: Context) {
            val intent = Intent(context, HomeInterceptionKeepAliveService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Stop the keep-alive when the toggle is turned off. */
        fun stop(context: Context) {
            context.stopService(Intent(context, HomeInterceptionKeepAliveService::class.java))
        }
    }
}
