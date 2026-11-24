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

    fun setExperience(experience: Experience) {
        _state.value = experience
        prefs.edit { putString(KEY_EXPERIENCE, experience.name) }
    }

    /** Records a default that was actually opened, so backups report the real experience. */
    fun adoptIfUnset(experience: Experience) {
        if (storedExperience == null) setExperience(experience)
    }

    private fun load(): Experience =
        Experience.fromNameOrDefault(prefs.getString(KEY_EXPERIENCE, null))

    private companion object {
        const val PREFS_NAME = "experience_prefs"
        const val KEY_EXPERIENCE = "experience"
    }
}
