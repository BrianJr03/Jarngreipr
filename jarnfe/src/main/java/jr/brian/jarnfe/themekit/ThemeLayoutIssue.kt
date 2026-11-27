package jr.brian.jarnfe.themekit

/** Something the layout engine changed or noticed while placing a view, for the builder's fit report. */
internal sealed interface ThemeLayoutIssue {
    /** The layer involved, or null for the primary. */
    val layerId: String?

    /** The primary's box had no area or a non-finite value, so it fills the safe area instead. */
    data object PrimaryInvalid : ThemeLayoutIssue {
        override val layerId: String? = null
    }

    /** The primary was smaller than the minimum and was grown around its center. */
    data object PrimaryTooSmall : ThemeLayoutIssue {
        override val layerId: String? = null
    }

    /** The primary reached past the safe area and was moved or shrunk into it. */
    data object PrimaryClamped : ThemeLayoutIssue {
        override val layerId: String? = null
    }

    /** The layer's rect had no area or a non-finite value. The layer is dropped. */
    data class InvalidRect(override val layerId: String) : ThemeLayoutIssue

    /** The layer is entirely off the screen. The layer is dropped. */
    data class OffScreen(override val layerId: String) : ThemeLayoutIssue

    /** Only one video plays per view. This one shows its fallback picture, or is dropped when it has none. */
    data class ExtraVideo(
        override val layerId: String,
        val playingId: String,
        val fallbackShown: Boolean,
    ) : ThemeLayoutIssue

    /** A text layer sat under the hint bar or a system bar and was moved out. */
    data class TextMoved(override val layerId: String) : ThemeLayoutIssue

    /** A text layer sits under the hint bar or a system bar and is too big to move out. */
    data class TextInSafeZone(override val layerId: String) : ThemeLayoutIssue

    /** A decor layer runs under the hint bar or a system bar. Kept, since Jarn draws its bars on top. */
    data class InSafeZone(override val layerId: String) : ThemeLayoutIssue

    /** A mostly opaque layer above the primary covered most of it, so it now draws below the primary. */
    data class CoveredPrimary(override val layerId: String) : ThemeLayoutIssue
}
