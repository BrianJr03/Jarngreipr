package jr.brian.jarnfe.library

import androidx.annotation.StringRes
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemeViews
import jr.brian.jarnfe.R

/**
 * A layout template that ships in code. [id] is stable across releases, so a saved choice keeps
 * pointing at it, and [nameRes] is what the picker shows. Templates have no pins and no colors of
 * their own: they lay out purely from [spec] and draw with the player's Look.
 */
internal data class LibraryTemplateTheme(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val blurbRes: Int,
    val spec: JarnThemeSpec,
)

internal object LibraryTemplateThemes {
    val BigShelf = template(
        id = "template.big-shelf",
        nameRes = R.string.jarnfe_theme_template_big_shelf,
        blurbRes = R.string.jarnfe_theme_template_big_shelf_blurb,
        name = "Big shelf",
        views = ThemeViews(systems = BigShelfSystems, games = BigShelfGames),
    )

    val CoverGrid = template(
        id = "template.cover-grid",
        nameRes = R.string.jarnfe_theme_template_cover_grid,
        blurbRes = R.string.jarnfe_theme_template_cover_grid_blurb,
        name = "Cover grid",
        views = ThemeViews(systems = CoverGridSystems, games = CoverGridGames),
    )

    val ListWithArt = template(
        id = "template.list-art",
        nameRes = R.string.jarnfe_theme_template_list_art,
        blurbRes = R.string.jarnfe_theme_template_list_art_blurb,
        name = "List with art",
        views = ThemeViews(systems = ListArtSystems, games = ListArtGames),
    )

    val All: List<LibraryTemplateTheme> = listOf(BigShelf, CoverGrid, ListWithArt)

    fun byId(id: String): LibraryTemplateTheme? = All.firstOrNull { it.id == id }
}

/** [name] is the theme file's own name, for export; the picker reads [nameRes]. */
private fun template(
    id: String,
    @StringRes nameRes: Int,
    @StringRes blurbRes: Int,
    name: String,
    views: ThemeViews,
) = LibraryTemplateTheme(
    id = id,
    nameRes = nameRes,
    blurbRes = blurbRes,
    spec = JarnThemeSpec(meta = ThemeMeta(name = name), tokens = LibraryDefaultLookTokens, views = views),
)
