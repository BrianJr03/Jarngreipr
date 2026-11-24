package jr.brian.frontend.look

import kotlin.math.roundToInt

/** Which physical screen keeps the controller after a touch. */
enum class JarnTapControl(val id: String) {
    FollowTap("follow"),
    AlwaysTop("top"),
    AlwaysBottom("bottom"),
    ;

    companion object {
        fun fromId(id: String?): JarnTapControl? = entries.firstOrNull { it.id == id }
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
    val screenSwitchMillis: Int = DEFAULT_SCREEN_SWITCH_MILLIS,
) {
    fun coerced(): JarnDisplayPrefs = copy(
        systemTileScale = coercedScale(systemTileScale),
        gameTileScale = coercedScale(gameTileScale),
        screenSwitchMillis = snappedScreenSwitchMillis(screenSwitchMillis.toFloat()),
    )

    companion object {
        const val DEFAULT_TILE_SCALE = 1f
        const val MIN_TILE_SCALE = 0.75f
        const val MAX_TILE_SCALE = 1.5f
        const val TILE_SCALE_STEPS = 15
        const val DEFAULT_SCREEN_SWITCH_MILLIS = 300
        const val MIN_SCREEN_SWITCH_MILLIS = 300
        const val MAX_SCREEN_SWITCH_MILLIS = 1000
        private const val SCREEN_SWITCH_STEP_MILLIS = 100
        const val SCREEN_SWITCH_STEPS =
            (MAX_SCREEN_SWITCH_MILLIS - MIN_SCREEN_SWITCH_MILLIS) / SCREEN_SWITCH_STEP_MILLIS

        /** Rounds to the slider's 100 ms steps and keeps a hand-edited backup in range. */
        fun snappedScreenSwitchMillis(millis: Float): Int {
            if (millis.isNaN()) return DEFAULT_SCREEN_SWITCH_MILLIS
            val snapped = (millis / SCREEN_SWITCH_STEP_MILLIS).roundToInt() * SCREEN_SWITCH_STEP_MILLIS
            return snapped.coerceIn(MIN_SCREEN_SWITCH_MILLIS, MAX_SCREEN_SWITCH_MILLIS)
        }

        /** A hand-edited backup can hold NaN, which coerceIn would keep. */
        private fun coercedScale(scale: Float): Float =
            if (scale.isNaN()) DEFAULT_TILE_SCALE else scale.coerceIn(MIN_TILE_SCALE, MAX_TILE_SCALE)
    }
}
