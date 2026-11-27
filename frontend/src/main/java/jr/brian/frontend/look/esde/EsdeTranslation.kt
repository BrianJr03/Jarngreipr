package jr.brian.frontend.look.esde

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeColors

/**
 * An ES-DE theme as a Jarn theme. [assets] are the theme-relative files the spec points at, to
 * copy next to it. [colorCandidates] are the raw colors the layout uses, for the M2 fallback
 * ticket to turn into readable tokens; until then the spec carries the caller's colors.
 */
data class EsdeTranslation(
    val spec: JarnThemeSpec,
    val assets: List<String>,
    val colorCandidates: List<EsdeColorCandidate>,
    val report: EsdeTranslationReport,
)

/**
 * [systemsTopClear] is the top band of the Systems view that stays free for the Continue playing
 * strip (the frontend's library header band). [fallbackColors] fill the spec's tokens.
 */
data class EsdeTranslateOptions(
    val fallbackColors: ThemeColors,
    val fallbackName: String,
    val systemsTopClear: Float,
)

/** A raw ES-DE color (`RRGGBB` or `RRGGBBAA`) and where it was set. */
data class EsdeColorCandidate(
    val view: EsdeView,
    val element: EsdeElementKey,
    val property: String,
    val value: String,
)

data class EsdeTranslationReport(
    val translated: List<EsdeReportItem>,
    val adjusted: List<EsdeReportItem>,
    val notUsed: List<EsdeReportItem>,
)

/** [element] is null for notes about the whole theme or view. The UI turns [note] into player-facing text. */
data class EsdeReportItem(
    val view: EsdeView?,
    val element: EsdeElementKey?,
    val note: EsdeTranslationNote? = null,
)

enum class EsdeTranslationNote {
    /** A wheel carousel became a straight shelf. */
    WheelAsShelf,
    /** Moved down so the Continue playing strip stays clear. */
    MovedBelowHeaderBand,
    /** The theme left the size to the image; Jarn picked one. */
    SizeGuessed,
    /** Tiled images draw cropped instead. */
    TileAsCrop,
    /** A per-system file became Jarn's own system logo or art. */
    PerSystemFileAsSystemArt,
    /** The element only exists for some systems, so it only shows on those. */
    ShownOnSomeSystems,
    /** The first media type Jarn doesn't track (3D box, back cover) was skipped for the next one. */
    MediaKindSkipped,
    /** The theme's own font isn't used yet. */
    FontNotUsed,
    /** An ES-DE element type Jarn has no layer for. */
    UnsupportedElement,
    /** A second browsable list in the same view. Jarn uses the first. */
    ExtraPrimary,
    /** Only media Jarn doesn't track (3D box, back cover). */
    UnsupportedMedia,
    /** A file or text that changes per system in a way Jarn can't follow. */
    DiffersPerSystem,
    /** Game or system info Jarn doesn't show. */
    UnsupportedField,
    /** Everything the element set was unreadable, unknown or outside the theme folder. */
    NothingReadable,
    /** Hidden by the theme. */
    Hidden,
    /** The view has no carousel, grid or text list, so Jarn keeps its own layout. */
    NoPrimary,
    /** A theme in the format from before ES-DE, which Jarn doesn't read. */
    LegacyTheme,
    /** Transitions and variant triggers (no videos, no media) aren't read from ES-DE themes yet. */
    TransitionsAndTriggers,
}
