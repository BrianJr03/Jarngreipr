package jr.brian.core.model.theme

import kotlinx.serialization.Serializable

/**
 * Changes a view makes on a narrow screen, such as the Thor's 4:3 bottom screen. [primary] overrides
 * single list settings, [layers] replace the layer with the same id or are added on top, and [hide]
 * drops layers by id. Optional and additive, so older builds simply ignore it.
 */
@Serializable
data class ThemeViewPatch(
    val primary: ThemePrimaryPatch? = null,
    @Serializable(with = ThemeLayerListSerializer::class)
    val layers: List<ThemeLayer> = emptyList(),
    val hide: List<String> = emptyList(),
)

/** Each set field replaces the primary's own; fields a list type doesn't have are ignored. */
@Serializable
data class ThemePrimaryPatch(
    val box: ThemeRect? = null,
    val itemsBefore: Int? = null,
    val itemsAfter: Int? = null,
    val focusedScale: Float? = null,
    val showTitle: Boolean? = null,
    val columns: Int? = null,
    val itemAspect: Float? = null,
    val spacing: Float? = null,
    val rows: Int? = null,
)

/**
 * Below this width over height of the area a view lays out on, its [ViewSpec.narrow] patch applies.
 * Browse loses its header and the hint bar, so a 16:9 screen's area is about 2.2 to 2.4 and a 4:3
 * screen's about 1.45 to 1.55; this splits the two with room to spare.
 */
const val THEME_NARROW_ASPECT = 1.8f

/** The view to lay out on a [width] by [height] area: this one, or with its narrow patch applied. */
fun ViewSpec.forArea(width: Int, height: Int): ViewSpec {
    val patch = narrow ?: return this
    if (height <= 0 || width.toFloat() / height >= THEME_NARROW_ASPECT) return copy(narrow = null)
    return ViewSpec(primary = primary.patched(patch.primary), layers = layers.patched(patch), narrow = null)
}

private fun ThemePrimary.patched(patch: ThemePrimaryPatch?): ThemePrimary {
    if (patch == null) return this
    return when (this) {
        is ThemePrimary.Shelf -> copy(
            box = patch.box ?: box,
            itemsBefore = patch.itemsBefore ?: itemsBefore,
            itemsAfter = patch.itemsAfter ?: itemsAfter,
            focusedScale = patch.focusedScale ?: focusedScale,
            showTitle = patch.showTitle ?: showTitle,
        )
        is ThemePrimary.Grid -> copy(
            box = patch.box ?: box,
            columns = patch.columns ?: columns,
            itemAspect = patch.itemAspect ?: itemAspect,
            spacing = patch.spacing ?: spacing,
            showTitle = patch.showTitle ?: showTitle,
        )
        is ThemePrimary.List -> copy(box = patch.box ?: box, rows = patch.rows ?: rows)
    }
}

private fun List<ThemeLayer>.patched(patch: ThemeViewPatch): List<ThemeLayer> {
    val replacements = patch.layers.associateBy { it.id }
    val kept = filter { it.id !in patch.hide }.map { replacements[it.id] ?: it }
    val existing = mapTo(HashSet()) { it.id }
    return kept + patch.layers.filter { it.id !in existing && it.id !in patch.hide }
}
