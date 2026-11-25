package jr.brian.frontend.look

import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.core.ui.theme.JarnSwitchCue

/** Which physical screen keeps the controller after a touch. [FollowNoTap] leaves it where X put it. */
enum class JarnTapControl(val id: String) {
    FollowTap("follow"),
    FollowNoTap("follow_no_tap"),
    AlwaysTop("top"),
    AlwaysBottom("bottom"),
    ;

    companion object {
        fun fromId(id: String?): JarnTapControl? = entries.firstOrNull { it.id == id }
    }
}

/** How long the bottom screen's Now Playing waits before it dims. [seconds] is null for never. */
enum class JarnPlayingDim(val id: String, val seconds: Int?) {
    TenSeconds("10s", 10),
    ThirtySeconds("30s", 30),
    OneMinute("1m", 60),
    FiveMinutes("5m", 300),
    Never("never", null),
    ;

    companion object {
        fun fromId(id: String?): JarnPlayingDim? = entries.firstOrNull { it.id == id }
    }
}

/** Jarn FE display choices. The defaults match how Jarn FE looked before they existed. */
data class JarnDisplayPrefs(
    val systemTileScale: Float = DEFAULT_TILE_SCALE,
    val gameTileScale: Float = DEFAULT_TILE_SCALE,
    val hintsVisible: Boolean = true,
    val buttonHaptics: Boolean = true,
    val tapControl: JarnTapControl = JarnTapControl.FollowTap,
    val appNamesVisible: Boolean = true,
    val undoCountdownVisible: Boolean = true,
    val switchCue: JarnSwitchCue = JarnSwitchCue.Pulse,
    val systemView: JarnBrowseView = JarnBrowseView.Grid,
    val gameView: JarnBrowseView = JarnBrowseView.Grid,
    val pageTransition: JarnPageTransition = JarnPageTransition.JarnSlide,
    val systemNamesVisible: Boolean = true,
    val randomVisible: Boolean = true,
    val lastPlayedVisible: Boolean = true,
    val systemLogoVisible: Boolean = true,
    val gameCountVisible: Boolean = true,
    val lastPlayedTextVisible: Boolean = true,
    val gameRowVisible: Boolean = true,
    val systemBackgroundVisible: Boolean = true,
    val playingScreenVisible: Boolean = true,
    val playingDim: JarnPlayingDim = JarnPlayingDim.ThirtySeconds,
    val playingScreenOff: Boolean = false,
) {
    fun coerced(): JarnDisplayPrefs = copy(
        systemTileScale = coercedScale(systemTileScale),
        gameTileScale = coercedScale(gameTileScale),
    )

    companion object {
        const val DEFAULT_TILE_SCALE = 1f
        const val MIN_TILE_SCALE = 0.75f
        const val MAX_TILE_SCALE = 1.5f
        const val TILE_SCALE_STEPS = 15

        /** A hand-edited backup can hold NaN, which coerceIn would keep. */
        private fun coercedScale(scale: Float): Float =
            if (scale.isNaN()) DEFAULT_TILE_SCALE else scale.coerceIn(MIN_TILE_SCALE, MAX_TILE_SCALE)
    }
}
