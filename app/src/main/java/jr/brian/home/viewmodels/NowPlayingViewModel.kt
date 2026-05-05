package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.NowPlayingManager
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val nowPlayingManager: NowPlayingManager
) : ViewModel() {
    val nowPlaying = nowPlayingManager.nowPlaying
    val volume = nowPlayingManager.volume
    val currentPosition = nowPlayingManager.currentPosition
    val duration = nowPlayingManager.duration

    fun togglePlayPause() = nowPlayingManager.togglePlayPause()
    fun skipToNext() = nowPlayingManager.skipToNext()
    fun skipToPrevious() = nowPlayingManager.skipToPrevious()
    fun setVolume(v: Float) = nowPlayingManager.setVolume(v)
    fun seekTo(positionMs: Long) = nowPlayingManager.seekTo(positionMs)
}
