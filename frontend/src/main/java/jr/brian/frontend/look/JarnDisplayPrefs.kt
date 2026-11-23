package jr.brian.frontend.look

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
