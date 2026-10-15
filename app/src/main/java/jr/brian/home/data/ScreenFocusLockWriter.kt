package jr.brian.home.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.model.ScreenAssignment
import jr.brian.home.util.ThorDetection

/**
 * Writes AYN's `Settings.System.screen_focus_lock` so gamepad input
 * routes to whichever display the user is interacting with. Called
 * from screen-swap triggers, app launches, game launches, and boot so
 * AYN's Dual Screen Assistant pins gamepad to the active display
 * instead of auto-following the last-interacted screen.
 *
 * Values AYN's Focus Lock quick-settings tile publishes:
 *  - 0 = Auto (gamepad follows last-interacted screen)
 *  - 1 = Top (gamepad locked to primary display)
 *  - 2 = Bottom (gamepad locked to external display)
 *
 * We never write 0 — the intent is "gamepad always on the active
 * surface," so each call pins the lock to a specific display.
 *
 * Every call is a no-op unless the user has
 * [FocusLockPrefsManager.autoManageEnabled] on. The guard lives here
 * (not at call sites) so the handful of hooks sprinkled through launch
 * code paths stay one-liners.
 */
object ScreenFocusLockWriter {

    private const val TAG = "ScreenFocusLockWriter"
    private const val KEY = "screen_focus_lock"
    private const val VALUE_TOP = 1
    private const val VALUE_BOTTOM = 2

    fun syncToAssignment(context: Context, assignment: ScreenAssignment) {
        val target = when (assignment) {
            ScreenAssignment.FRONTEND_ON_TOP -> VALUE_TOP
            ScreenAssignment.FRONTEND_ON_BOTTOM -> VALUE_BOTTOM
        }
        write(context, target)
    }

    fun syncToDisplayId(context: Context, displayId: Int) {
        val target = if (displayId == PRIMARY_DISPLAY_ID) VALUE_TOP else VALUE_BOTTOM
        write(context, target)
    }

    private fun write(context: Context, target: Int) {
        if (!ThorDetection.isThor()) return
        if (!shouldAutoManage(context)) return
        if (!Settings.System.canWrite(context)) {
            Log.d(TAG, "WRITE_SETTINGS not granted; skipping focus lock sync")
            return
        }
        val current = Settings.System.getInt(context.contentResolver, KEY, -1)
        if (current == target) return
        runCatching {
            Settings.System.putInt(context.contentResolver, KEY, target)
        }.onFailure { Log.w(TAG, "Failed to write $KEY=$target", it) }
    }

    private fun shouldAutoManage(context: Context): Boolean = try {
        EntryPointAccessors
            .fromApplication(context.applicationContext, FocusLockEntryPoint::class.java)
            .focusLockPrefsManager()
            .autoManageEnabled
            .value
    } catch (_: Throwable) {
        // Fallback to the default-on behaviour if Hilt isn't reachable
        // (unit tests, pre-Application init). Matches the pref's default.
        FocusLockPrefsManager.DEFAULT_AUTO_MANAGE
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface FocusLockEntryPoint {
        fun focusLockPrefsManager(): FocusLockPrefsManager
    }
}
