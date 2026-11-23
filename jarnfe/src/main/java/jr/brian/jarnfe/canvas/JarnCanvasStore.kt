package jr.brian.jarnfe.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val NO_PAGE = -1

/** Shared by the Stage and Deck windows, so the chosen page survives a tab or window change. */
internal class JarnCanvasStore {
    var snapshot by mutableStateOf<JarnCanvasSnapshot?>(null)
        private set

    var selectedPage by mutableIntStateOf(NO_PAGE)

    /** Runs until cancelled. Starts on the Home page, and keeps the selection on a real page. */
    suspend fun observe(data: JarnCanvasData) {
        data.snapshot.collect { next ->
            snapshot = next
            selectedPage = resolveSelectedPage(selectedPage, next)
        }
    }
}

internal fun resolveSelectedPage(selected: Int, snapshot: JarnCanvasSnapshot): Int {
    val last = snapshot.pages.lastIndex
    return when {
        last < 0 -> NO_PAGE
        selected == NO_PAGE -> snapshot.homePage.coerceIn(0, last)
        else -> selected.coerceIn(0, last)
    }
}

internal val JarnCanvas = JarnCanvasStore()
