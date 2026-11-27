package jr.brian.jarnfe.library.parity

import android.app.Application
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.library.JarnLibraryFeed
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.look.JarnLookStoreProvider

/** Gives browse the real display prefs store, so a test can pick Grid or Carousel. */
class BrowseParityApplication : Application(), JarnLookStoreProvider {
    override val jarnLookStore: JarnLookStore by lazy { JarnLookStore(this) }
}

/** A small library that never changes, so the parity golden only moves when browse does. */
internal object BrowseParityLibrary {
    const val OPEN_SYSTEM = "snes"

    val systems = listOf("nes", "snes", "n64", "gb", "gba", "nds", "psx", "ps2", "genesis", "dc")
        .mapIndexed { index, id -> JarnLibrarySystem(id, "System %02d %s".format(index + 1, id), gameCount = index + 3) }

    val games = (1..14).map { JarnLibraryGame("$OPEN_SYSTEM-$it", OPEN_SYSTEM, "Game %02d".format(it)) }

    fun systemsStore(): JarnLibraryStore = JarnLibraryStore().apply {
        onPlay = {}
        val all = systems
        update { copy(systems = JarnLibraryFeed.ready(all), focusedSystemId = all.first().id) }
    }

    fun gamesStore(): JarnLibraryStore = systemsStore().apply {
        update {
            copy(
                gamesBySystem = mapOf(OPEN_SYSTEM to JarnLibraryFeed.ready(games)),
                openSystemId = OPEN_SYSTEM,
                focusedSystemId = OPEN_SYSTEM,
            )
        }
    }
}

internal fun BrowseParityApplication.useViews(systems: JarnBrowseView, games: JarnBrowseView) {
    jarnLookStore.updateDisplay { it.copy(systemView = systems, gameView = games) }
}
