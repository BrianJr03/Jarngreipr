package jr.brian.jarnfe.themekit

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.SystemTextField
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeVisibleWhen
import jr.brian.jarnfe.library.JarnGameMetadata
import jr.brian.jarnfe.library.releaseYear
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val STAR_SCALE = 5f
private const val TEN_SCALE = 10f
private const val ES_DE_DATE_DIGITS = 8
private val EsDeDate = DateTimeFormatter.ofPattern("yyyyMMdd")

/**
 * The focused game's media paths by kind. [gameId] says which game they belong to, so a layer
 * never shows one game's art beside another's title.
 */
internal data class ThemeFocusMedia(val gameId: String? = null, val paths: Map<MediaKind, String> = emptyMap()) {
    fun pathFor(game: String, kind: MediaKind): String? = paths[kind]?.takeIf { gameId == game && it.isNotBlank() }
}

/** A text binding's value. Counts and times are formatted with string resources when drawn. */
internal sealed interface ThemeTextValue {
    data class Plain(val text: String) : ThemeTextValue
    data class GameCount(val count: Int) : ThemeTextValue
    data class FavoriteCount(val count: Int) : ThemeTextValue
    data class PlayCount(val count: Int) : ThemeTextValue
    data class Playtime(val minutes: Long) : ThemeTextValue
}

/** A date binding's value. [epochMs] is null when only the [year] is known. */
internal data class ThemeDateValue(val year: Int, val epochMs: Long? = null)

/** Null when the focused item has no value for [content], so the layer hides. */
internal fun resolveThemeText(content: ThemeText, focus: FocusedItem): ThemeTextValue? = when (content) {
    is ThemeText.Static -> content.text.cleaned()?.let(ThemeTextValue::Plain)
    is ThemeText.Game -> focus.game?.let { gameText(content.field, it.title, it.metadata, focus) }
    is ThemeText.System -> (focus as? FocusedItem.System)?.let { systemText(content.field, it) }
}

private fun gameText(field: GameTextField, title: String, meta: JarnGameMetadata, focus: FocusedItem): ThemeTextValue? =
    when (field) {
        GameTextField.Title -> title.plain()
        GameTextField.SystemTitle -> focus.system?.title.plain()
        GameTextField.Description -> meta.description.plain()
        GameTextField.Developer -> meta.developer.plain()
        GameTextField.Publisher -> meta.publisher.plain()
        GameTextField.Genre -> meta.genre.plain()
        GameTextField.Players -> meta.players.cleaned()?.takeUnless { it == "0" }?.let(ThemeTextValue::Plain)
        GameTextField.PlayCount -> meta.playCount?.takeIf { it > 0 }?.let(ThemeTextValue::PlayCount)
        GameTextField.Playtime -> meta.playtimeMinutes?.takeIf { it > 0 }?.let(ThemeTextValue::Playtime)
    }

private fun systemText(field: SystemTextField, focus: FocusedItem.System): ThemeTextValue? {
    val system = focus.system
    return when (field) {
        SystemTextField.Title -> system.title.plain()
        SystemTextField.GameCount -> ThemeTextValue.GameCount(system.gameCount)
        SystemTextField.FavoriteCount -> system.favoriteCount.takeIf { it > 0 }?.let(ThemeTextValue::FavoriteCount)
        SystemTextField.Playtime -> system.playtimeMinutes.takeIf { it > 0 }?.let(ThemeTextValue::Playtime)
    }
}

/** Dates belong to games; a focused system has none. */
internal fun resolveThemeDate(field: ThemeDateField, focus: FocusedItem, zone: ZoneId = ZoneId.systemDefault()): ThemeDateValue? {
    val meta = focus.game?.metadata ?: return null
    return when (field) {
        ThemeDateField.ReleaseDate -> releaseDateValue(meta.releaseDate, zone)
        ThemeDateField.LastPlayed -> meta.lastPlayedAtMs?.takeIf { it > 0 }?.let { ms ->
            ThemeDateValue(Instant.ofEpochMilli(ms).atZone(zone).year, ms)
        }
    }
}

/** Reads ES-DE's `19950623T000000`, and falls back to any year written by hand. */
internal fun releaseDateValue(releaseDate: String?, zone: ZoneId = ZoneId.systemDefault()): ThemeDateValue? {
    val text = releaseDate.cleaned() ?: return null
    val day = text.take(ES_DE_DATE_DIGITS).takeIf { it.length == ES_DE_DATE_DIGITS && it.all(Char::isDigit) }?.let {
        try {
            LocalDate.parse(it, EsDeDate)
        } catch (_: DateTimeParseException) {
            null
        }
    }
    if (day != null) return ThemeDateValue(day.year, day.atStartOfDay(zone).toInstant().toEpochMilli())
    return releaseYear(text)?.toIntOrNull()?.let { ThemeDateValue(it) }
}

/**
 * The focused game's rating from 0 to 1. ES-DE writes 0 to 1; 0 to 5 and 0 to 10 scales are
 * read too. Null when unrated, so the layer hides.
 */
internal fun resolveThemeRating(focus: FocusedItem): Float? {
    val value = focus.game?.metadata?.rating.cleaned()?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: return null
    return when {
        value <= 1f -> value
        value <= STAR_SCALE -> value / STAR_SCALE
        value <= TEN_SCALE -> value / TEN_SCALE
        else -> null
    }
}

/**
 * The file a picture layer shows, or null to hide it. Game media needs [media] for the focused
 * game; a theme file needs [themeDir] and must stay inside it.
 */
internal fun resolveThemePicture(
    source: ThemePicture,
    focus: FocusedItem,
    media: ThemeFocusMedia,
    themeDir: File?,
): String? = when (source) {
    is ThemePicture.GameMedia -> focus.game?.let { gameMediaPath(it.id, it.artPath, source.kind, media) }
    is ThemePicture.SystemArt -> focus.system?.let { system ->
        when (source.kind) {
            SystemArtKind.Art -> system.artPath
            SystemArtKind.Logo -> system.logoPath
            SystemArtKind.Background -> system.nowBackgroundPath.cleaned() ?: system.artPath
        }.cleaned()
    }
    is ThemePicture.File -> themeFile(themeDir, source.path)
}

/** Every file [sources] has for the focused item, in order and without repeats, for a picture to try one by one. */
internal fun resolveThemePictures(
    sources: List<ThemePicture>,
    focus: FocusedItem,
    media: ThemeFocusMedia,
    themeDir: File?,
): List<String> = sources.mapNotNull { resolveThemePicture(it, focus, media, themeDir) }.distinct()

private fun gameMediaPath(gameId: String, artPath: String?, kind: MediaKind, media: ThemeFocusMedia): String? =
    media.pathFor(gameId, kind) ?: artPath.cleaned()?.takeIf { kind == MediaKind.Cover }

private fun themeFile(themeDir: File?, path: String): String? {
    val dir = themeDir?.canonicalFile ?: return null
    val file = File(dir, path.cleaned() ?: return null).canonicalFile
    return file.path.takeIf { file.toPath().startsWith(dir.toPath()) && file != dir }
}

/**
 * Every set condition must hold. With nothing set the layer always shows. [ThemeVisibleWhen.missing]
 * flips the media, text and system art checks, and only when at least one of them is set.
 */
internal fun themeLayerVisible(rule: ThemeVisibleWhen?, focus: FocusedItem, media: ThemeFocusMedia): Boolean {
    if (rule == null) return true
    val game = focus.game
    val mediaOk = rule.hasMedia?.let { kind -> game != null && gameMediaPath(game.id, game.artPath, kind, media) != null }
    val textOk = rule.hasText?.let { field -> resolveThemeText(ThemeText.Game(field), focus) != null }
    val artOk = rule.hasSystemArt?.let { kind -> resolveThemePicture(ThemePicture.SystemArt(kind), focus, media, null) != null }
    val checks = listOfNotNull(mediaOk, textOk, artOk)
    val present = checks.all { it }
    val presenceOk = if (rule.missing && checks.isNotEmpty()) !present else present
    val systemOk = rule.systems.isEmpty() || (focus.system?.id ?: game?.systemId) in rule.systems
    return presenceOk && systemOk
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun String?.plain(): ThemeTextValue? = cleaned()?.let(ThemeTextValue::Plain)
