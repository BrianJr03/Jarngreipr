package jr.brian.frontend.library.media

/**
 * The art kinds Jarngreipr tracks per game. Each enum value is tied to
 * the folder name a layout uses on disk; the folder names match ES-DE's
 * `downloaded_media/<system>/<folder>/` convention so a user running
 * real ES-DE sees the same art Jarngreipr does.
 *
 * Enum constants are stable identifiers — persisted in
 * [jr.brian.frontend.library.db.MediaEntity] column names, used as map
 * keys in [MediaIndex]. Do not reorder; add new kinds at the end.
 */
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
