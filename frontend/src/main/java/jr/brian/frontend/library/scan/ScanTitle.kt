package jr.brian.frontend.library.scan

import java.io.File

/**
 * The title a scan gives a game: its file name without the ROM extension.
 * A folder game found by its markers keeps its whole name, since
 * "Donkey Kong Bananza v1.0" has no ROM extension to strip.
 */
internal fun scannedTitle(relativePath: String, romExtensions: Set<String>): String {
    val file = File(relativePath)
    return if (file.extension.lowercase() in romExtensions) file.nameWithoutExtension else file.name
}
