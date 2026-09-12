package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.VolumeChordManager

val LocalVolumeChordManager = staticCompositionLocalOf<VolumeChordManager> {
    error("No VolumeChordManager provided")
}
