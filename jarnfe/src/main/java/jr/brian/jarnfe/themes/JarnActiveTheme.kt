package jr.brian.jarnfe.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeView
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.look.jarnLookStoreOrNull

/** The theme a player has picked, as the picker shows it. */
@Immutable
internal sealed interface JarnThemeChoice {
    /** The built-ins, per view. Equal views read as that one built-in, different ones as a mix. */
    data class BuiltIn(val systems: JarnBrowseView, val games: JarnBrowseView) : JarnThemeChoice {
        val mixed: Boolean get() = systems != games
    }

    data class Installed(val entry: JarnThemeEntry) : JarnThemeChoice
}

/** The theme each browse view draws. Both are the same entry unless the built-ins are mixed. */
@Immutable
internal data class JarnActiveTheme(val systems: JarnThemeEntry, val games: JarnThemeEntry) {
    fun forView(view: ThemeView): JarnThemeEntry = when (view) {
        ThemeView.Systems -> systems
        ThemeView.Games -> games
    }

    /** Colors that replace the Look's, or null while the theme follows the Look. */
    val ownColors: ThemeColors?
        get() = listOf(systems, games).firstOrNull { it.ownColors }?.theme?.spec?.tokens?.colors
}

/**
 * The saved choice. A null or unknown [activeId] (a theme not on this device yet) reads as the
 * built-ins from the stored Grid or Carousel views, so existing players keep what they had.
 */
internal fun resolveJarnThemeChoice(
    activeId: String?,
    display: JarnDisplayPrefs,
    installed: List<JarnThemeEntry> = JarnThemeRegistry.installed,
): JarnThemeChoice = installed.firstOrNull { it.id == activeId }?.let(JarnThemeChoice::Installed)
    ?: JarnThemeChoice.BuiltIn(display.systemView, display.gameView)

/** A view the chosen theme leaves out keeps the built-in its stored view picks. */
internal fun JarnThemeChoice.activeTheme(display: JarnDisplayPrefs): JarnActiveTheme = when (this) {
    is JarnThemeChoice.BuiltIn -> JarnActiveTheme(
        systems = JarnThemeRegistry.builtInFor(systems),
        games = JarnThemeRegistry.builtInFor(games),
    )
    is JarnThemeChoice.Installed -> JarnActiveTheme(
        systems = entry.takeIf { it.viewSpec(ThemeView.Systems) != null } ?: JarnThemeRegistry.builtInFor(display.systemView),
        games = entry.takeIf { it.viewSpec(ThemeView.Games) != null } ?: JarnThemeRegistry.builtInFor(display.gameView),
    )
}

/**
 * Saves [choice]. A built-in clears the active id and sets both views; a theme with its own colors
 * turns off both Command Panel matches, since it replaces those colors.
 */
internal fun JarnLookStore.applyThemeChoice(choice: JarnThemeChoice) {
    when (choice) {
        is JarnThemeChoice.BuiltIn -> {
            setActiveThemeId(null)
            updateDisplay { it.copy(systemView = choice.systems, gameView = choice.games) }
        }
        is JarnThemeChoice.Installed -> {
            setActiveThemeId(choice.entry.id)
            if (choice.entry.ownColors) {
                setMatchCommandPanel(false)
                setMatchCommandPanelFonts(false)
            }
        }
    }
}

/** An unsaved theme shown on both screens while its card has focus. */
internal object JarnThemePreview {
    var choice by mutableStateOf<JarnThemeChoice?>(null)
}

@Composable
internal fun rememberSavedThemeChoice(): JarnThemeChoice {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val activeId = store?.activeThemeIdState?.collectAsState()?.value
    val display = store?.displayState?.collectAsState()?.value ?: JarnDisplayPrefs()
    return remember(activeId, display.systemView, display.gameView) { resolveJarnThemeChoice(activeId, display) }
}

/** The live preview wins, then the saved choice. */
@Composable
internal fun rememberJarnActiveTheme(): JarnActiveTheme {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val display = store?.displayState?.collectAsState()?.value ?: JarnDisplayPrefs()
    val saved = rememberSavedThemeChoice()
    val choice = JarnThemePreview.choice ?: saved
    return remember(choice, display.systemView, display.gameView) { choice.activeTheme(display) }
}
