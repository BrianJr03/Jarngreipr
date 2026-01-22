package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.NotificationCountManager

val LocalNotificationCountManager = staticCompositionLocalOf<NotificationCountManager> {
    error("No NotificationCountManager provided")
}
