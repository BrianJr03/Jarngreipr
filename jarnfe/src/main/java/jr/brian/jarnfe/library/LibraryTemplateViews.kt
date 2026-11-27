package jr.brian.jarnfe.library

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.SystemTextField
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeColorRole
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.core.model.theme.ThemeVisibleWhen
import jr.brian.core.model.theme.ViewSpec

/*
 * The three layout templates from Talanah's #558 spec. Text rects are sized so one line of their
 * role, on its readable plate, fits the Thor's Library stage (805px tall at 369 dpi), and
 * everything stays above y = 0.90 so a full screen layout clears the hint bar too. Systems views
 * keep the top LIBRARY_HEADER_BAND clear for the Continue playing strip, so nothing below starts
 * above SYSTEMS_TOP.
 */

private const val SYSTEMS_TOP = LIBRARY_HEADER_BAND + 0.01f

private val BigShelfSystemsBox = ThemeRect(0.04f, 0.44f, 0.92f, 0.46f)
private val BigShelfSystemsPlate = ThemeRect(0f, 0.42f, 1f, 0.58f)
private val BigShelfGamesBox = ThemeRect(0.04f, 0.30f, 0.92f, 0.60f)
private val BigShelfGamesPlate = ThemeRect(0f, 0.28f, 1f, 0.72f)
private val CoverGridGamesBox = ThemeRect(0.03f, 0.06f, 0.62f, 0.84f)
private val CoverGridGamesRail = ThemeRect(0.67f, 0.06f, 0.30f, 0.84f)
private val CoverGridSystemsBox = ThemeRect(0.03f, SYSTEMS_TOP, 0.62f, 0.90f - SYSTEMS_TOP)
private val CoverGridSystemsRail = ThemeRect(0.67f, SYSTEMS_TOP, 0.30f, 0.90f - SYSTEMS_TOP)
private val ListArtGamesBox = ThemeRect(0.04f, 0.08f, 0.40f, 0.80f)
private val ListArtGamesPlate = ThemeRect(0.02f, 0.05f, 0.44f, 0.86f)
private val ListArtSystemsBox = ThemeRect(0.04f, SYSTEMS_TOP + 0.02f, 0.40f, 0.89f - SYSTEMS_TOP - 0.02f)
private val ListArtSystemsPlate = ThemeRect(0.02f, SYSTEMS_TOP, 0.44f, 0.91f - SYSTEMS_TOP)
private val FullScreen = ThemeRect(0f, 0f, 1f, 1f)

private const val BIG_SHELF_PLATE_OPACITY = 0.7f
private const val COVER_RAIL_OPACITY = 0.92f
private const val LIST_PLATE_OPACITY = 0.85f
private const val LIST_GAME_BACKDROP_OPACITY = 0.35f
private const val LIST_SYSTEM_BACKDROP_OPACITY = 0.5f
private const val BIG_SHELF_VIDEO_DELAY_MS = 800

/** Rows that fit the Thor stage at two lines each; a taller screen gives each row more room. */
private const val LIST_GAME_ROWS = 5
private const val LIST_SYSTEM_ROWS = 4

internal val BigShelfSystems = ViewSpec(
    primary = ThemePrimary.Shelf(box = BigShelfSystemsBox, focusedScale = 1.35f, showTitle = false),
    layers = listOf(
        picture("background", FullScreen, z = -3, SystemArtKind.Background, ThemeFit.Crop),
        plate("plate", BigShelfSystemsPlate, ThemeColorRole.Bg, BIG_SHELF_PLATE_OPACITY, ThemeCorner.None),
        picture("logo", ThemeRect(0.62f, SYSTEMS_TOP, 0.33f, 0.20f), z = 1, SystemArtKind.Logo, ThemeFit.Fit),
        text("title", ThemeRect(0.05f, SYSTEMS_TOP, 0.55f, 0.125f), systemText(SystemTextField.Title), ThemeTextRole.Title),
        text(
            "count",
            ThemeRect(0.05f, 0.34f, 0.40f, 0.075f),
            systemText(SystemTextField.GameCount),
            ThemeTextRole.Label,
            ThemeColorRole.TextMid,
        ),
    ),
)

internal val BigShelfGames = ViewSpec(
    primary = ThemePrimary.Shelf(
        box = BigShelfGamesBox,
        art = ThemePicture.GameMedia(MediaKind.Cover),
        focusedScale = 1.3f,
        showTitle = false,
    ),
    layers = listOf(
        ThemeLayer.Video(
            id = "video",
            rect = ThemeRect(0f, 0f, 1f, 0.40f),
            z = -3,
            delayMs = BIG_SHELF_VIDEO_DELAY_MS,
            fallback = ThemePicture.GameMedia(MediaKind.Fanart),
        ),
        plate("plate", BigShelfGamesPlate,ThemeColorRole.Bg, BIG_SHELF_PLATE_OPACITY, ThemeCorner.None),
        text("title", ThemeRect(0.05f, 0.06f, 0.60f, 0.125f), gameText(GameTextField.Title), ThemeTextRole.Title),
        ThemeLayer.Rating(id = "rating", rect = ThemeRect(0.05f, 0.19f, 0.14f, 0.075f), z = 2),
        year("year", ThemeRect(0.20f, 0.19f, 0.10f, 0.075f)),
        text(
            "genre",
            ThemeRect(0.31f, 0.19f, 0.30f, 0.075f),
            gameText(GameTextField.Genre),
            ThemeTextRole.Label,
            ThemeColorRole.TextMid,
        ),
    ),
)

internal val CoverGridGames = ViewSpec(
    primary = ThemePrimary.Grid(
        box = CoverGridGamesBox,
        art = ThemePicture.GameMedia(MediaKind.Cover),
        columns = 4,
        itemAspect = 0.70f,
        spacing = 0.012f,
        showTitle = false,
    ),
    layers = listOf(
        plate("rail", CoverGridGamesRail, ThemeColorRole.Surface, COVER_RAIL_OPACITY, ThemeCorner.Lg),
        ThemeLayer.Video(
            id = "video",
            rect = ThemeRect(0.685f, 0.08f, 0.27f, 0.22f),
            z = 1,
            fallback = ThemePicture.GameMedia(MediaKind.Fanart),
            corner = ThemeCorner.Md,
        ),
        text(
            "title",
            ThemeRect(0.685f, 0.31f, 0.27f, 0.175f),
            gameText(GameTextField.Title),
            ThemeTextRole.Heading,
            maxLines = 2,
        ),
        ThemeLayer.Rating(id = "rating", rect = ThemeRect(0.685f, 0.49f, 0.13f, 0.075f), z = 2),
        year("year", ThemeRect(0.83f, 0.49f, 0.12f, 0.075f), ThemeAlign.End),
        text(
            "playtime",
            ThemeRect(0.685f, 0.57f, 0.27f, 0.075f),
            gameText(GameTextField.Playtime),
            ThemeTextRole.Label,
            ThemeColorRole.TextMid,
        ),
        text(
            "description",
            ThemeRect(0.685f, 0.655f, 0.27f, 0.235f),
            gameText(GameTextField.Description),
            ThemeTextRole.Body,
            ThemeColorRole.TextMid,
            maxLines = 3,
        ),
    ),
)

internal val CoverGridSystems = ViewSpec(
    primary = ThemePrimary.Grid(box = CoverGridSystemsBox, columns = 3, itemAspect = 1.33f, showTitle = true),
    layers = listOf(
        plate("rail", CoverGridSystemsRail, ThemeColorRole.Surface, COVER_RAIL_OPACITY, ThemeCorner.Lg),
        picture(
            "art",
            ThemeRect(0.685f, SYSTEMS_TOP + 0.02f, 0.27f, 0.20f),
            z = 1,
            SystemArtKind.Background,
            ThemeFit.Crop,
            ThemeCorner.Md,
        ),
        text(
            "title",
            ThemeRect(0.685f, 0.44f, 0.27f, 0.175f),
            systemText(SystemTextField.Title),
            ThemeTextRole.Heading,
            maxLines = 2,
        ),
        text(
            "count",
            ThemeRect(0.685f, 0.62f, 0.27f, 0.075f),
            systemText(SystemTextField.GameCount),
            ThemeTextRole.Label,
            ThemeColorRole.TextMid,
        ),
        text(
            "playtime",
            ThemeRect(0.685f, 0.70f, 0.27f, 0.07f),
            systemText(SystemTextField.Playtime),
            ThemeTextRole.Caption,
            ThemeColorRole.TextLow,
        ),
    ),
)

internal val ListArtGames = ViewSpec(
    primary = ThemePrimary.List(box = ListArtGamesBox, rows = LIST_GAME_ROWS, secondary = gameText(GameTextField.Genre)),
    layers = listOf(
        ThemeLayer.Picture(
            id = "backdrop",
            rect = FullScreen,
            z = -3,
            opacity = LIST_GAME_BACKDROP_OPACITY,
            source = ThemePicture.GameMedia(MediaKind.Fanart),
            fit = ThemeFit.Crop,
        ),
        plate("plate", ListArtGamesPlate, ThemeColorRole.Bg, LIST_PLATE_OPACITY, ThemeCorner.Lg),
        ThemeLayer.Picture(
            id = "miximage",
            rect = ThemeRect(0.50f, 0.08f, 0.46f, 0.38f),
            z = 1,
            visibleWhen = ThemeVisibleWhen(hasMedia = MediaKind.Miximage),
            source = ThemePicture.GameMedia(MediaKind.Miximage),
        ),
        text(
            "title",
            ThemeRect(0.50f, 0.47f, 0.46f, 0.175f),
            gameText(GameTextField.Title),
            ThemeTextRole.Heading,
            maxLines = 2,
        ),
        ThemeLayer.Rating(id = "rating", rect = ThemeRect(0.50f, 0.65f, 0.14f, 0.06f), z = 2),
        text(
            "description",
            ThemeRect(0.50f, 0.72f, 0.46f, 0.18f),
            gameText(GameTextField.Description),
            ThemeTextRole.Body,
            ThemeColorRole.TextMid,
            maxLines = 2,
        ),
    ),
)

internal val ListArtSystems = ViewSpec(
    primary = ThemePrimary.List(box = ListArtSystemsBox, rows = LIST_SYSTEM_ROWS, secondary = systemText(SystemTextField.GameCount)),
    layers = listOf(
        ThemeLayer.Picture(
            id = "backdrop",
            rect = FullScreen,
            z = -3,
            opacity = LIST_SYSTEM_BACKDROP_OPACITY,
            source = ThemePicture.SystemArt(SystemArtKind.Background),
            fit = ThemeFit.Crop,
        ),
        plate("plate", ListArtSystemsPlate, ThemeColorRole.Bg, LIST_PLATE_OPACITY, ThemeCorner.Lg),
        picture("logo", ThemeRect(0.52f, SYSTEMS_TOP + 0.02f, 0.42f, 0.24f), z = 1, SystemArtKind.Logo, ThemeFit.Fit),
        text(
            "favorites",
            ThemeRect(0.52f, 0.50f, 0.42f, 0.075f),
            systemText(SystemTextField.FavoriteCount),
            ThemeTextRole.Label,
            ThemeColorRole.TextMid,
        ),
        text(
            "playtime",
            ThemeRect(0.52f, 0.59f, 0.42f, 0.075f),
            systemText(SystemTextField.Playtime),
            ThemeTextRole.Label,
            ThemeColorRole.TextLow,
        ),
    ),
)

private fun gameText(field: GameTextField) = ThemeText.Game(field)

private fun systemText(field: SystemTextField) = ThemeText.System(field)

private fun picture(
    id: String,
    rect: ThemeRect,
    z: Int,
    kind: SystemArtKind,
    fit: ThemeFit,
    corner: ThemeCorner = ThemeCorner.None,
) = ThemeLayer.Picture(id = id, rect = rect, z = z, source = ThemePicture.SystemArt(kind), fit = fit, corner = corner)

private fun plate(id: String, rect: ThemeRect, color: ThemeColorRole, opacity: Float, corner: ThemeCorner) =
    ThemeLayer.Panel(id = id, rect = rect, z = -1, opacity = opacity, color = color, corner = corner)

/** Text and dates sit above the primary at z 2, as the spec draws them. */
private fun text(
    id: String,
    rect: ThemeRect,
    content: ThemeText,
    style: ThemeTextRole,
    color: ThemeColorRole = ThemeColorRole.TextHigh,
    maxLines: Int = 1,
) = ThemeLayer.Text(id = id, rect = rect, z = 2, content = content, style = style, color = color, maxLines = maxLines)

private fun year(id: String, rect: ThemeRect, align: ThemeAlign = ThemeAlign.Start) =
    ThemeLayer.Date(id = id, rect = rect, z = 2, field = ThemeDateField.ReleaseDate, align = align)
