package jr.brian.jarnfe.library

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow

/** Stands in for the host player. Tests drive [FakeJarnGameVideo.state] by hand. */
internal class FakeJarnGameVideos : JarnGameVideos {
    val created = mutableListOf<FakeJarnGameVideo>()

    val live: List<FakeJarnGameVideo> get() = created.filterNot { it.released }

    override fun create(context: Context, path: String, crop: Boolean): JarnGameVideo =
        FakeJarnGameVideo(View(context), path, crop).also(created::add)
}

internal class FakeJarnGameVideo(
    override val view: View,
    val path: String,
    val crop: Boolean,
) : JarnGameVideo {
    override val state = MutableStateFlow(JarnGameVideoState.Loading)
    var isPlaying = true
        private set
    var isMuted = true
        private set
    var released = false
        private set

    override fun play() {
        isPlaying = true
    }

    override fun pause() {
        isPlaying = false
    }

    override fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    override fun release() {
        released = true
        isPlaying = false
    }
}
