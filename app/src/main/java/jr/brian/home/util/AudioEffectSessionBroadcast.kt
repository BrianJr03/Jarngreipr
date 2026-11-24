package jr.brian.home.util

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

/**
 * Broadcasts that a new audio session is open so system-wide effect apps
 * (JamesDSP, Wavelet, Viper, and stock equalizers) can attach to it.
 * Sessions that never broadcast are invisible to those apps and receive
 * no processing.
 */
fun Context.openAudioEffectSession(sessionId: Int) {
    if (sessionId == AudioEffect.ERROR_BAD_VALUE || sessionId == 0) return
    val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    runCatching { sendBroadcast(intent) }
}

fun Context.closeAudioEffectSession(sessionId: Int) {
    if (sessionId == AudioEffect.ERROR_BAD_VALUE || sessionId == 0) return
    val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
    }
    runCatching { sendBroadcast(intent) }
}
