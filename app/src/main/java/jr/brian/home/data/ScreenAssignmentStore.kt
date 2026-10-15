package jr.brian.home.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ScreenAssignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted [ScreenAssignment] for the Phase 5 screen-swap feature.
 * One pref, its own file — avoids piling onto [ESDEPrefsState]
 * (same reason as [LegacyFrontendPrefs]).
 *
 * Default is [ScreenAssignment.DEFAULT] (`FRONTEND_ON_TOP`) per the
 * Phase 0 decision. A user who swaps sees the new assignment
 * persisted and respected across Home presses, reboots, and the
 * launcher's own cold-boot route.
 */
@Singleton
class ScreenAssignmentStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ScreenAssignment> = _state.asStateFlow()

    val assignment: ScreenAssignment get() = _state.value

    fun setAssignment(assignment: ScreenAssignment) {
        _state.value = assignment
        prefs.edit { putString(KEY_ASSIGNMENT, assignment.name) }
    }

    fun flip() {
        setAssignment(assignment.flipped())
    }

    private fun load(): ScreenAssignment =
        ScreenAssignment.fromNameOrDefault(prefs.getString(KEY_ASSIGNMENT, null))

    private companion object {
        const val PREFS_NAME = "screen_assignment_prefs"
        const val KEY_ASSIGNMENT = "assignment"
    }
}
