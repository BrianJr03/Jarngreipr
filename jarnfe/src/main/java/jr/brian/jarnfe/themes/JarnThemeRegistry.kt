package jr.brian.jarnfe.themes

import androidx.annotation.StringRes
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ViewSpec
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.LibraryBuiltInTheme
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import jr.brian.jarnfe.library.LibraryTemplateThemes
import java.io.File

internal enum class JarnThemeKind { BuiltIn, Template, Imported }

/** The picture a layout card draws. Imports set none and draw their systems list's shape. */
internal enum class JarnThemeSketchKind { Grid, Carousel, List, BigShelf, CoverGrid, ListWithArt }

/**
 * A theme the player can pick: a layout plus where its colors come from. With [ownColors] off the
 * spec's colors are ignored and the theme follows the chosen Look. [nameRes] wins over the spec's
 * name, and [themeDir] holds an imported theme's own files.
 */
internal data class JarnThemeEntry(
    val id: String,
    val kind: JarnThemeKind,
    val theme: LibraryBuiltInTheme,
    @param:StringRes val nameRes: Int? = null,
    @param:StringRes val blurbRes: Int? = null,
    val ownColors: Boolean = false,
    val themeDir: File? = null,
    val sketch: JarnThemeSketchKind? = null,
) {
    val sketchKind: JarnThemeSketchKind
        get() = sketch ?: when (viewSpec(ThemeView.Systems)?.primary) {
            is ThemePrimary.Shelf -> JarnThemeSketchKind.Carousel
            is ThemePrimary.List -> JarnThemeSketchKind.List
            is ThemePrimary.Grid, null -> JarnThemeSketchKind.Grid
        }

    fun viewSpec(view: ThemeView): ViewSpec? = theme.spec.views[view]

    /** The Jarn list closest to [view]'s primary, for previews and tile size settings. */
    fun browseView(view: ThemeView): JarnBrowseView =
        if (viewSpec(view)?.primary is ThemePrimary.Shelf) JarnBrowseView.Carousel else JarnBrowseView.Grid
}

/**
 * Every layout this build ships. A null active id is a player from before layouts: the stored
 * systems view's built-in is their layout, and a different games view is an override.
 */
private val TemplateSketches = mapOf(
    LibraryTemplateThemes.BigShelf.id to JarnThemeSketchKind.BigShelf,
    LibraryTemplateThemes.CoverGrid.id to JarnThemeSketchKind.CoverGrid,
    LibraryTemplateThemes.ListWithArt.id to JarnThemeSketchKind.ListWithArt,
)

internal object JarnThemeRegistry {
    val Grid = JarnThemeEntry(
        id = "builtin-grid",
        kind = JarnThemeKind.BuiltIn,
        theme = LibraryBuiltInThemes.Grid,
        nameRes = R.string.jarnfe_browse_view_grid,
        blurbRes = R.string.jarnfe_theme_grid_blurb,
    )

    val Carousel = JarnThemeEntry(
        id = "builtin-carousel",
        kind = JarnThemeKind.BuiltIn,
        theme = LibraryBuiltInThemes.Carousel,
        nameRes = R.string.jarnfe_browse_view_carousel,
        blurbRes = R.string.jarnfe_theme_carousel_blurb,
    )

    val builtIns: List<JarnThemeEntry> = listOf(Grid, Carousel)

    /**
     * Templates that ship in code (#558). Each is [JarnThemeKind.Template], follows the Look
     * (ownColors = false) and keeps a stable id forever, since the id is what backups save.
     */
    val templates: List<JarnThemeEntry> = LibraryTemplateThemes.All.map { template ->
        JarnThemeEntry(
            id = template.id,
            kind = JarnThemeKind.Template,
            theme = LibraryBuiltInTheme(template.spec),
            nameRes = template.nameRes,
            blurbRes = template.blurbRes,
            sketch = TemplateSketches[template.id],
        )
    }

    /** Layouts that can be saved as the active theme, imports last. */
    val installed: List<JarnThemeEntry> get() = builtIns + templates + JarnImportedThemes.entries

    fun builtInFor(view: JarnBrowseView): JarnThemeEntry = when (view) {
        JarnBrowseView.Grid, JarnBrowseView.SameAsLayout -> Grid
        JarnBrowseView.Carousel -> Carousel
    }
}
