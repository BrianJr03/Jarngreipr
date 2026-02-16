package jr.brian.home.model

import jr.brian.home.esde.preferences.VideoScaleMode

data class VideoLaunchEvent(
    val videoPath: String,
    val audioEnabled: Boolean,
    val scaleMode: VideoScaleMode = VideoScaleMode.FillScreen
)