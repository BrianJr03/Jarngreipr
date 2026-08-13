package jr.brian.home.esde.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemCustomization(
    val backgroundUri: String? = null,
    val showName: Boolean = true,
    val solidColorArgb: Long? = null,
    /**
     * Full-screen background shown while this system is focused. Independent of
     * [backgroundUri], which is the tile's own art.
     */
    val focusBackgroundUri: String? = null
) {
    companion object {
        const val TRANSPARENT_ARGB: Long = 0x00000000L
    }
}
