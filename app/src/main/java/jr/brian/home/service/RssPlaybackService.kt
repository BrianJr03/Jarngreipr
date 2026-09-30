package jr.brian.home.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.util.closeAudioEffectSession
import jr.brian.home.util.openAudioEffectSession

@AndroidEntryPoint
class RssPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var openedAudioSessionId: Int = 0

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (openedAudioSessionId != 0 && openedAudioSessionId != audioSessionId) {
                    closeAudioEffectSession(openedAudioSessionId)
                }
                openAudioEffectSession(audioSessionId)
                openedAudioSessionId = audioSessionId
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        if (openedAudioSessionId != 0) {
            closeAudioEffectSession(openedAudioSessionId)
            openedAudioSessionId = 0
        }
        super.onDestroy()
    }
}
