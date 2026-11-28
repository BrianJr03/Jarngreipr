package jr.brian.jarnfe.editor

import androidx.compose.runtime.Immutable
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.model.theme.narrowView
import jr.brian.jarnfe.library.LibraryBuiltInTheme
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.JarnThemeKind

/** The tools tabs, in the order L2 and R2 step through them. */
internal enum class ThemeEditorTab { Layers, Inspect, Colors, Preview }

/** What the editor has selected: the view's list (the navigator) or one decor layer. */
@Immutable
internal sealed interface ThemeEditorTarget {
    data object Navigator : ThemeEditorTarget

    data class Layer(val id: String) : ThemeEditorTarget
}

/** Which side L1 and R1 resize while a piece is held. Select flips it. */
internal enum class ThemeEditorAxis { Width, Height }

/** A picked up piece. [before] is the draft as it was, for B to put it back. */
@Immutable
internal data class ThemeEditorHold(val before: JarnThemeSpec)

/**
 * One editing session. [spec] is the live draft, shown on the canvas as it changes. [sheetFocused]
 * is the single screen case, where X moves control between the canvas and the tools sheet.
 * [narrow] shows the canvas at the bottom screen's shape and edits that screen's layout: every
 * change then lands in the view's narrow patch (see [withViewSpec]), and the wide layout stays.
 */
@Immutable
internal data class ThemeEditorState(
    val themeId: String,
    val spec: JarnThemeSpec,
    val view: ThemeView = ThemeView.Systems,
    val selected: ThemeEditorTarget = ThemeEditorTarget.Navigator,
    val tab: ThemeEditorTab = ThemeEditorTab.Layers,
    val sheetFocused: Boolean = false,
    val held: ThemeEditorHold? = null,
    val axis: ThemeEditorAxis = ThemeEditorAxis.Width,
    val snap: Boolean = true,
    val narrow: Boolean = false,
) {
    /** The view as the canvas shows it: the narrow layout while [narrow] is on. */
    val viewSpec: ViewSpec? get() = wideViewSpec?.let { if (narrow) it.narrowView() else it }

    /** The view as written, its narrow patch included. */
    val wideViewSpec: ViewSpec? get() = spec.views[view]

    /** The navigator first, then the layers bottom to top. */
    val targets: List<ThemeEditorTarget>
        get() = listOf(ThemeEditorTarget.Navigator) +
            viewSpec?.layers.orEmpty().sortedBy { it.z }.map { ThemeEditorTarget.Layer(it.id) }

    fun layer(id: String): ThemeLayer? = viewSpec?.layers?.firstOrNull { it.id == id }

    /** The draft as a theme the browse screens can draw. */
    val entry: JarnThemeEntry
        get() = JarnThemeEntry(
            id = themeId,
            kind = JarnThemeKind.User,
            theme = LibraryBuiltInTheme(spec),
            ownColors = !spec.meta.followsLook,
        )

    fun withTab(step: Int): ThemeEditorState {
        val tabs = ThemeEditorTab.entries
        return copy(tab = tabs[(tabs.indexOf(tab) + step).mod(tabs.size)])
    }

    /** Turns the narrow canvas on or off, letting go of a piece the other layout doesn't have. */
    fun withNarrowCanvas(on: Boolean): ThemeEditorState {
        val next = copy(narrow = on)
        return if (next.selected in next.targets) next else next.copy(selected = ThemeEditorTarget.Navigator)
    }

    fun withView(next: ThemeView): ThemeEditorState =
        copy(view = next, selected = ThemeEditorTarget.Navigator)

    fun withSelectionStep(step: Int): ThemeEditorState {
        val all = targets
        val index = all.indexOf(selected).coerceAtLeast(0)
        return copy(selected = all[(index + step).mod(all.size)])
    }
}

/** Both views exist from the start, so either can be edited. A missing one copies [fallback]'s. */
internal fun JarnThemeSpec.withBothViews(fallback: JarnThemeSpec): JarnThemeSpec = copy(
    views = ThemeViews(
        systems = views.systems ?: fallback.views.systems,
        games = views.games ?: fallback.views.games,
    ),
)
