package jr.brian.jarnfe.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeTokens
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.frontend.look.JarnBrowseView

/** A theme that ships in code, with the [pins] that keep it identical to Jarn's own layout. */
internal data class LibraryBuiltInTheme(
    val spec: JarnThemeSpec,
    val pins: Map<ThemeView, LibraryPrimaryPins> = emptyMap(),
) {
    fun pinsFor(view: ThemeView): LibraryPrimaryPins = pins[view] ?: LibraryPrimaryPins()
}

/** The primary fills the browse area it is given, so fractions resolve to today's sizes. */
internal val LibraryFullBox = ThemeRect(x = 0f, y = 0f, width = 1f, height = 1f)

private const val GRID_THEME_NAME = "Grid"
private const val CAROUSEL_THEME_NAME = "Carousel"
private const val SYSTEM_ITEM_ASPECT = 16f / 10f
private const val GAME_ITEM_ASPECT = 3f / 4f

internal object LibraryBuiltInThemes {
    val Grid = LibraryBuiltInTheme(
        spec = builtInSpec(
            name = GRID_THEME_NAME,
            systems = ThemePrimary.Grid(box = LibraryFullBox, itemAspect = SYSTEM_ITEM_ASPECT),
            games = ThemePrimary.Grid(box = LibraryFullBox, itemAspect = GAME_ITEM_ASPECT),
        ),
        pins = ThemeView.entries.associateWith { LibraryPrimaryPins(grid = LibraryGridPins(LibraryGridRowSpacing)) },
    )

    val Carousel = LibraryBuiltInTheme(
        spec = builtInSpec(
            name = CAROUSEL_THEME_NAME,
            systems = shelfPrimary(LibraryShelfKind.Systems),
            games = shelfPrimary(LibraryShelfKind.Games),
        ),
        pins = LibraryShelfKind.entries.associate { it.themeView to LibraryPrimaryPins(shelf = it.geometry) },
    )

    fun forView(view: JarnBrowseView): LibraryBuiltInTheme = when (view) {
        JarnBrowseView.Grid -> Grid
        JarnBrowseView.Carousel -> Carousel
    }
}

/** One over the neighbor scale is not exact in floats, so the carousel pins the geometry too. */
private fun shelfPrimary(kind: LibraryShelfKind): ThemePrimary.Shelf {
    val geometry = kind.geometry
    return ThemePrimary.Shelf(
        box = LibraryFullBox,
        itemsBefore = geometry.neighborsBefore,
        itemsAfter = geometry.neighborsAfter,
        focusedScale = 1f / geometry.neighborScale,
    )
}

private fun builtInSpec(name: String, systems: ThemePrimary, games: ThemePrimary) = JarnThemeSpec(
    meta = ThemeMeta(name = name),
    tokens = ThemeTokens(colors = JarnLooks.Forge.colors.toThemeColors()),
    views = ThemeViews(systems = ViewSpec(systems), games = ViewSpec(games)),
)

private fun JarnColors.toThemeColors() = ThemeColors(
    bg = bg.toThemeColor(),
    surface = surface.toThemeColor(),
    raised = raised.toThemeColor(),
    textHigh = textHigh.toThemeColor(),
    textMid = textMid.toThemeColor(),
    textLow = textLow.toThemeColor(),
    accent = accent.toThemeColor(),
    danger = danger.toThemeColor(),
    success = success.toThemeColor(),
    line = line.toThemeColor(),
    focus = focus.toThemeColor(),
)

private fun Color.toThemeColor() = ThemeColor(toArgb())
