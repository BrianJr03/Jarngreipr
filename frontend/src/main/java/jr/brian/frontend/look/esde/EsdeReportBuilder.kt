package jr.brian.frontend.look.esde

import jr.brian.core.model.theme.ThemePicture

/** Collects the report, assets and raw colors while a theme translates. Repeated items are kept once. */
internal class EsdeReportBuilder {
    private val translated = LinkedHashSet<EsdeReportItem>()
    private val adjusted = LinkedHashSet<EsdeReportItem>()
    private val notUsed = LinkedHashSet<EsdeReportItem>()
    private val assets = LinkedHashSet<String>()
    private val colors = LinkedHashSet<EsdeColorCandidate>()

    fun translated(view: EsdeView, key: EsdeElementKey) {
        translated += EsdeReportItem(view, key)
    }

    fun adjusted(view: EsdeView?, key: EsdeElementKey?, note: EsdeTranslationNote) {
        adjusted += EsdeReportItem(view, key, note)
    }

    fun notUsed(view: EsdeView?, key: EsdeElementKey?, note: EsdeTranslationNote) {
        notUsed += EsdeReportItem(view, key, note)
    }

    fun asset(path: String): ThemePicture.File {
        assets += path
        return ThemePicture.File(path)
    }

    /** Every `color` / `...Color` property, raw, for the M2 fallback ticket to repair into tokens. */
    fun collectColors(view: EsdeView, element: EsdeElementAcrossSystems) {
        element.props.forEach { (property, value) ->
            if (property == "color" || property.endsWith("Color")) {
                colors += EsdeColorCandidate(view, element.key, property, value.trim())
            }
        }
    }

    fun assets(): List<String> = assets.toList()
    fun colors(): List<EsdeColorCandidate> = colors.toList()
    fun report() = EsdeTranslationReport(translated.toList(), adjusted.toList(), notUsed.toList())
}
