package jr.brian.jarnfe.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.core.ui.components.jarnNormalizedOrder
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.look.jarnLookStoreOrNull

internal fun librarySectionOrder(ids: List<String>): List<LibrarySection> =
    jarnNormalizedOrder(ids, LibrarySection.entries) { it.name }

internal fun deckTabOrder(ids: List<String>): List<DeckTab> = jarnNormalizedOrder(ids, DeckTab.entries) { it.name }

/**
 * The player's order of the Library tabs and the bottom screen tabs. Labels, shoulder steps and
 * reordering all read it, so no strip ever follows the built-in order once it has been changed.
 */
internal object JarnTabOrders {
    var library by mutableStateOf(LibrarySection.entries.toList())
        private set
    var deck by mutableStateOf(DeckTab.entries.toList())
        private set

    fun sync(display: JarnDisplayPrefs) {
        library = librarySectionOrder(display.librarySectionOrder)
        deck = deckTabOrder(display.deckTabOrder)
    }

    fun reset() = sync(JarnDisplayPrefs())
}

/** Keeps [JarnTabOrders] on the saved display prefs. Call once per window. */
@Composable
internal fun SyncTabOrders(display: JarnDisplayPrefs) {
    SideEffect { JarnTabOrders.sync(display) }
}

/** Saves a new Library order, shown at once, before the prefs come back. */
@Composable
internal fun rememberSaveLibraryOrder(): (List<LibrarySection>) -> Unit = rememberSaveOrder { order ->
    copy(librarySectionOrder = order.map { it.name })
}

/** Saves a new bottom screen order, shown at once, before the prefs come back. */
@Composable
internal fun rememberSaveDeckOrder(): (List<DeckTab>) -> Unit = rememberSaveOrder { order ->
    copy(deckTabOrder = order.map { it.name })
}

@Composable
private fun <T> rememberSaveOrder(write: JarnDisplayPrefs.(List<T>) -> JarnDisplayPrefs): (List<T>) -> Unit {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    return remember(store) {
        { order ->
            val current = store?.display ?: JarnDisplayPrefs(
                librarySectionOrder = JarnTabOrders.library.map { it.name },
                deckTabOrder = JarnTabOrders.deck.map { it.name },
            )
            val next = current.write(order)
            JarnTabOrders.sync(next)
            store?.updateDisplay { it.write(order) }
        }
    }
}
