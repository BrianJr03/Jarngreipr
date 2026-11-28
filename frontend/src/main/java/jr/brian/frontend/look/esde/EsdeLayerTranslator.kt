package jr.brian.frontend.look.esde

import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.SystemTextField
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemeDateFormat
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.core.model.theme.ThemeVisibleWhen
import kotlin.math.abs
import kotlin.math.roundToInt

/** ES-DE's default zIndex per element type; primaries draw at 50. */
internal const val ESDE_PRIMARY_Z = 50f
private val DEFAULT_Z = mapOf("image" to 30f, "video" to 30f, "text" to 40f, "datetime" to 40f, "rating" to 45f)
private const val OTHER_DEFAULT_Z = 35f
private val LAYER_TYPES = setOf("image", "video", "text", "rating", "datetime")
private const val DEFAULT_FONT_SIZE = 0.045f
private const val DEFAULT_LINE_SPACING = 1.5f
private const val GUESSED_SIDE = 0.2f
private const val GUESSED_TEXT_WIDTH = 0.4f
private const val BACKGROUND_AREA = 0.6f
private const val SECONDS_TO_MS = 1000f
private const val LINE_FIT_SLACK = 0.01f

/** Translates one ES-DE secondary element into a Jarn layer, or reports why it can't. */
internal class EsdeLayerTranslator(
    private val view: EsdeView,
    private val primaryZ: Float,
    private val aspect: Float,
    private val report: EsdeReportBuilder,
) {

    fun translate(element: EsdeElementAcrossSystems): ThemeLayer? {
        if (element.key.type !in LAYER_TYPES) return notUsed(element, EsdeTranslationNote.UnsupportedElement)
        if (element.props.values.all { it.isBlank() }) return notUsed(element, EsdeTranslationNote.NothingReadable)
        if (element.bool("visible") == false) return notUsed(element, EsdeTranslationNote.Hidden)
        val layer = when (element.key.type) {
            "image" -> image(element)
            "video" -> video(element)
            "text" -> text(element)
            "rating" -> rating(element)
            else -> date(element)
        } ?: return null
        report.collectColors(view, element)
        if (!element.onEverySystem) report.adjusted(view, element.key, EsdeTranslationNote.ShownOnSomeSystems)
        report.translated(view, element.key)
        return layer
    }

    private fun notUsed(element: EsdeElementAcrossSystems, note: EsdeTranslationNote): ThemeLayer? {
        report.notUsed(view, element.key, note)
        return null
    }

    private fun EsdeElementAcrossSystems.common(rect: EsdeRect): Common {
        if (rect.guessed) report.adjusted(view, key, EsdeTranslationNote.SizeGuessed)
        val zIndex = float("zIndex") ?: DEFAULT_Z[key.type] ?: OTHER_DEFAULT_Z
        return Common(
            id = "${key.type}-${key.name}",
            rect = rect.rect,
            z = (zIndex - primaryZ).roundToInt(),
            opacity = (float("opacity") ?: 1f).coerceIn(0f, 1f),
            visibleWhen = systemNames.takeUnless { onEverySystem }?.let { ThemeVisibleWhen(systems = it) },
        )
    }

    private fun squareSide(): Pair<Float, Float> = GUESSED_SIDE / aspect to GUESSED_SIDE

    private fun image(element: EsdeElementAcrossSystems): ThemeLayer? {
        val rect = element.rect(squareSide())
        val chain = pictureChainOf(element, rect.rect) ?: return null
        if (element.bool("tile") == true) report.adjusted(view, element.key, EsdeTranslationNote.TileAsCrop)
        val common = element.common(rect)
        return ThemeLayer.Picture(
            id = common.id,
            rect = common.rect,
            z = common.z,
            opacity = common.opacity,
            visibleWhen = common.visibleWhen,
            source = chain.first(),
            fit = fitOf(element),
            corner = cornerOf(element.float("cornerRadius")),
            fallbacks = chain.drop(1),
        )
    }

    /**
     * Source first, then fallbacks: the imageType chain (the focused game's media, in ES-DE's
     * order) or else the static `path`, then the `default` image. Null after reporting why.
     */
    private fun pictureChainOf(element: EsdeElementAcrossSystems, rect: ThemeRect): List<ThemePicture>? {
        val media = mediaChainOf(element)
        val primary = media.ifEmpty { listOfNotNull(staticPicture(element, "path", rect)) }
        val chain = (primary + listOfNotNull(staticPicture(element, "default", rect))).distinct()
        if (chain.isNotEmpty()) return chain
        val note = if (mediaKindsOf(element.props["imageType"]).isNotEmpty()) {
            EsdeTranslationNote.UnsupportedMedia
        } else {
            EsdeTranslationNote.UnsupportedElement
        }
        report.notUsed(view, element.key, note)
        return null
    }

    /** The imageType kinds Jarn tracks, in ES-DE's order. */
    private fun mediaChainOf(element: EsdeElementAcrossSystems): List<ThemePicture> {
        val kinds = mediaKindsOf(element.props["imageType"])
        val known = kinds.filterNotNull()
        if (known.isNotEmpty() && kinds.first() == null) {
            report.adjusted(view, element.key, EsdeTranslationNote.MediaKindSkipped)
        }
        return known.map { ThemePicture.GameMedia(it) }
    }

    /** A file in the theme. One that changes per system becomes Jarn's own system art. */
    private fun staticPicture(element: EsdeElementAcrossSystems, property: String, rect: ThemeRect): ThemePicture? {
        val path = element.props[property]?.ifBlank { null } ?: return null
        if (element.varies(property)) {
            report.adjusted(view, element.key, EsdeTranslationNote.PerSystemFileAsSystemArt)
            return ThemePicture.SystemArt(systemArtFor(element, path, rect))
        }
        return report.asset(themeFilePath(path))
    }

    private fun systemArtFor(element: EsdeElementAcrossSystems, path: String, rect: ThemeRect): SystemArtKind = when {
        "logo" in element.key.name.lowercase() || "logo" in path.lowercase() -> SystemArtKind.Logo
        rect.width * rect.height >= BACKGROUND_AREA -> SystemArtKind.Background
        else -> SystemArtKind.Art
    }

    private fun fitOf(element: EsdeElementAcrossSystems): ThemeFit = when {
        element.bool("tile") == true || element.pair("cropSize") != null -> ThemeFit.Crop
        element.pair("maxSize") != null -> ThemeFit.Fit
        element.pair("size")?.let { (w, h) -> w > 0f && h > 0f } == true -> ThemeFit.Fill
        else -> ThemeFit.Fit
    }

    private fun video(element: EsdeElementAcrossSystems): ThemeLayer {
        val rect = element.rect(squareSide())
        val common = element.common(rect)
        val chain = (mediaChainOf(element) + listOfNotNull(staticPicture(element, "defaultImage", rect.rect))).distinct()
        return ThemeLayer.Video(
            id = common.id,
            rect = common.rect,
            z = common.z,
            opacity = common.opacity,
            visibleWhen = common.visibleWhen,
            delayMs = ((element.float("delay") ?: 0f) * SECONDS_TO_MS).roundToInt().coerceAtLeast(0),
            audio = element.bool("audio") ?: false,
            fallback = chain.firstOrNull(),
            fallbacks = chain.drop(1),
        )
    }

    private fun text(element: EsdeElementAcrossSystems): ThemeLayer? {
        element.props["metadata"]?.trim()?.lowercase()?.let { field ->
            when (field) {
                "rating" -> return rating(element)
                "releasedate", "lastplayed" -> return date(element)
            }
        }
        val content = textContent(element) ?: return null
        if (element.props["fontPath"] != null) report.adjusted(view, element.key, EsdeTranslationNote.FontNotUsed)
        val fontSize = element.float("fontSize") ?: DEFAULT_FONT_SIZE
        val lineHeight = fontSize * (element.float("lineSpacing") ?: DEFAULT_LINE_SPACING)
        val rect = element.rect(GUESSED_TEXT_WIDTH to lineHeight)
        val common = element.common(rect)
        return ThemeLayer.Text(
            id = common.id,
            rect = common.rect,
            z = common.z,
            opacity = common.opacity,
            visibleWhen = common.visibleWhen,
            content = content,
            style = textRoleFor(fontSize, common.rect.height),
            align = alignOf(element.props["horizontalAlignment"]),
            maxLines = linesThatFit(common.rect.height, lineHeight).coerceAtLeast(1),
        )
    }

    private fun textContent(element: EsdeElementAcrossSystems): ThemeText? {
        element.props["metadata"]?.let { field ->
            return gameFieldOf(field)?.let { ThemeText.Game(it) } ?: notUsedText(element, EsdeTranslationNote.UnsupportedField)
        }
        element.props["systemdata"]?.let { field ->
            return systemFieldOf(field)?.let { ThemeText.System(it) } ?: notUsedText(element, EsdeTranslationNote.UnsupportedField)
        }
        val text = element.props["text"]?.ifBlank { null } ?: return notUsedText(element, EsdeTranslationNote.UnsupportedElement)
        if (!element.varies("text")) return ThemeText.Static(text)
        val isSystemName = element.bySystem.all { (system, props) ->
            props["text"] == system.fullName || props["text"] == system.name
        }
        if (isSystemName) return ThemeText.System(SystemTextField.Title)
        return notUsedText(element, EsdeTranslationNote.DiffersPerSystem)
    }

    private fun notUsedText(element: EsdeElementAcrossSystems, note: EsdeTranslationNote): ThemeText? {
        report.notUsed(view, element.key, note)
        return null
    }

    private fun rating(element: EsdeElementAcrossSystems): ThemeLayer {
        val rect = element.rect(GUESSED_SIDE to GUESSED_SIDE / (2 * aspect))
        val common = element.common(rect)
        return ThemeLayer.Rating(
            id = common.id,
            rect = common.rect,
            z = common.z,
            opacity = common.opacity,
            visibleWhen = common.visibleWhen,
        )
    }

    private fun date(element: EsdeElementAcrossSystems): ThemeLayer? {
        val field = when (element.props["metadata"]?.trim()?.lowercase()) {
            "lastplayed" -> ThemeDateField.LastPlayed
            "releasedate", null -> ThemeDateField.ReleaseDate
            else -> {
                report.notUsed(view, element.key, EsdeTranslationNote.UnsupportedField)
                return null
            }
        }
        val fontSize = element.float("fontSize") ?: DEFAULT_FONT_SIZE
        val rect = element.rect(GUESSED_TEXT_WIDTH to fontSize * DEFAULT_LINE_SPACING)
        val common = element.common(rect)
        return ThemeLayer.Date(
            id = common.id,
            rect = common.rect,
            z = common.z,
            opacity = common.opacity,
            visibleWhen = common.visibleWhen,
            field = field,
            format = dateFormatOf(element, field),
            style = textRoleFor(fontSize, common.rect.height),
            align = alignOf(element.props["horizontalAlignment"]),
        )
    }

    private fun dateFormatOf(element: EsdeElementAcrossSystems, field: ThemeDateField): ThemeDateFormat {
        val format = element.props["format"]?.trim()
        return when {
            element.bool("displayRelative") == true -> ThemeDateFormat.Relative
            format == "%Y" -> ThemeDateFormat.Year
            format != null -> ThemeDateFormat.Short
            field == ThemeDateField.LastPlayed -> ThemeDateFormat.Relative
            else -> ThemeDateFormat.Short
        }
    }

    private data class Common(
        val id: String,
        val rect: ThemeRect,
        val z: Int,
        val opacity: Float,
        val visibleWhen: ThemeVisibleWhen?,
    )
}

internal fun gameFieldOf(field: String): GameTextField? = when (field.trim().lowercase()) {
    "name" -> GameTextField.Title
    "description" -> GameTextField.Description
    "developer" -> GameTextField.Developer
    "publisher" -> GameTextField.Publisher
    "genre" -> GameTextField.Genre
    "players" -> GameTextField.Players
    "playcount" -> GameTextField.PlayCount
    "systemname", "systemfullname" -> GameTextField.SystemTitle
    else -> null
}

internal fun systemFieldOf(field: String): SystemTextField? = when (field.trim().lowercase()) {
    "name", "fullname" -> SystemTextField.Title
    "gamecount", "gamecountgames", "gamecountgamesnotext" -> SystemTextField.GameCount
    "gamecountfavorites", "gamecountfavoritesnotext" -> SystemTextField.FavoriteCount
    else -> null
}

/**
 * Jarn's roles, largest first, as font size and line height in dp. Captions and labels have
 * strong twins with the same metrics, so only the plain ones are picked.
 */
private val ROLE_METRICS = listOf(
    Triple(ThemeTextRole.Display, 56f, 58f),
    Triple(ThemeTextRole.Title, 32f, 34f),
    Triple(ThemeTextRole.Heading, 24f, 26f),
    Triple(ThemeTextRole.Body, 16f, 22f),
    Triple(ThemeTextRole.Label, 14f, 18f),
    Triple(ThemeTextRole.Caption, 12f, 16f),
)

/** The Thor's Library stage height in dp (805px at 369 dpi), the screen Jarn's own layouts are sized for. */
internal const val ESDE_REFERENCE_STAGE_DP = 349f

/**
 * ES-DE font sizes are fractions of the screen height. On the reference stage that is a size in
 * dp; this picks Jarn's nearest role, then steps down until one line fits [rectHeight].
 */
internal fun textRoleFor(fontSize: Float, rectHeight: Float): ThemeTextRole {
    val sizeDp = fontSize * ESDE_REFERENCE_STAGE_DP
    val nearest = ROLE_METRICS.indexOf(ROLE_METRICS.minBy { abs(it.second - sizeDp) })
    return fittedRole(ROLE_METRICS[nearest].first, rectHeight)
}

/** [role], or the next smaller role whose line fits [rectHeight] of the reference stage. */
internal fun fittedRole(role: ThemeTextRole, rectHeight: Float): ThemeTextRole {
    val roomDp = rectHeight * ESDE_REFERENCE_STAGE_DP
    val start = ROLE_METRICS.indexOfFirst { it.first == role }.takeIf { it >= 0 } ?: return role
    return ROLE_METRICS.drop(start).firstOrNull { it.third <= roomDp }?.first ?: ThemeTextRole.Caption
}

/** Whole lines of [lineHeight] in [height], forgiving float error just under a whole line. */
internal fun linesThatFit(height: Float, lineHeight: Float): Int = (height / lineHeight + LINE_FIT_SLACK).toInt()

internal fun cornerOf(radius: Float?): ThemeCorner = when {
    radius == null || radius <= 0f -> ThemeCorner.None
    radius < 0.005f -> ThemeCorner.Sm
    radius < 0.01f -> ThemeCorner.Md
    radius < 0.02f -> ThemeCorner.Lg
    else -> ThemeCorner.Xl
}
