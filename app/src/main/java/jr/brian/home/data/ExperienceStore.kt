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

    fun setExperience(experience: Experience) {
        _state.value = experience
        prefs.edit { putString(KEY_EXPERIENCE, experience.name) }
    }

    private fun load(): Experience =
        Experience.fromNameOrDefault(prefs.getString(KEY_EXPERIENCE, null))

    private companion object {
        const val PREFS_NAME = "experience_prefs"
        const val KEY_EXPERIENCE = "experience"
    }
}
