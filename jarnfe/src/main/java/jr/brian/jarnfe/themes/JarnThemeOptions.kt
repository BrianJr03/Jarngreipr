package jr.brian.jarnfe.themes

import jr.brian.frontend.look.JarnBrowseView

/** One layout card. Picking it keeps the per-view overrides, which have their own rows. */
internal data class JarnThemeOption(val entry: JarnThemeEntry, val choice: JarnThemeChoice, val chosen: Boolean) {
    val key: String get() = entry.id
}

internal fun jarnThemeOptions(
    saved: JarnThemeChoice,
    installed: List<JarnThemeEntry> = JarnThemeRegistry.installed,
): List<JarnThemeOption> = installed.map { entry ->
    JarnThemeOption(entry, saved.copy(layout = entry), chosen = entry == saved.layout)
}

/** The override row's choices, in the order A steps through them. Null is the same as the layout. */
internal val JarnViewOverrides: List<JarnBrowseView?> = listOf(null, JarnBrowseView.Grid, JarnBrowseView.Carousel)

internal fun JarnBrowseView?.nextOverride(): JarnBrowseView? =
    JarnViewOverrides[(JarnViewOverrides.indexOf(this) + 1) % JarnViewOverrides.size]
