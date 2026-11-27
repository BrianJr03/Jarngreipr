package jr.brian.jarnfe.themes

import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs

/** One card in the theme picker. [key] is stable across recompositions, for focus tracking. */
internal data class JarnThemeOption(val key: String, val choice: JarnThemeChoice, val chosen: Boolean)

private const val MIXED_KEY = "builtin-mixed"

/**
 * The picker's cards: Grid, Carousel, then the stored mix when systems and games use different
 * built-ins (so a mix set before themes existed can be picked again), then [installed] themes.
 */
internal fun jarnThemeOptions(
    saved: JarnThemeChoice,
    display: JarnDisplayPrefs,
    installed: List<JarnThemeEntry> = JarnThemeRegistry.installed,
): List<JarnThemeOption> {
    val builtIns = JarnBrowseView.entries.map { view ->
        JarnThemeChoice.BuiltIn(view, view) to JarnThemeRegistry.builtInFor(view).id
    }
    val mix = JarnThemeChoice.BuiltIn(display.systemView, display.gameView)
        .takeIf { it.mixed }
        ?.let { listOf(it to MIXED_KEY) }
        .orEmpty()
    val themes = installed.map { JarnThemeChoice.Installed(it) to it.id }
    return (builtIns + mix + themes).map { (choice, key) -> JarnThemeOption(key, choice, choice == saved) }
}
