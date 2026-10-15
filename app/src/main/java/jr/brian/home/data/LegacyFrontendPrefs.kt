package jr.brian.home.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-key preference holder for the Phase 3 "Legacy frontend"
 * toggle. Deliberately NOT a field on [ESDEPrefsState] — that type is
 * on the plan's Phase 6 "collapse this" list and adding to it would
 * walk in the wrong direction.
 *
 * ### Semantics
 *
 * - `true` (default) → start `:app`'s existing `FrontEndActivity`,
 *   same code path as before this PR.
 * - `false` → start the new `:frontend` `FrontendActivity` via
 *   [jr.brian.frontend.ui.FrontendEntry].
 *
 * Default is `true` so no behaviour change on upgrade; the new
 * frontend is opt-in until Phase 3h flips the default. Phase 3h
 * also deletes this manager and the old `FrontEndActivity`.
 */
@Singleton
class LegacyFrontendPrefs @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_LEGACY_FRONTEND_ENABLED, true))

    val legacyFrontendEnabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setLegacyFrontendEnabled(enabled: Boolean) {
        _enabled.value = enabled
        prefs.edit { putBoolean(KEY_LEGACY_FRONTEND_ENABLED, enabled) }
    }

    private companion object {
        const val PREFS_NAME = "legacy_frontend_prefs"
        const val KEY_LEGACY_FRONTEND_ENABLED = "legacy_frontend_enabled"
    }
}
