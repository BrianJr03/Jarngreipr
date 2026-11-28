package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeView

/**
 * What changed between two versions of a theme, in counts a player can read: pieces added, removed
 * or changed across both screens, whether either list changed, and whether the colors did.
 */
internal data class ThemeVersionDiff(
    val added: Int,
    val removed: Int,
    val changed: Int,
    val listChanged: Boolean,
    val colorsChanged: Boolean,
) {
    val same: Boolean get() = added == 0 && removed == 0 && changed == 0 && !listChanged && !colorsChanged
}

/** How [now] differs from [then]. A piece counts once however many of its settings changed. */
internal fun themeVersionDiff(then: JarnThemeSpec, now: JarnThemeSpec): ThemeVersionDiff {
    var added = 0
    var removed = 0
    var changed = 0
    var listChanged = false
    ThemeView.entries.forEach { view ->
        val before = then.views[view]
        val after = now.views[view]
        val oldLayers = before?.layers.orEmpty().associateBy { it.id }
        val newLayers = after?.layers.orEmpty().associateBy { it.id }
        added += newLayers.keys.count { it !in oldLayers }
        removed += oldLayers.keys.count { it !in newLayers }
        changed += newLayers.count { (id, layer) -> oldLayers[id]?.let { it != layer } == true }
        if (before?.primary != after?.primary || before?.narrow != after?.narrow) listChanged = true
    }
    val colorsChanged = then.meta.followsLook != now.meta.followsLook ||
        (!now.meta.followsLook && then.tokens.colors != now.tokens.colors)
    return ThemeVersionDiff(added, removed, changed, listChanged, colorsChanged)
}
