package jr.brian.frontend.library.art

import jr.brian.frontend.library.media.MediaKind
import java.io.File

/** Images the user set by hand for one game. A null path means no custom image. */
data class CustomGameArt(
    val cover: String? = null,
    val marquee: String? = null,
    val fanart: String? = null,
    val screenshot: String? = null,
) {
    fun pathFor(kind: MediaKind): String? = when (kind) {
        MediaKind.Cover -> cover
        MediaKind.Marquee -> marquee
        MediaKind.Fanart -> fanart
        MediaKind.Screenshot -> screenshot
        else -> null
    }
}

/** The user's custom art owned by `:app`, read-only here, keyed by [customArtKey]. */
fun interface CustomArtSource {
    fun forSystem(systemId: String): Map<String, CustomGameArt>
}

/** Matches the key Classic's Edit Info saves under, so both find the same game. */
internal fun customArtKey(absolutePath: String?, relativePath: String): String =
    File(absolutePath ?: relativePath).nameWithoutExtension
