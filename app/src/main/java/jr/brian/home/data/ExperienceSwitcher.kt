package jr.brian.home.data

import android.content.Context
import jr.brian.core.model.Experience
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.launchJarnFe
import jr.brian.home.ui.util.routeToClassic
import javax.inject.Inject
import javax.inject.Singleton

/** Moves between experiences. Only the selector changes, so every other store is left alone. */
@Singleton
class ExperienceSwitcher @Inject constructor(
    private val store: ExperienceStore,
    private val esdePreferences: ESDEPreferencesManager,
) {
    fun switchToClassic(host: Context): Boolean = switchExperience(store, Experience.Classic) {
        routeToClassic(host, esdePreferences.state.value.frontendEnabled)
        true
    }

    fun switchToJarnFe(host: Context): Boolean = switchExperience(store, Experience.JarnFE) {
        launchJarnFe(host)
    }
}

/** Saves [target] only once it opened, so a failed switch can't strand the next launch. */
internal fun switchExperience(
    store: ExperienceStore,
    target: Experience,
    open: () -> Boolean,
): Boolean {
    val opened = runCatching(open).getOrDefault(false)
    if (opened) store.setExperience(target)
    return opened
}
