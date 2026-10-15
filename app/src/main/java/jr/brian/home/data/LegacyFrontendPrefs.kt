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
 * One-key preference holder for the "Legacy frontend" toggle.
 * Deliberately NOT a field on `ESDEPrefsState` — that type is on
 * the plan's Phase 6 "collapse this" list and adding to it would
 * walk in the wrong direction.
 *
 * ### Semantics
 *
 * - `true` → start `:app`'s legacy `FrontEndActivity` (the
 *   pre-Phase-3 code path).
 * - `false` (default from Phase 3h on) → start the new `:frontend`
 *   `FrontendActivity` via
 *   [jr.brian.frontend.ui.FrontendEntry].
 *
 * The plan calls for one release of coexistence before the legacy
 * code path is deleted. During that window users who hit regressions
 * can flip the Settings → Experimental row below back to legacy.
 * The next phase (post-3h, release-after-3h) deletes the legacy
 * activity and this class.
 */
@Singleton
class LegacyFrontendPrefs @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(
        // Fresh installs default to the new frontend; users who had the
        // key written explicitly (any value) during the Phase 3a window
        // keep their choice.
        prefs.getBoolean(KEY_LEGACY_FRONTEND_ENABLED, false),
    )

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
