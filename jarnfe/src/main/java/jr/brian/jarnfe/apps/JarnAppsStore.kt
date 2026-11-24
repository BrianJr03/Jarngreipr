package jr.brian.jarnfe.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Outlives the Apps tab, so coming back shows the last list on the first frame instead of nothing. */
internal class JarnAppsStore {
    var snapshot by mutableStateOf<JarnAppsSnapshot?>(null)
        private set

    /** Runs until cancelled. An equal snapshot leaves the drawer untouched. */
    suspend fun observe(data: JarnAppsData) {
        data.snapshot.collect { snapshot = it }
    }
}

internal val JarnApps = JarnAppsStore()
