package jr.brian.home.util

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Single source of truth for volume control on the AYN Thor, whose two screens
 * take independent levels.
 *
 * The Thor firmware watches two `Settings.System` keys and applies the level to
 * the external (bottom) screen. Any app with the user-grantable `WRITE_SETTINGS`
 * special access can write them — no root, no ADB, no Shizuku. The primary/top
 * screen is plain AudioManager.
 *
 * | Screen  | Mechanism                                                        |
 * |---------|------------------------------------------------------------------|
 * | Top     | `AudioManager.setStreamVolume(STREAM_MUSIC, level, 0)`           |
 * | Bottom  | `Settings.System.putInt("secondary_screen_volume_level", level)` |
 * | Bottom, wired headset plugged in | `secondary_screen_volume_level_for_headphones` |
 */
object ThorVolume {
    const val MIN = 0
    const val MAX = 15
    const val DEFAULT = 7
    const val KEY_SECONDARY = "secondary_screen_volume_level"
    const val KEY_SECONDARY_HEADPHONES = "secondary_screen_volume_level_for_headphones"

    fun canWriteSettings(ctx: Context): Boolean = Settings.System.canWrite(ctx)

    fun manageWriteSettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            .setData("package:${ctx.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun getTop(ctx: Context): Int {
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    fun setTop(ctx: Context, level: Int) {
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            level.coerceIn(MIN, MAX),
            0
        )
    }

    fun getBottom(ctx: Context, headphones: Boolean = false): Int =
        Settings.System.getInt(ctx.contentResolver, keyFor(headphones), DEFAULT)

    fun setBottom(ctx: Context, level: Int, headphones: Boolean = false): Boolean {
        if (!canWriteSettings(ctx)) return false
        val clamped = level.coerceIn(MIN, MAX)
        return Settings.System.putInt(ctx.contentResolver, keyFor(headphones), clamped)
    }

    fun adjustBottom(ctx: Context, delta: Int, headphones: Boolean = false): Boolean {
        val useHeadphones = headphones || isWiredHeadsetConnected(ctx)
        val current = getBottom(ctx, useHeadphones)
        return setBottom(ctx, current + delta, useHeadphones)
    }

    fun isWiredHeadsetConnected(ctx: Context): Boolean {
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    fun bottomVolumeUri(headphones: Boolean = false): Uri =
        Settings.System.getUriFor(keyFor(headphones))

    private fun keyFor(headphones: Boolean): String =
        if (headphones) KEY_SECONDARY_HEADPHONES else KEY_SECONDARY
}
