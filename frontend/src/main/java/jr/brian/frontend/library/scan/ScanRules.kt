package jr.brian.frontend.library.scan

/**
 * Shared scan filters. Both [FastWalkScanner] (file-system) and the SAF
 * tree scanner apply the same rules so switching between storage
 * backends never changes what counts as a ROM.
 *
 * These values are deliberately a direct port of the equivalents in
 * `:app`'s `RomScanner` — the Phase 2+ engine should see the same
 * library the user sees today once Phase 3 flips the UI across.
 */
internal object ScanRules {

    /** Filenames and directories we always skip because they are never a ROM. */
    val SKIPPED_NAMES: Set<String> = setOf("media", ".DS_Store", "systeminfo.txt")

    /**
     * Extensions the scanner never treats as a ROM even if a system
     * claims them. Save-state files, scraper media sidecars, parse
     * output — anything that lives next to a ROM but is not itself one.
     */
    val ALWAYS_SKIPPED_EXTENSIONS: Set<String> = setOf(
        "txt", "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg",
        "sav", "srm", "state", "auto", "sram", "rtc", "dsv",
        "xml", "cfg", "ini", "log", "bak",
    )

    /** Depth bound on the recursive walk. Real ROM libraries rarely nest deeper than this. */
    const val WALK_MAX_DEPTH: Int = 6

    /**
     * Should the scanner descend into [name]? Hidden entries, SKIPPED_NAMES,
     * and the media directory are all pruned here.
     */
    fun shouldDescend(name: String): Boolean =
        !name.startsWith(".") && name !in SKIPPED_NAMES

    /**
     * Does [fileName] match an extension this system accepts? Returns false
     * for empty extensions, for globally-skipped extensions, and when the
     * extension isn't in [extensions].
     */
    fun matchesExtension(fileName: String, extensions: Set<String>): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return false
        if (ext in ALWAYS_SKIPPED_EXTENSIONS) return false
        return ext in extensions
    }
}
