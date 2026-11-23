package jr.brian.core.model

/**
 * Which product the user has selected. Independent of `frontendEnabled`, which
 * stays a Classic-only setting so switching experience never rewrites it.
 *
 * Stored by [name] in preferences and backups, so renaming an entry is a
 * breaking change.
 */
enum class Experience {
    Classic,
    JarnFE;

    companion object {
        val DEFAULT = Classic

        fun fromNameOrDefault(name: String?): Experience =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
