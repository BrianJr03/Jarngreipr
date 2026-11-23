package jr.brian.frontend.library.art

import jr.brian.frontend.library.media.MediaKind

enum class ArtScreen { Top, Bottom }

/** Every still-image kind, in menu order. [Default] follows what each screen ships with. */
enum class ArtChoice(val kind: MediaKind?) {
    Cover(MediaKind.Cover),
    Miximage(MediaKind.Miximage),
    Marquee(MediaKind.Marquee),
    PhysicalMedia(MediaKind.PhysicalMedia),
    Fanart(MediaKind.Fanart),
    Screenshot(MediaKind.Screenshot),
    Titlescreen(MediaKind.Titlescreen),
    Default(null);

    companion object {
        fun fromNameOrNull(name: String?): ArtChoice? = entries.firstOrNull { it.name == name }
    }
}

private val TOP_DEFAULT_KINDS = listOf(
    MediaKind.Cover,
    MediaKind.Miximage,
    MediaKind.Screenshot,
    MediaKind.Titlescreen,
    MediaKind.Fanart,
)
private val BOTTOM_DEFAULT_KINDS = listOf(MediaKind.Marquee) + TOP_DEFAULT_KINDS

private val ART_CHAINS: Map<ArtScreen, Map<ArtChoice, List<MediaKind>>> =
    ArtScreen.entries.associateWith { screen ->
        val defaults = if (screen == ArtScreen.Top) TOP_DEFAULT_KINDS else BOTTOM_DEFAULT_KINDS
        ArtChoice.entries.associateWith { choice ->
            (listOfNotNull(choice.kind) + defaults).distinct()
        }
    }

/** The kinds to try in order. A missing choice falls through the screen's Default. */
internal fun artChain(screen: ArtScreen, choice: ArtChoice): List<MediaKind> =
    ART_CHAINS.getValue(screen).getValue(choice)

/** Game entries use [LibraryGame.key], which survives a rescan. */
data class GameArtChoices(
    val topBySystem: Map<String, ArtChoice> = emptyMap(),
    val bottomBySystem: Map<String, ArtChoice> = emptyMap(),
    val topByGame: Map<String, ArtChoice> = emptyMap(),
    val bottomByGame: Map<String, ArtChoice> = emptyMap(),
) {
    private fun systems(screen: ArtScreen) = if (screen == ArtScreen.Top) topBySystem else bottomBySystem

    private fun games(screen: ArtScreen) = if (screen == ArtScreen.Top) topByGame else bottomByGame

    private fun with(
        screen: ArtScreen,
        systems: Map<String, ArtChoice> = systems(screen),
        games: Map<String, ArtChoice> = games(screen),
    ) = if (screen == ArtScreen.Top) {
        copy(topBySystem = systems, topByGame = games)
    } else {
        copy(bottomBySystem = systems, bottomByGame = games)
    }

    fun effective(screen: ArtScreen, gameKey: String, systemId: String): ArtChoice =
        games(screen)[gameKey] ?: systems(screen)[systemId] ?: ArtChoice.Default

    /** Default is only stored when it has a system choice to override. */
    fun withGame(screen: ArtScreen, gameKey: String, systemId: String, choice: ArtChoice): GameArtChoices {
        val keepsDefault = choice != ArtChoice.Default || systems(screen).containsKey(systemId)
        val games = if (keepsDefault) games(screen) + (gameKey to choice) else games(screen) - gameKey
        return with(screen, games = games)
    }

    /** Replaces every game override for this screen in the system. */
    fun withSystem(screen: ArtScreen, systemId: String, choice: ArtChoice): GameArtChoices {
        val systems = if (choice == ArtChoice.Default) {
            systems(screen) - systemId
        } else {
            systems(screen) + (systemId to choice)
        }
        val games = games(screen).filterKeys { parseGameKey(it)?.systemId != systemId }
        return with(screen, systems, games)
    }

    fun gameOverrideCount(screen: ArtScreen, systemId: String): Int =
        games(screen).keys.count { parseGameKey(it)?.systemId == systemId }
}
