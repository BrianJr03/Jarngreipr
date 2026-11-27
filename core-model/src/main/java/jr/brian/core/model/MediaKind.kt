package jr.brian.core.model

import kotlinx.serialization.Serializable

/**
 * The art kinds Jarngreipr tracks per game. Each enum value is tied to
 * the folder name a layout uses on disk; the folder names match ES-DE's
 * `downloaded_media/<system>/<folder>/` convention so a user running
 * real ES-DE sees the same art Jarngreipr does.
 *
 * Enum constants are stable identifiers: persisted in the frontend's
 * media table column names, used as map keys in its media index and
 * written by name into Jarn theme files. Do not reorder or rename; add
 * new kinds at the end.
 */
@Serializable
enum class MediaKind(val folderName: String) {
    Cover("covers"),
    Marquee("marquees"),
    Fanart("fanart"),
    Screenshot("screenshots"),
    Titlescreen("titlescreens"),
    Miximage("miximages"),
    PhysicalMedia("physicalmedia"),
    Video("videos"),
}
