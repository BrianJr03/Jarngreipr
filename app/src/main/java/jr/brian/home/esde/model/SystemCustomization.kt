package jr.brian.home.esde.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemCustomization(
    val backgroundUri: String? = null,
    val showName: Boolean = true,
    val solidColorArgb: Long? = null
) {
    companion object {
        const val TRANSPARENT_ARGB: Long = 0x00000000L
    }
}
