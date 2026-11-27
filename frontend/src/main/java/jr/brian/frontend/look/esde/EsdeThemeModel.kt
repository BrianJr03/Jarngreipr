package jr.brian.frontend.look.esde

/**
 * An ES-DE theme read for a set of player systems, before any mapping to Jarn.
 * Every value is a resolved string: variables substituted, relative paths made relative to the theme root.
 */
data class EsdeThemeModel(
    val capabilities: EsdeThemeCapabilities,
    val selection: EsdeThemeSelection,
    /** Keyed by [EsdeSystemRef.name]. */
    val systems: Map<String, EsdeSystemTheme>,
    val issues: List<EsdeParseIssue>,
) {
    val isLegacy: Boolean get() = issues.any { it.kind == EsdeParseIssue.Kind.LegacyFormat }
}

/** One player system as ES-DE sees it. [theme] is the theme folder name, usually the same as [name]. */
data class EsdeSystemRef(
    val name: String,
    val fullName: String = name,
    val theme: String = name,
)

data class EsdeSystemTheme(
    val system: EsdeSystemRef,
    val views: Map<EsdeView, EsdeViewModel>,
)

enum class EsdeView(val xmlName: String) {
    System("system"),
    Gamelist("gamelist");

    companion object {
        fun fromXml(name: String): EsdeView? = entries.firstOrNull { it.xmlName == name }
    }
}

/** Elements in first-definition order; a later definition of the same key replaces single properties. */
data class EsdeViewModel(val elements: Map<EsdeElementKey, Map<String, String>>) {
    operator fun get(type: String, name: String): Map<String, String>? = elements[EsdeElementKey(type, name)]
}

/** ES-DE merges elements by type and name, so `image` "logo" and `text` "logo" are different elements. */
data class EsdeElementKey(val type: String, val name: String)

data class EsdeThemeCapabilities(
    val themeName: String? = null,
    val variants: List<EsdeNamedOption> = emptyList(),
    val colorSchemes: List<EsdeNamedOption> = emptyList(),
    val fontSizes: List<String> = emptyList(),
    val aspectRatios: List<String> = emptyList(),
)

/** [label] is the theme author's text, shown as-is in the theme picker. */
data class EsdeNamedOption(val name: String, val label: String? = null, val selectable: Boolean = true)

/** A null field picks the theme's first declared option. */
data class EsdeThemeSelection(
    val variant: String? = null,
    val colorScheme: String? = null,
    val fontSize: String? = null,
    val aspectRatio: String? = null,
)

/** Something the parser skipped. [file] is relative to the theme root; [systemId] is null for theme-wide problems. */
data class EsdeParseIssue(
    val kind: Kind,
    val file: String,
    val detail: String,
    val line: Int? = null,
    val systemId: String? = null,
) {
    enum class Kind {
        MissingCapabilities,
        MissingThemeFile,
        LegacyFormat,
        Unreadable,
        Malformed,
        DocTypeRefused,
        FileTooLarge,
        TotalSizeExceeded,
        PathOutsideTheme,
        MissingInclude,
        IncludeCycle,
        IncludeTooDeep,
        UnknownVariable,
        UnknownSelection,
    }
}
