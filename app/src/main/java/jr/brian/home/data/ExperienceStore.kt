package jr.brian.home.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.Experience
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted [Experience] selector. One pref in its own file, deliberately
 * separate from `frontendEnabled`. Missing or unknown values read as Classic.
 */
@Singleton
class ExperienceStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Experience> = _state.asStateFlow()

    val experience: Experience get() = _state.value

    /** Null until something has saved a choice, which [experience] cannot distinguish. */
    val storedExperience: Experience?
        get() = prefs.getString(KEY_EXPERIENCE, null)?.let(Experience::fromNameOrDefault)

    private val _everJarnFe = MutableStateFlow(
        prefs.getBoolean(KEY_EVER_JARN_FE, false) || storedExperience == Experience.JarnFE,
    )

    /** True once JarnFE was ever saved here, even when Classic was picked again later. */
    val everJarnFeState: StateFlow<Boolean> = _everJarnFe.asStateFlow()
    val everJarnFe: Boolean get() = _everJarnFe.value

    fun setExperience(experience: Experience) {
        _state.value = experience
        val isJarnFe = experience == Experience.JarnFE
        if (isJarnFe) _everJarnFe.value = true
        prefs.edit {
            putString(KEY_EXPERIENCE, experience.name)
            if (isJarnFe) putBoolean(KEY_EVER_JARN_FE, true)
        }
    }

    private fun load(): Experience =
        Experience.fromNameOrDefault(prefs.getString(KEY_EXPERIENCE, null))

    private companion object {
        const val PREFS_NAME = "experience_prefs"
        const val KEY_EXPERIENCE = "experience"
        const val KEY_EVER_JARN_FE = "ever_jarn_fe"
    }
}
