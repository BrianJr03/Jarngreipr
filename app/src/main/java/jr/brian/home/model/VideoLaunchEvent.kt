package jr.brian.home.model

import jr.brian.home.esde.model.VideoScaleMode

data class VideoLaunchEvent(
    val videoPath: String,
    val audioEnabled: Boolean,
    val scaleMode: VideoScaleMode = VideoScaleMode.FillScreen,
    val overlayEnabled: Boolean = true
)