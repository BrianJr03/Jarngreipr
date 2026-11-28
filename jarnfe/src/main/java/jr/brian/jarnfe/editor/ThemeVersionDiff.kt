package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ViewSpec

/**
 * What changed between two versions of a theme, in counts a player can read: pieces added, removed
 * or changed across both screens, whether either list changed, how many pieces changed only on the
 * narrow (bottom) screen, and whether the colors did.
 */
internal data class ThemeVersionDiff(
    val added: Int,
    val removed: Int,
    val changed: Int,
    val listChanged: Boolean,
    val colorsChanged: Boolean,
    val narrowChanged: Int = 0,
) {
    val same: Boolean get() = added == 0 && removed == 0 && changed == 0 && !listChanged && !colorsChanged && narrowChanged == 0
}

/** How [now] differs from [then]. A piece counts once however many of its settings changed. */
internal fun themeVersionDiff(then: JarnThemeSpec, now: JarnThemeSpec): ThemeVersionDiff {
    var added = 0
    var removed = 0
    var changed = 0
    var listChanged = false
    var narrowChanged = 0
    ThemeView.entries.forEach { view ->
        val before = then.views[view]
        val after = now.views[view]
        val oldLayers = before?.layers.orEmpty().associateBy { it.id }
        val newLayers = after?.layers.orEmpty().associateBy { it.id }
        added += newLayers.keys.count { it !in oldLayers }
        removed += oldLayers.keys.count { it !in newLayers }
        changed += newLayers.count { (id, layer) -> oldLayers[id]?.let { it != layer } == true }
        if (before?.primary != after?.primary) listChanged = true
        narrowChanged += narrowEntries(before).let { old ->
            val new = narrowEntries(after)
            (old.keys + new.keys).count { old[it] != new[it] }
        }
    }
    val colorsChanged = then.meta.followsLook != now.meta.followsLook ||
        (!now.meta.followsLook && then.tokens.colors != now.tokens.colors)
    return ThemeVersionDiff(added, removed, changed, listChanged, colorsChanged, narrowChanged)
}

/** What the narrow patch says per piece: its replacement layer, hidden, or the list's own settings. */
private fun narrowEntries(view: ViewSpec?): Map<String, Any> {
    val patch = view?.narrow ?: return emptyMap()
    return buildMap {
        patch.primary?.let { put(NARROW_LIST_KEY, it) }
        patch.layers.forEach { put(NARROW_LAYER_PREFIX + it.id, it) }
        patch.hide.forEach { put(NARROW_LAYER_PREFIX + it, NARROW_HIDDEN) }
    }
}

private const val NARROW_LIST_KEY = "list"
private const val NARROW_LAYER_PREFIX = "layer:"
private const val NARROW_HIDDEN = "hidden"
