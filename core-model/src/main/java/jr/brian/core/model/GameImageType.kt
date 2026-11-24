package jr.brian.core.model

/**
 * Which media kind the launcher should display for a focused game. Persisted
 * via `@SerialName`-stable enum names so canvas layouts and config exports
 * authored on older builds decode unchanged — if you rename a value here you
 * will break every user's saved canvas.
 *
 * `folderName` is the on-disk directory relative to a media root (ES-DE
 * convention). [None] / [All] / [Description] are logical modes with no
 * single folder: `None` hides art, `All` picks randomly from every
 * `randomizableTypes` directory, and `Description` renders the game's text
 * blurb instead of an image.
 */
@kotlinx.serialization.Serializable
enum class GameImageType(val folderName: String?) {
    None(null),
    All(null),
    Screenshots("screenshots"),
    Fanart("fanart"),
    TitleScreens("titlescreens"),
    Covers("covers"),
    PhysicalMedia("physicalmedia"),
    Marquee("marquees"),
    MixImages("miximages"),
    Description(null);

    companion object {
        fun randomizableTypes(): List<GameImageType> =
            entries.filter { it.folderName != null }
    }
}
