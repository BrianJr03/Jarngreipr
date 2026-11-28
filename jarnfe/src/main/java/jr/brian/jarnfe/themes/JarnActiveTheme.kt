package jr.brian.jarnfe.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeView
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.look.jarnLookStoreOrNull

/**
 * The player's layout plus a per-view override. A null override means the view is the same as
 * the layout; Grid or Carousel draws that built-in for the view instead.
 */
@Immutable
internal data class JarnThemeChoice(
    val layout: JarnThemeEntry,
    val systems: JarnBrowseView? = null,
    val games: JarnBrowseView? = null,
) {
    fun override(view: ThemeView): JarnBrowseView? = when (view) {
        ThemeView.Systems -> systems
        ThemeView.Games -> games
    }

    fun withOverride(view: ThemeView, override: JarnBrowseView?): JarnThemeChoice = when (view) {
        ThemeView.Systems -> copy(systems = override)
        ThemeView.Games -> copy(games = override)
    }
}

/** The theme each browse view draws. */
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
 * The saved choice. A saved layout reads both stored views as overrides. A null or unknown
 * [activeId] (from before layouts, or a theme not on this device yet) takes the stored systems
 * view's built-in as the layout and a different games view as its override, so existing players
 * keep exactly what they had.
 */
internal fun resolveJarnThemeChoice(
    activeId: String?,
    display: JarnDisplayPrefs,
    installed: List<JarnThemeEntry> = JarnThemeRegistry.installed,
): JarnThemeChoice {
    installed.firstOrNull { it.id == activeId }?.let { saved ->
        return JarnThemeChoice(saved, display.systemView.asOverride(), display.gameView.asOverride())
    }
    val layout = JarnThemeRegistry.builtInFor(display.systemView)
    val layoutView = layout.browseView(ThemeView.Games)
    return JarnThemeChoice(layout, games = display.gameView.asOverride()?.takeUnless { it == layoutView })
}

/** A view the layout leaves out, and isn't overridden, draws the grid. */
internal fun JarnThemeChoice.activeTheme(): JarnActiveTheme {
    fun entryFor(view: ThemeView): JarnThemeEntry = override(view)?.let(JarnThemeRegistry::builtInFor)
        ?: layout.takeIf { it.viewSpec(view) != null }
        ?: JarnThemeRegistry.Grid
    return JarnActiveTheme(entryFor(ThemeView.Systems), entryFor(ThemeView.Games))
}

private fun JarnBrowseView.asOverride(): JarnBrowseView? = takeUnless { it == JarnBrowseView.SameAsLayout }

/**
 * Saves [choice]: the layout id and both views, so a player from before layouts moves to the new
 * form on their first change. A layout with its own colors turns off both Command Panel matches,
 * since it replaces those colors.
 */
internal fun JarnLookStore.applyThemeChoice(choice: JarnThemeChoice) {
    setActiveThemeId(choice.layout.id)
    updateDisplay {
        it.copy(
            systemView = choice.systems ?: JarnBrowseView.SameAsLayout,
            gameView = choice.games ?: JarnBrowseView.SameAsLayout,
        )
    }
    if (choice.layout.ownColors) {
        setMatchCommandPanel(false)
        setMatchCommandPanelFonts(false)
    }
}

/**
 * An unsaved layout shown on both screens: a focused card, the editor's canvas or Try it. Each
 * caller shows its own and clears only its own, so one leaving (the canvas as Try it starts)
 * never wipes out the one that just took over. The newest still showing wins.
 */
internal object JarnThemePreview {
    private val shown = mutableStateListOf<Pair<Any, JarnThemeChoice>>()

    val choice: JarnThemeChoice? get() = shown.lastOrNull()?.second

    /** Shows [choice] for [owner], replacing what [owner] showed before; null clears it. */
    fun show(owner: Any, choice: JarnThemeChoice?) {
        val index = shown.indexOfFirst { it.first === owner }
        when {
            choice == null -> if (index >= 0) shown.removeAt(index)
            index >= 0 -> shown[index] = owner to choice
            else -> shown.add(owner to choice)
        }
    }

    fun clear(owner: Any) = show(owner, null)
}

/** Shows [choice] on both screens while this is in composition, or until [choice] is null. */
@Composable
internal fun ThemePreviewEffect(choice: JarnThemeChoice?) {
    val owner = remember { Any() }
    DisposableEffect(owner, choice) {
        JarnThemePreview.show(owner, choice)
        onDispose { JarnThemePreview.clear(owner) }
    }
}

@Composable
internal fun rememberSavedThemeChoice(): JarnThemeChoice {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val activeId = store?.activeThemeIdState?.collectAsState()?.value
    val display = store?.displayState?.collectAsState()?.value ?: JarnDisplayPrefs()
    LoadImportedThemes()
    val installed = JarnThemeRegistry.installed
    return remember(activeId, display.systemView, display.gameView, installed) {
        resolveJarnThemeChoice(activeId, display, installed)
    }
}

/** The live preview wins, then the saved choice. */
@Composable
internal fun rememberJarnActiveTheme(): JarnActiveTheme {
    val saved = rememberSavedThemeChoice()
    val choice = JarnThemePreview.choice ?: saved
    return remember(choice) { choice.activeTheme() }
}
