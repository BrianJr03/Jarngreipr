package jr.brian.jarnfe.editor

import androidx.annotation.StringRes
import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.SystemTextField
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeColorRole
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemeDateFormat
import jr.brian.core.model.theme.ThemeEdge
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.jarnfe.R

/** The four text sizes the editor offers, each one of Jarn's type roles. */
internal enum class ThemeEditorTextSize(val role: ThemeTextRole, @param:StringRes val labelRes: Int) {
    S(ThemeTextRole.Caption, R.string.jarnfe_editor_size_s),
    M(ThemeTextRole.Body, R.string.jarnfe_editor_size_m),
    L(ThemeTextRole.Title, R.string.jarnfe_editor_size_l),
    XL(ThemeTextRole.Display, R.string.jarnfe_editor_size_xl),
}

/** The nearest editor size for any role, so imported themes show a size too. */
internal val ThemeTextRole.editorSize: ThemeEditorTextSize
    get() = when (this) {
        ThemeTextRole.Caption, ThemeTextRole.CaptionStrong -> ThemeEditorTextSize.S
        ThemeTextRole.Body, ThemeTextRole.Label, ThemeTextRole.LabelStrong -> ThemeEditorTextSize.M
        ThemeTextRole.Heading, ThemeTextRole.Title -> ThemeEditorTextSize.L
        ThemeTextRole.Display -> ThemeEditorTextSize.XL
    }

/** What a text layer can show, in the order A steps through them. Fixed text stays as written. */
internal val TextChoices: List<ThemeText> =
    GameTextField.entries.map(ThemeText::Game) + SystemTextField.entries.map(ThemeText::System)

/** Pictures a layer can show, in the order A steps through them. */
internal val PictureChoices: List<ThemePicture> =
    MediaKind.entries.filter { it != MediaKind.Video }.map(ThemePicture::GameMedia) +
        SystemArtKind.entries.map(ThemePicture::SystemArt)

@get:StringRes
internal val ThemeText.labelRes: Int
    get() = when (this) {
        is ThemeText.Static -> R.string.jarnfe_editor_fixed_text
        is ThemeText.Game -> field.labelRes
        is ThemeText.System -> field.labelRes
    }

@get:StringRes
private val GameTextField.labelRes: Int
    get() = when (this) {
        GameTextField.Title -> R.string.jarnfe_editor_game_title
        GameTextField.SystemTitle -> R.string.jarnfe_editor_game_system
        GameTextField.Description -> R.string.jarnfe_editor_game_description
        GameTextField.Developer -> R.string.jarnfe_library_meta_developer
        GameTextField.Publisher -> R.string.jarnfe_library_meta_publisher
        GameTextField.Genre -> R.string.jarnfe_library_meta_genre
        GameTextField.Players -> R.string.jarnfe_library_meta_players
        GameTextField.PlayCount -> R.string.jarnfe_editor_game_play_count
        GameTextField.Playtime -> R.string.jarnfe_editor_game_playtime
    }

@get:StringRes
private val SystemTextField.labelRes: Int
    get() = when (this) {
        SystemTextField.Title -> R.string.jarnfe_editor_system_name
        SystemTextField.GameCount -> R.string.jarnfe_editor_system_games
        SystemTextField.FavoriteCount -> R.string.jarnfe_editor_system_favorites
        SystemTextField.Playtime -> R.string.jarnfe_editor_system_playtime
    }

@get:StringRes
internal val ThemePicture?.labelRes: Int
    get() = when (this) {
        null -> R.string.jarnfe_editor_fallback_none
        is ThemePicture.File -> R.string.jarnfe_editor_theme_file
        is ThemePicture.SystemArt -> when (kind) {
            SystemArtKind.Art -> R.string.jarnfe_system_art_title
            SystemArtKind.Logo -> R.string.jarnfe_editor_system_logo
            SystemArtKind.Background -> R.string.jarnfe_settings_system_background
        }
        is ThemePicture.GameMedia -> kind.labelRes
    }

@get:StringRes
private val MediaKind.labelRes: Int
    get() = when (this) {
        MediaKind.Cover -> R.string.jarnfe_game_art_cover
        MediaKind.Marquee -> R.string.jarnfe_game_art_marquee
        MediaKind.Fanart -> R.string.jarnfe_game_art_fanart
        MediaKind.Screenshot -> R.string.jarnfe_game_art_screenshot
        MediaKind.Titlescreen -> R.string.jarnfe_game_art_titlescreen
        MediaKind.Miximage -> R.string.jarnfe_game_art_mixed
        MediaKind.PhysicalMedia -> R.string.jarnfe_game_art_physical
        MediaKind.Video -> R.string.jarnfe_rss_badge_video
    }

@get:StringRes
internal val ThemeColorRole.labelRes: Int
    get() = when (this) {
        ThemeColorRole.Bg -> R.string.jarnfe_editor_color_bg
        ThemeColorRole.Surface -> R.string.jarnfe_editor_color_surface
        ThemeColorRole.Raised -> R.string.jarnfe_editor_color_raised
        ThemeColorRole.Line -> R.string.jarnfe_editor_color_line
        ThemeColorRole.TextHigh -> R.string.jarnfe_editor_color_text_high
        ThemeColorRole.TextMid -> R.string.jarnfe_editor_color_text_mid
        ThemeColorRole.TextLow -> R.string.jarnfe_editor_color_text_low
        ThemeColorRole.Accent -> R.string.jarnfe_editor_color_accent
        ThemeColorRole.Danger -> R.string.jarnfe_editor_color_danger
        ThemeColorRole.Success -> R.string.jarnfe_editor_color_success
    }

@get:StringRes
internal val ThemeAlign.labelRes: Int
    get() = when (this) {
        ThemeAlign.Start -> R.string.jarnfe_editor_align_start
        ThemeAlign.Center -> R.string.jarnfe_editor_align_center
        ThemeAlign.End -> R.string.jarnfe_editor_align_end
    }

@get:StringRes
internal val ThemeFit.labelRes: Int
    get() = when (this) {
        ThemeFit.Fit -> R.string.jarnfe_editor_fit_fit
        ThemeFit.Crop -> R.string.jarnfe_editor_fit_crop
        ThemeFit.Fill -> R.string.jarnfe_editor_fit_fill
    }

@get:StringRes
internal val ThemeCorner.labelRes: Int
    get() = when (this) {
        ThemeCorner.None -> R.string.jarnfe_editor_corner_none
        ThemeCorner.Sm -> R.string.jarnfe_editor_corner_sm
        ThemeCorner.Md -> R.string.jarnfe_editor_corner_md
        ThemeCorner.Lg -> R.string.jarnfe_editor_corner_lg
        ThemeCorner.Xl -> R.string.jarnfe_editor_corner_xl
    }

@get:StringRes
internal val ThemeEdge?.labelRes: Int
    get() = when (this) {
        null -> R.string.jarnfe_editor_edge_none
        ThemeEdge.Top -> R.string.jarnfe_editor_edge_top
        ThemeEdge.Bottom -> R.string.jarnfe_editor_edge_bottom
        ThemeEdge.Start -> R.string.jarnfe_editor_edge_start
        ThemeEdge.End -> R.string.jarnfe_editor_edge_end
    }

@get:StringRes
internal val ThemeDateField.labelRes: Int
    get() = when (this) {
        ThemeDateField.ReleaseDate -> R.string.jarnfe_editor_date_release
        ThemeDateField.LastPlayed -> R.string.jarnfe_library_meta_last_played
    }

@get:StringRes
internal val ThemeDateFormat.labelRes: Int
    get() = when (this) {
        ThemeDateFormat.Year -> R.string.jarnfe_editor_date_year
        ThemeDateFormat.Short -> R.string.jarnfe_editor_date_short
        ThemeDateFormat.Relative -> R.string.jarnfe_editor_date_relative
    }

@get:StringRes
internal val ThemePrimary.typeRes: Int
    get() = when (this) {
        is ThemePrimary.Shelf -> R.string.jarnfe_editor_list_shelf
        is ThemePrimary.Grid -> R.string.jarnfe_browse_view_grid
        is ThemePrimary.List -> R.string.jarnfe_editor_list_list
    }

/** The next item after [current] in this list, wrapping, or the first when [current] isn't in it. */
internal fun <T> List<T>.after(current: T): T = this[(indexOf(current) + 1).mod(size)]
