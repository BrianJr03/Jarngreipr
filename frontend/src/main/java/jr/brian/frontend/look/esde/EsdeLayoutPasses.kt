package jr.brian.frontend.look.esde

import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.SystemTextField
import jr.brian.core.model.theme.THEME_NARROW_ASPECT
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemePrimaryPatch
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeViewPatch
import jr.brian.core.model.theme.ThemeVisibleWhen
import jr.brian.core.model.theme.ViewSpec
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val SAME_ROW = 0.005f
private const val MAX_STACK_GAP = 0.03f
/** Rows may reach this far into the next one and still stack; the stack then cuts them to fit. */
private const val MAX_STACK_OVERLAP = 0.08f
private const val MAX_ROW_GAP = 0.1f
private const val MIN_STACK_ROWS = 2
private const val PAIR_OVERLAP = 0.5f
private const val PREFERRED_NARROW = "4:3"
private const val PREFERRED_NARROW_ASPECT = 4f / 3f

/**
 * The theme's declared aspect ratio Jarn uses for narrow screens: 4:3 when declared, else the
 * landscape ratio closest to it below [THEME_NARROW_ASPECT]. Null when the theme has none.
 */
fun narrowAspectRatioOf(capabilities: EsdeThemeCapabilities): String? {
    val narrow = capabilities.aspectRatios.filter { aspectOf(it) in 1f..<THEME_NARROW_ASPECT }
    return narrow.firstOrNull { it == PREFERRED_NARROW } ?: narrow.minByOrNull { abs(aspectOf(it) - PREFERRED_NARROW_ASPECT) }
}

/**
 * Text, dates and ratings in rows that sit right under each other and share a column become one
 * stack, so an empty row (no rating, no developer) gives its room to the next instead of a hole.
 */
internal fun List<ThemeLayer>.stacked(view: EsdeView, report: EsdeReportBuilder): List<ThemeLayer> {
    val candidates = filter { it.isStackable() }.sortedBy { it.rect.top() }
    val stackOf = HashMap<String, String>()
    val heightCap = HashMap<String, Float>()
    var group = mutableListOf<ThemeLayer>()
    fun close() {
        val rows = group.map { it.rect.top() }.fold(mutableListOf<Float>()) { tops, top ->
            if (tops.none { abs(it - top) <= SAME_ROW }) tops += top
            tops
        }
        if (rows.size >= MIN_STACK_ROWS) {
            group.forEach { member ->
                stackOf[member.id] = "stack-${group.first().id}"
                rows.filter { it > member.rect.top() + SAME_ROW }.minOrNull()?.let { heightCap[member.id] = it - member.rect.top() }
            }
        }
        group = mutableListOf()
    }
    candidates.forEach { layer ->
        if (group.isNotEmpty() && !group.canTake(layer)) close()
        group += layer
    }
    close()
    if (stackOf.isNotEmpty()) report.adjusted(view, null, EsdeTranslationNote.StackedDetails)
    return map { layer -> stackOf[layer.id]?.let { layer.inStack(it, heightCap[layer.id]) } ?: layer }
}

private fun ThemeLayer.isStackable(): Boolean =
    this is ThemeLayer.Text || this is ThemeLayer.Date || this is ThemeLayer.Rating

private fun List<ThemeLayer>.canTake(layer: ThemeLayer): Boolean {
    val lastTop = maxOf { it.rect.top() }
    val lastRow = filter { abs(it.rect.top() - lastTop) <= SAME_ROW }
    val left = minOf { it.rect.left() }
    val right = maxOf { it.rect.left() + it.rect.width }
    val horizontalGap = max(left, layer.rect.left()) - min(right, layer.rect.left() + layer.rect.width)
    if (abs(layer.rect.top() - lastTop) <= SAME_ROW) return horizontalGap <= MAX_ROW_GAP
    if (horizontalGap >= 0f) return false
    val gap = layer.rect.top() - lastRow.maxOf { it.rect.top() + it.rect.height }
    return gap in -MAX_STACK_OVERLAP..MAX_STACK_GAP
}

/** Stack members sit at the top of their rect, so the room they don't use flows to the rows below. */
/**
 * Stack members sit at the top of their rect, so the room they don't use flows to the rows below.
 * A member that reaches into the next row is cut to end where it starts ([maxHeight]), with its
 * text stepping down to a role that still fits, so rows never draw over each other.
 */
private fun ThemeLayer.inStack(name: String, maxHeight: Float?): ThemeLayer {
    val rect = maxHeight?.takeIf { it < rect.height }?.let { rect.copy(y = rect.top() + rect.originY * it, height = it) } ?: rect
    return when (this) {
        is ThemeLayer.Text -> copy(rect = rect, stack = name, verticalAlign = ThemeAlign.Start, style = fittedRole(style, rect.height))
        is ThemeLayer.Date -> copy(rect = rect, stack = name, verticalAlign = ThemeAlign.Start, style = fittedRole(style, rect.height))
        is ThemeLayer.Rating -> copy(rect = rect, stack = name)
        else -> this
    }
}

/**
 * ES-DE themes often put a system's logo and its name in the same spot and rely on the missing
 * logo file to show the name. Jarn shows the logo when the system has one and the name otherwise.
 */
internal fun List<ThemeLayer>.logoOrName(view: EsdeView, report: EsdeReportBuilder): List<ThemeLayer> {
    val logos = filterIsInstance<ThemeLayer.Picture>().filter { it.source == ThemePicture.SystemArt(SystemArtKind.Logo) }
    val names = filterIsInstance<ThemeLayer.Text>().filter { it.isSystemName() }
    val pairs = logos.mapNotNull { logo -> names.firstOrNull { it.rect.overlapShare(logo.rect) >= PAIR_OVERLAP }?.let { logo.id to it.id } }
    if (pairs.isEmpty()) return this
    report.adjusted(view, null, EsdeTranslationNote.NameWhenNoLogo)
    val logoIds = pairs.map { it.first }.toSet()
    val nameIds = pairs.map { it.second }.toSet()
    return map { layer ->
        when (layer.id) {
            in logoIds -> (layer as ThemeLayer.Picture).copy(visibleWhen = layer.visibleWhen.withLogo(missing = false))
            in nameIds -> (layer as ThemeLayer.Text).copy(visibleWhen = layer.visibleWhen.withLogo(missing = true))
            else -> layer
        }
    }
}

private fun ThemeLayer.Text.isSystemName(): Boolean =
    content == ThemeText.System(SystemTextField.Title) || content == ThemeText.Game(GameTextField.SystemTitle)

private fun ThemeVisibleWhen?.withLogo(missing: Boolean): ThemeVisibleWhen =
    (this ?: ThemeVisibleWhen()).copy(hasSystemArt = SystemArtKind.Logo, missing = missing)

/** How much of the smaller rect the two share, 0 to 1. */
private fun ThemeRect.overlapShare(other: ThemeRect): Float {
    val width = min(left() + this.width, other.left() + other.width) - max(left(), other.left())
    val height = min(top() + this.height, other.top() + other.height) - max(top(), other.top())
    if (width <= 0f || height <= 0f) return 0f
    return width * height / min(this.width * this.height, other.width * other.height)
}

/**
 * The [narrow] translation as a patch on [wide]: changed primary settings, changed or new layers,
 * and hidden layers. Null when nothing differs. A narrow layout that swaps the list type keeps the
 * wide one and patches only the layers.
 */
internal fun narrowPatch(wide: ViewSpec, narrow: ViewSpec, view: EsdeView, report: EsdeReportBuilder): ThemeViewPatch? {
    val primary = primaryPatch(wide.primary, narrow.primary)
    if (wide.primary::class != narrow.primary::class) report.adjusted(view, null, EsdeTranslationNote.NarrowListTypeKept)
    val wideById = wide.layers.associateBy { it.id }
    val layers = narrow.layers.filter { wideById[it.id] != it }
    val narrowIds = narrow.layers.mapTo(HashSet()) { it.id }
    val hide = wide.layers.map { it.id }.filter { it !in narrowIds }
    if (primary == null && layers.isEmpty() && hide.isEmpty()) return null
    report.adjusted(view, null, EsdeTranslationNote.NarrowScreenLayout)
    return ThemeViewPatch(primary = primary, layers = layers, hide = hide)
}

private fun primaryPatch(wide: ThemePrimary, narrow: ThemePrimary): ThemePrimaryPatch? {
    fun <T> changed(a: T, b: T): T? = b.takeIf { it != a }
    val patch = when {
        wide is ThemePrimary.Shelf && narrow is ThemePrimary.Shelf -> ThemePrimaryPatch(
            box = changed(wide.box, narrow.box),
            itemsBefore = changed(wide.itemsBefore, narrow.itemsBefore),
            itemsAfter = changed(wide.itemsAfter, narrow.itemsAfter),
            focusedScale = changed(wide.focusedScale, narrow.focusedScale),
            showTitle = changed(wide.showTitle, narrow.showTitle),
        )
        wide is ThemePrimary.Grid && narrow is ThemePrimary.Grid -> ThemePrimaryPatch(
            box = changed(wide.box, narrow.box),
            columns = changed(wide.columns, narrow.columns),
            itemAspect = changed(wide.itemAspect, narrow.itemAspect),
            spacing = changed(wide.spacing, narrow.spacing),
            showTitle = changed(wide.showTitle, narrow.showTitle),
        )
        wide is ThemePrimary.List && narrow is ThemePrimary.List -> ThemePrimaryPatch(
            box = changed(wide.box, narrow.box),
            rows = changed(wide.rows, narrow.rows),
        )
        else -> ThemePrimaryPatch()
    }
    return patch.takeUnless { it == ThemePrimaryPatch() }
}

internal fun ThemeRect.left(): Float = x - originX * width
