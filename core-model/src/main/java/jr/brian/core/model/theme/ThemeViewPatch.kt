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

/** This view as a narrow screen shows it: the patch applied, or the view as written without one. */
fun ViewSpec.narrowView(): ViewSpec = forArea(width = 1, height = 1)

/**
 * The view whose narrow layout is [edited], keeping this view's wide layout. Layers that differ
 * from the wide ones become replacements, new ones are added, and missing ones are hidden. List
 * settings the patch can hold go into it; the rest (its type, art, axis, alignment and second
 * line) apply to both screens, since the format keeps one of each.
 */
fun ViewSpec.withNarrowEdit(edited: ViewSpec): ViewSpec {
    val (widePrimary, primaryPatch) = splitPrimary(primary, edited.primary)
    val wide = layers.associateBy { it.id }
    val shown = edited.layers.mapTo(HashSet()) { it.id }
    val patch = ThemeViewPatch(
        primary = primaryPatch,
        layers = edited.layers.filter { wide[it.id] != it },
        hide = layers.map { it.id }.filter { it !in shown },
    )
    return copy(primary = widePrimary, narrow = patch.takeUnless { it == ThemeViewPatch() })
}

/** The view with the narrow entries for layer [id] dropped, so it shows as on the wide screen. */
fun ViewSpec.withoutNarrowLayer(id: String): ViewSpec {
    val patch = narrow ?: return this
    val next = patch.copy(layers = patch.layers.filterNot { it.id == id }, hide = patch.hide - id)
    return copy(narrow = next.takeUnless { it == ThemeViewPatch() })
}

/** The view with its narrow list settings dropped. */
fun ViewSpec.withoutNarrowPrimary(): ViewSpec {
    val patch = narrow ?: return this
    return copy(narrow = patch.copy(primary = null).takeUnless { it == ThemeViewPatch() })
}

/** Whether the narrow screen shows layer [id] differently, or not at all. */
fun ViewSpec.narrowChanges(id: String): Boolean =
    narrow?.let { patch -> id in patch.hide || patch.layers.any { it.id == id } } == true

/** The wide primary with [edited]'s shared settings, and the patch for what may differ by screen. */
private fun splitPrimary(wide: ThemePrimary, edited: ThemePrimary): Pair<ThemePrimary, ThemePrimaryPatch?> {
    val box = edited.box.takeIf { it != wide.box }
    val (shared, patch) = when {
        edited is ThemePrimary.Shelf && wide is ThemePrimary.Shelf -> edited.copy(
            box = wide.box,
            itemsBefore = wide.itemsBefore,
            itemsAfter = wide.itemsAfter,
            focusedScale = wide.focusedScale,
            showTitle = wide.showTitle,
        ) to ThemePrimaryPatch(
            box = box,
            itemsBefore = edited.itemsBefore.takeIf { it != wide.itemsBefore },
            itemsAfter = edited.itemsAfter.takeIf { it != wide.itemsAfter },
            focusedScale = edited.focusedScale.takeIf { it != wide.focusedScale },
            showTitle = edited.showTitle.takeIf { it != wide.showTitle },
        )
        edited is ThemePrimary.Grid && wide is ThemePrimary.Grid -> edited.copy(
            box = wide.box,
            columns = wide.columns,
            itemAspect = wide.itemAspect,
            spacing = wide.spacing,
            showTitle = wide.showTitle,
        ) to ThemePrimaryPatch(
            box = box,
            columns = edited.columns.takeIf { it != wide.columns },
            itemAspect = edited.itemAspect.takeIf { it != wide.itemAspect },
            spacing = edited.spacing.takeIf { it != wide.spacing },
            showTitle = edited.showTitle.takeIf { it != wide.showTitle },
        )
        edited is ThemePrimary.List && wide is ThemePrimary.List -> edited.copy(box = wide.box, rows = wide.rows) to
            ThemePrimaryPatch(box = box, rows = edited.rows.takeIf { it != wide.rows })
        else -> edited.withSameBox(wide.box) to ThemePrimaryPatch(box = box)
    }
    return shared to patch.takeUnless { it == ThemePrimaryPatch() }
}

private fun ThemePrimary.withSameBox(box: ThemeRect): ThemePrimary = when (this) {
    is ThemePrimary.Shelf -> copy(box = box)
    is ThemePrimary.Grid -> copy(box = box)
    is ThemePrimary.List -> copy(box = box)
}
