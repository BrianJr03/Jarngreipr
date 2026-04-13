package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.NotificationManager

val LocalNotificationManager = staticCompositionLocalOf<NotificationManager> {
    error("No NotificationCountManager provided")
}
