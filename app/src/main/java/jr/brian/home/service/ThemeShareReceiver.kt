package jr.brian.home.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import jr.brian.home.MainActivity

class ThemeShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MainActivity.ACTION_OPEN_THEME_SHARE) {
            MainActivity.openThemeShareRequests.tryEmit(Unit)
        }
    }
}
