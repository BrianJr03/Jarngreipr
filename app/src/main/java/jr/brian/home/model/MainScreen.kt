package jr.brian.home.model

/**
 * Which display hosts `MainActivity` (and therefore receives input focus) when
 * [HomeTarget.BOTH] is selected.
 *
 * Meaningless for [HomeTarget.TOP] and [HomeTarget.BOTTOM] — those targets
 * already pin MainActivity to a specific display. Persisted alongside
 * [HomeTarget] so its value is remembered when the user flips back to BOTH.
 */
enum class MainScreen {
    /** MainActivity on the primary/top display; FrontEndActivity on the bottom. */
    TOP,

    /** MainActivity on the external/bottom display; FrontEndActivity on the top. */
    BOTTOM;

    companion object {
        /** Parses a stored name back to [MainScreen], falling back to [BOTTOM]. */
        fun fromNameOrDefault(name: String?): MainScreen =
            entries.firstOrNull { it.name == name } ?: BOTTOM
    }
}
