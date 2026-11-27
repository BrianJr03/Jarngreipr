package jr.brian.frontend.look.esde

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.ThemeAxis
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeSource
import jr.brian.core.model.theme.ThemeTokens
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import kotlin.math.roundToInt

private val PRIMARY_TYPES = setOf("carousel", "grid", "textlist")
private const val DEFAULT_MAX_ITEMS = 3f
private const val DEFAULT_ITEM_SCALE = 1.2f
private const val MAX_FOCUSED_SCALE = 2f
private const val DEFAULT_GRID_ITEM_WIDTH = 0.15f
private const val DEFAULT_GRID_ITEM_HEIGHT = 0.25f
private const val DEFAULT_GRID_SPACING = 0.02f
private const val MIN_COLUMNS = 2
private const val MAX_COLUMNS = 10
private const val MIN_ROWS = 3
private const val MAX_ROWS = 20
private const val DEFAULT_LIST_FONT_SIZE = 0.045f
private const val DEFAULT_LIST_LINE_SPACING = 1.5f
private const val MIN_PRIMARY_HEIGHT = 0.15f
private const val MIN_LAYER_HEIGHT = 0.03f
private const val BACKDROP_WIDTH = 0.95f
private val DefaultCarouselBox = ThemeRect(0f, 0.38f, 1f, 0.24f)
private val DefaultBox = ThemeRect(0.05f, 0.1f, 0.9f, 0.8f)

/**
 * Turns a parsed ES-DE theme into a Jarn theme: the layout keeps its shape, anything Jarn can't
 * draw is listed in the report, and nothing throws. Layout comes from the first system that
 * defines each element; elements only some systems have show only on those.
 */
object EsdeThemeTranslator {

    fun translate(model: EsdeThemeModel, options: EsdeTranslateOptions): EsdeTranslation {
        val report = EsdeReportBuilder()
        val aspect = aspectOf(model.selection.aspectRatio)
        // TODO(#568): translate the theme's narrow aspect ratios into ViewSpec.narrow once it lands.
        val views = ThemeViews(
            systems = translateView(model, EsdeView.System, aspect, report)?.clearOfBand(options.systemsTopClear, report),
            games = translateView(model, EsdeView.Gamelist, aspect, report),
        )
        report.notUsed(null, null, EsdeTranslationNote.TransitionsAndTriggers)
        val spec = JarnThemeSpec(
            meta = ThemeMeta(
                name = model.capabilities.themeName?.ifBlank { null } ?: options.fallbackName,
                source = ThemeSource.EsDe,
            ),
            // TODO(M2 fallback ticket): repair colorCandidates into tokens; fonts and radius stay Jarn's for now.
            tokens = ThemeTokens(colors = options.fallbackColors),
            views = views,
        )
        return EsdeTranslation(spec, report.assets(), report.colors(), report.report())
    }

    private fun translateView(model: EsdeThemeModel, view: EsdeView, aspect: Float, report: EsdeReportBuilder): ViewSpec? {
        val elements = model.elementsIn(view)
        if (elements.isEmpty()) return null
        val primaries = elements.filter { it.key.type in PRIMARY_TYPES && it.bool("visible") != false }
        val primaryElement = primaries.firstOrNull()
        primaries.drop(1).forEach { report.notUsed(view, it.key, EsdeTranslationNote.ExtraPrimary) }
        val primary = primaryElement?.let { primaryOf(it, view, aspect, report) }
        if (primary == null) {
            report.notUsed(view, null, EsdeTranslationNote.NoPrimary)
            return null
        }
        report.collectColors(view, primaryElement)
        report.translated(view, primaryElement.key)

        val primaryZ = primaryElement.float("zIndex") ?: ESDE_PRIMARY_Z
        val layers = EsdeLayerTranslator(view, primaryZ, aspect, report).let { translator ->
            elements.filter { it.key.type !in PRIMARY_TYPES }.mapNotNull(translator::translate)
        }
        return ViewSpec(primary = primary, layers = layers.sortedBy { it.z })
    }

    private fun primaryOf(element: EsdeElementAcrossSystems, view: EsdeView, aspect: Float, report: EsdeReportBuilder): ThemePrimary =
        when (element.key.type) {
            "carousel" -> shelfOf(element, view, report)
            "grid" -> gridOf(element, view, aspect, report)
            else -> textListOf(element)
        }

    private fun shelfOf(element: EsdeElementAcrossSystems, view: EsdeView, report: EsdeReportBuilder): ThemePrimary.Shelf {
        val type = element.props["type"]?.trim().orEmpty()
        if (type.endsWith("Wheel")) report.adjusted(view, element.key, EsdeTranslationNote.WheelAsShelf)
        val maxItems = element.float("maxItemCount") ?: DEFAULT_MAX_ITEMS
        val side = ((maxItems - 1f) / 2f).toInt().coerceAtLeast(1)
        return ThemePrimary.Shelf(
            box = element.rect(DefaultCarouselBox.width to DefaultCarouselBox.height).rect.withDefaultPos(element, DefaultCarouselBox),
            art = primaryArt(element, view, report),
            axis = if (type.startsWith("vertical")) ThemeAxis.Vertical else ThemeAxis.Horizontal,
            itemsBefore = element.float("itemsBeforeCenter")?.roundToInt() ?: side,
            itemsAfter = element.float("itemsAfterCenter")?.roundToInt() ?: side,
            focusedScale = (element.float("itemScale") ?: DEFAULT_ITEM_SCALE).coerceIn(1f, MAX_FOCUSED_SCALE),
        )
    }

    private fun gridOf(element: EsdeElementAcrossSystems, view: EsdeView, aspect: Float, report: EsdeReportBuilder): ThemePrimary.Grid {
        val box = element.rect(DefaultBox.width to DefaultBox.height).rect.withDefaultPos(element, DefaultBox)
        val (itemWidth, itemHeight) = element.pair("itemSize") ?: (DEFAULT_GRID_ITEM_WIDTH to DEFAULT_GRID_ITEM_HEIGHT)
        val spacing = element.pair("itemSpacing")?.first ?: DEFAULT_GRID_SPACING
        val columns = (box.width / (itemWidth + spacing).coerceAtLeast(0.01f)).roundToInt()
        return ThemePrimary.Grid(
            box = box,
            art = primaryArt(element, view, report),
            columns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            itemAspect = if (itemHeight > 0f) itemWidth * aspect / itemHeight else 1f,
            spacing = (spacing / box.width).coerceIn(0f, 1f),
        )
    }

    private fun textListOf(element: EsdeElementAcrossSystems): ThemePrimary.List {
        val box = element.rect(DefaultBox.width to DefaultBox.height).rect.withDefaultPos(element, DefaultBox)
        val lineHeight = (element.float("fontSize") ?: DEFAULT_LIST_FONT_SIZE) *
            (element.float("lineSpacing") ?: DEFAULT_LIST_LINE_SPACING)
        return ThemePrimary.List(
            box = box,
            rows = linesThatFit(box.height, lineHeight).coerceIn(MIN_ROWS, MAX_ROWS),
            align = alignOf(element.props["horizontalAlignment"]),
        )
    }

    /** A primary with no pos or size takes ES-DE's default placement for its type. */
    private fun ThemeRect.withDefaultPos(element: EsdeElementAcrossSystems, default: ThemeRect): ThemeRect =
        if (element.props["pos"] == null && element.props["size"] == null) default else this

    /** Games show their imageType; systems show a static image, which per system means Jarn's own logo. */
    private fun primaryArt(element: EsdeElementAcrossSystems, view: EsdeView, report: EsdeReportBuilder): ThemePicture? {
        mediaKindsOf(element.props["imageType"]).firstOrNull { it != null }?.let { return ThemePicture.GameMedia(it) }
        val path = element.props["staticImage"]?.ifBlank { null } ?: return null
        if (element.varies("staticImage")) {
            report.adjusted(view, element.key, EsdeTranslationNote.PerSystemFileAsSystemArt)
            return ThemePicture.SystemArt(SystemArtKind.Logo)
        }
        return report.asset(themeFilePath(path))
    }

    /** Keeps the top [band] of the Systems view free; full-width backdrops may still sit behind it. */
    private fun ViewSpec.clearOfBand(band: Float, report: EsdeReportBuilder): ViewSpec {
        val box = primary.box
        val clearedPrimary = if (box.top() < band) {
            val bottom = box.top() + box.height
            val height = (bottom - band).coerceAtLeast(MIN_PRIMARY_HEIGHT)
            report.adjusted(EsdeView.System, null, EsdeTranslationNote.MovedBelowHeaderBand)
            primary.withBox(ThemeRect(box.x, band + box.originY * height, box.width, height, box.originX, box.originY))
        } else {
            primary
        }
        val clearedLayers = layers.map { layer ->
            if (layer.isBackdrop() || layer.rect.top() >= band) return@map layer
            report.adjusted(EsdeView.System, null, EsdeTranslationNote.MovedBelowHeaderBand)
            layer.withRect(layer.rect.shiftedBelow(band))
        }
        return copy(primary = clearedPrimary, layers = clearedLayers)
    }

    private fun ThemeLayer.isBackdrop(): Boolean =
        (this is ThemeLayer.Picture || this is ThemeLayer.Panel || this is ThemeLayer.Video) && rect.width >= BACKDROP_WIDTH

    private fun ThemeRect.shiftedBelow(band: Float): ThemeRect {
        val height = height.coerceAtMost(1f - band).coerceAtLeast(MIN_LAYER_HEIGHT)
        return copy(y = band + originY * height, height = height)
    }

    private fun ThemePrimary.withBox(box: ThemeRect): ThemePrimary = when (this) {
        is ThemePrimary.Shelf -> copy(box = box)
        is ThemePrimary.Grid -> copy(box = box)
        is ThemePrimary.List -> copy(box = box)
    }

    private fun ThemeLayer.withRect(rect: ThemeRect): ThemeLayer = when (this) {
        is ThemeLayer.Picture -> copy(rect = rect)
        is ThemeLayer.Text -> copy(rect = rect)
        is ThemeLayer.Rating -> copy(rect = rect)
        is ThemeLayer.Date -> copy(rect = rect)
        is ThemeLayer.Panel -> copy(rect = rect)
        is ThemeLayer.Video -> copy(rect = rect)
    }
}
