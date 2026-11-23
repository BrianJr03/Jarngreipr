package jr.brian.core.model

/**
 * Where Jarn FE looks first for system tile art. Classic ignores this value.
 *
 * Stored by [name] in preferences and backups, so renaming an entry is a
 * breaking change.
 */
enum class SystemArtSource {
    RetroHrai,
    Bundled;

    companion object {
        val DEFAULT = RetroHrai

        fun fromNameOrDefault(name: String?): SystemArtSource =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
