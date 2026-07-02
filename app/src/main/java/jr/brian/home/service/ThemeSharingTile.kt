package jr.brian.home.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import jr.brian.home.MainActivity
import jr.brian.home.R
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.util.PingThemeUtil
import jr.brian.ping.PingPermissions.hasPingPermissions
import jr.brian.ping.PingService

class ThemeSharingTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        if (active) {
            stopService(Intent(this, PingService::class.java))
            prefs.edit { putBoolean(KEY_ACTIVE, false) }
        } else {
            if (hasPingPermissions()) {
                startThemeSharing()
                prefs.edit { putBoolean(KEY_ACTIVE, true) }
            }
        }
        updateTileState()
    }

    private fun startThemeSharing() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val themeId =
            prefs.getString(KEY_THEME, ColorTheme.PINK_VIOLET.id) ?: ColorTheme.PINK_VIOLET.id
        val theme = loadTheme(prefs, themeId)
        val name = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty().ifBlank { Build.MODEL }
        PingService.notificationTitle = getString(R.string.ping_notification_title)
        PingService.notificationIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, ThemeShareReceiver::class.java).apply {
                action = MainActivity.ACTION_OPEN_THEME_SHARE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val profile = PingThemeUtil.buildProfile(theme, name)
        startForegroundService(PingService.buildIntent(this, profile))
    }

    private fun loadTheme(
        prefs: SharedPreferences,
        themeId: String
    ): ColorTheme {
        if (!themeId.startsWith(ColorTheme.CUSTOM_THEME_PREFIX)) return ColorTheme.fromId(themeId)
        val raw = prefs.getString(KEY_CUSTOM_THEMES, "").orEmpty()
        return raw.split("|||").firstNotNullOfOrNull { data ->
            val parts = data.split(":::", limit = 5)
            if (parts.size >= 5 && parts[0] == themeId)
                runCatching {
                    ColorTheme.fromCustomData(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4].toBoolean()
                    )
                }.getOrNull()
            else null
        } ?: ColorTheme.PINK_VIOLET
    }

    private fun updateTileState() {
        val active = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)
        qsTile?.apply {
            icon = Icon.createWithResource(this@ThemeSharingTile, R.drawable.ic_tile_share)
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    companion object {
        private const val PREFS = "gaming_launcher_prefs"
        private const val KEY_ACTIVE = "ping_tile_active"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_CUSTOM_THEMES = "custom_themes"
        private const val KEY_DISPLAY_NAME = "ping_display_name"

        fun syncState(context: Context, isActive: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit { putBoolean(KEY_ACTIVE, isActive) }
            requestListeningState(context, ComponentName(context, ThemeSharingTile::class.java))
        }
    }
}
