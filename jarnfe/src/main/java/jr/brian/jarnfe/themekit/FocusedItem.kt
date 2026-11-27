package jr.brian.jarnfe.themekit

import androidx.compose.runtime.Immutable
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibrarySelection

/**
 * What the browse screen has focused, which every theme decor layer reads. View state only: never
 * saved and never backed up.
 */
@Immutable
sealed interface FocusedItem {
    data object None : FocusedItem

    data class System(val system: JarnLibrarySystem) : FocusedItem

    /** [system] is the game's own system, for system fields and art shown beside a game. */
    data class Game(val game: JarnLibraryGame, val system: JarnLibrarySystem? = null) : FocusedItem
}

internal val FocusedItem.game: JarnLibraryGame? get() = (this as? FocusedItem.Game)?.game

/** The focused system, or the focused game's own system. */
internal val FocusedItem.system: JarnLibrarySystem?
    get() = when (this) {
        FocusedItem.None -> null
        is FocusedItem.System -> system
        is FocusedItem.Game -> system
    }

/** Changes only when a different item takes focus, not when the same one gets new data. */
internal fun FocusedItem.focusKey(): String? = when (this) {
    FocusedItem.None -> null
    is FocusedItem.System -> "system:${system.id}"
    is FocusedItem.Game -> "game:${game.id}"
}

/**
 * The browse focus as a theme sees it. [systemOf] finds a game's system by id, for system fields
 * shown beside a game. Collections have no theme bindings in M1, so they read as nothing focused.
 */
fun LibrarySelection.toFocusedItem(systemOf: (String) -> JarnLibrarySystem? = { null }): FocusedItem = when (this) {
    LibrarySelection.None, is LibrarySelection.Collection -> FocusedItem.None
    is LibrarySelection.System -> FocusedItem.System(system)
    is LibrarySelection.Game -> FocusedItem.Game(game, systemOf(game.systemId))
}
