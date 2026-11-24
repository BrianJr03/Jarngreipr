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

/**
 * Which experience a launcher entry opens. Builds without Jarn FE always open Classic, even when
 * a restored backup stored [Experience.JarnFE]. With no saved choice, builds that include Jarn FE
 * open it.
 */
fun resolveLauncherExperience(
    jarnAvailable: Boolean,
    stored: Experience?,
    forceClassic: Boolean,
): Experience = when {
    !jarnAvailable || forceClassic -> Experience.Classic
    else -> stored ?: Experience.JarnFE
}
