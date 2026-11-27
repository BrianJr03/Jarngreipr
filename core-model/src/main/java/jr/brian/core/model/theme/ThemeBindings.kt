package jr.brian.core.model.theme

import jr.brian.core.model.MediaKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where a picture comes from: the focused game's media, the focused system's art, or a file in the theme. */
@Serializable
sealed interface ThemePicture {
    @Serializable
    @SerialName("media")
    data class GameMedia(val kind: MediaKind) : ThemePicture

    @Serializable
    @SerialName("system")
    data class SystemArt(val kind: SystemArtKind) : ThemePicture

    /** [path] is relative to the theme folder. */
    @Serializable
    @SerialName("file")
    data class File(val path: String) : ThemePicture
}

@Serializable
enum class SystemArtKind {
    @SerialName("art") Art,
    @SerialName("logo") Logo,
    @SerialName("background") Background,
}

/** What a text layer shows: fixed text, or a field of the focused game or system. */
@Serializable
sealed interface ThemeText {
    @Serializable
    @SerialName("static")
    data class Static(val text: String) : ThemeText

    @Serializable
    @SerialName("game")
    data class Game(val field: GameTextField) : ThemeText

    @Serializable
    @SerialName("system")
    data class System(val field: SystemTextField) : ThemeText
}

@Serializable
enum class GameTextField {
    @SerialName("title") Title,
    @SerialName("system") SystemTitle,
    @SerialName("description") Description,
    @SerialName("developer") Developer,
    @SerialName("publisher") Publisher,
    @SerialName("genre") Genre,
    @SerialName("players") Players,
    @SerialName("playCount") PlayCount,
    @SerialName("playtime") Playtime,
}

@Serializable
enum class SystemTextField {
    @SerialName("title") Title,
    @SerialName("gameCount") GameCount,
    @SerialName("favoriteCount") FavoriteCount,
    @SerialName("playtime") Playtime,
}

@Serializable
enum class ThemeDateField {
    @SerialName("releaseDate") ReleaseDate,
    @SerialName("lastPlayed") LastPlayed,
}

@Serializable
enum class ThemeDateFormat {
    @SerialName("year") Year,
    @SerialName("short") Short,
    @SerialName("relative") Relative,
}

@Serializable
enum class ThemeTextRole {
    @SerialName("display") Display,
    @SerialName("title") Title,
    @SerialName("heading") Heading,
    @SerialName("body") Body,
    @SerialName("label") Label,
    @SerialName("labelStrong") LabelStrong,
    @SerialName("caption") Caption,
    @SerialName("captionStrong") CaptionStrong,
}

@Serializable
enum class ThemeFit {
    @SerialName("fit") Fit,
    @SerialName("crop") Crop,
    @SerialName("fill") Fill,
}

/**
 * A layer shows only when every set condition holds. [hasMedia] / [hasText] look at the
 * focused game; [systems] limits the layer to those system ids.
 */
@Serializable
data class ThemeVisibleWhen(
    val hasMedia: MediaKind? = null,
    val hasText: GameTextField? = null,
    val systems: List<String> = emptyList(),
)
