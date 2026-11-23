package jr.brian.core.model

/**
 * Where Jarn FE looks first for game art. Classic ignores this value.
 *
 * Stored by [name] in preferences and backups, so renaming an entry is a
 * breaking change.
 */
enum class ArtSource {
    EsDe,
    RetroHrai,
    Scraped;

    /** This source first, then the others in declaration order to fill gaps. */
    fun priority(): List<ArtSource> = listOf(this) + entries.filter { it != this }

    companion object {
        val DEFAULT = EsDe

        fun fromNameOrDefault(name: String?): ArtSource =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
