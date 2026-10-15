package jr.brian.core.model.bus

import jr.brian.core.model.GameImageType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Typed, bidirectional contract between the launcher and the frontend.
 *
 * Replaces the ten-plus `SharedFlow` fields on `RomSearchStateHolder` and
 * the ad-hoc `FrontEndActivity.isRunning` static that currently coordinate
 * three activities across two displays. Everything that one side needs to
 * tell the other goes through this interface; nothing imports the other
 * side's concrete types.
 *
 * Phase 1 introduces the interface and an in-memory [FrontendBusAdapter]
 * implementation; no :app code consumes it yet. Later phases migrate the
 * `RomSearchStateHolder` signals onto it one at a time.
 */
interface FrontendBus {
    /** Frontend → launcher: what the user is currently looking at. Null when nothing is focused. */
    val focus: StateFlow<FrontendFocus?>

    /** Frontend → launcher: one-shot lifecycle events. Replay is NOT guaranteed. */
    val events: SharedFlow<FrontendEvent>

    /** Launcher → frontend: imperative requests ("show game X", "run a search"). */
    val commands: SharedFlow<FrontendCommand>

    /** Observable presence flags so each side can route around the other being absent. */
    val launcherPresent: StateFlow<Boolean>
    val frontendPresent: StateFlow<Boolean>

    /** Frontend → launcher: emit a new focus. */
    suspend fun publishFocus(focus: FrontendFocus?)

    /** Frontend → launcher: emit a lifecycle event. */
    suspend fun publishEvent(event: FrontendEvent)

    /** Launcher → frontend: send a command. */
    suspend fun sendCommand(command: FrontendCommand)

    /** Launcher calls on `onStart` / `onStop`; frontend calls on its own. */
    fun setLauncherPresent(present: Boolean)
    fun setFrontendPresent(present: Boolean)
}

/**
 * What the frontend is currently focused on. Carries the resolved art bundle
 * so widgets and wallpaper don't need to re-probe disk every focus change.
 */
sealed interface FrontendFocus {
    val art: ResolvedArt

    data class System(
        val id: String,
        val name: String,
        override val art: ResolvedArt,
    ) : FrontendFocus

    data class Game(
        val id: String,
        val title: String,
        val systemId: String,
        override val art: ResolvedArt,
    ) : FrontendFocus
}

/**
 * All per-type media paths for a focused entity, resolved once at index time.
 * Any field may be null when no asset is available. Paths are absolute (file
 * system) or `content://` URIs; consumers should treat them opaquely and feed
 * them straight to the image loader.
 *
 * See [GameImageType] for the enum that identifies which slot a tile wants.
 */
data class ResolvedArt(
    val cover: String? = null,
    val marquee: String? = null,
    val fanart: String? = null,
    val screenshot: String? = null,
    val titlescreen: String? = null,
    val miximage: String? = null,
    val video: String? = null,
) {
    /** Convenience accessor for the canvas tile renderer. */
    fun pathFor(kind: GameImageType): String? = when (kind) {
        GameImageType.Covers -> cover
        GameImageType.Marquee -> marquee
        GameImageType.Fanart -> fanart
        GameImageType.Screenshots -> screenshot
        GameImageType.TitleScreens -> titlescreen
        GameImageType.MixImages -> miximage
        GameImageType.PhysicalMedia -> null
        GameImageType.All, GameImageType.None, GameImageType.Description -> null
    }

    companion object {
        val EMPTY = ResolvedArt()
    }
}

/** Frontend → launcher lifecycle event stream. */
sealed interface FrontendEvent {
    data class GameLaunched(val gameId: String) : FrontendEvent
    data class GameEnded(val gameId: String) : FrontendEvent
    data object ScreensaverStarted : FrontendEvent
    data object ScreensaverEnded : FrontendEvent
}

/** Launcher → frontend command stream. */
sealed interface FrontendCommand {
    data class ShowGame(val gameId: String) : FrontendCommand
    data class ShowSystem(val systemId: String) : FrontendCommand
    data class Search(val query: String) : FrontendCommand
    data object SwapScreens : FrontendCommand
}
