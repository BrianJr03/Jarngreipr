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
 * Writes AYN's Settings.System.`screen_focus_lock` so gamepad input
 * routes to whichever display the current [FocusLockMode] dictates.
 *
 * Values AYN's Focus Lock quick-settings tile publishes:
 *  - 0 = Auto (we never write this)
 *  - 1 = Top (primary display)
 *  - 2 = Bottom (external display)
 *
 * Writes go through [ShizukuManager] because AYN's SettingsProvider
 * rejects third-party writes to this key with
 * `IllegalArgumentException: You cannot keep your settings in the
 * secure settings.`. Shizuku's shell service runs at the UID AYN's
 * provider allows.
 *
 * Mode interpretation:
 *  - [FocusLockMode.Off]: never writes.
 *  - [FocusLockMode.Top]: every sync writes 1, regardless of context.
 *  - [FocusLockMode.Bottom]: every sync writes 2, regardless of context.
 *  - [FocusLockMode.Follow]: writes the display that matches context —
 *    the frontend's assigned side after a swap, the launched app's
 *    display on tap, PRIMARY during a game, and back to the frontend's
 *    assigned side whenever our activities regain focus.
 */
object ScreenFocusLockWriter {

    private const val TAG = "ScreenFocusLockWriter"
    private const val KEY = "screen_focus_lock"
    private const val KEY_TICKLE = "focus_change"
    private const val VALUE_TOP = 1
    private const val VALUE_BOTTOM = 2

    /** Follow-mode sync: pin to the display the frontend is on. */
    fun syncToAssignment(context: Context, assignment: ScreenAssignment) {
        val mode = currentMode(context) ?: return
        val target = resolveTarget(mode, followValue = {
            if (assignment == ScreenAssignment.FRONTEND_ON_TOP) VALUE_TOP else VALUE_BOTTOM
        }) ?: return
        write(context, target)
    }

    /** Follow-mode sync: pin to a specific display id (used by app/game launches). */
    fun syncToDisplayId(context: Context, displayId: Int) {
        val mode = currentMode(context) ?: return
        val target = resolveTarget(mode, followValue = {
            if (displayId == PRIMARY_DISPLAY_ID) VALUE_TOP else VALUE_BOTTOM
        }) ?: return
        write(context, target)
    }

    private fun resolveTarget(mode: FocusLockMode, followValue: () -> Int): Int? = when (mode) {
        FocusLockMode.Off -> null
        FocusLockMode.Top -> VALUE_TOP
        FocusLockMode.Bottom -> VALUE_BOTTOM
        FocusLockMode.Follow -> followValue()
    }

    private fun currentMode(context: Context): FocusLockMode? {
        if (!ThorDetection.isThor()) return null
        val entry = runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, FocusLockEntryPoint::class.java)
        }.getOrNull() ?: return null
        return entry.focusLockPrefsManager().mode.value.takeIf { it != FocusLockMode.Off }
    }

    private fun write(context: Context, target: Int) {
        val entry = runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, FocusLockEntryPoint::class.java)
        }.getOrNull() ?: return
        val shizuku = entry.shizukuManager()
        if (!shizuku.isShizukuPermissionGranted.value) {
            Log.d(TAG, "Shizuku not granted; skipping focus lock sync (target=$target)")
            return
        }
        val current = Settings.System.getInt(context.contentResolver, KEY, -1)
        if (current == target) return
        val lockWrote = shizuku.putSystemSetting(KEY, target.toString())
        if (!lockWrote) {
            Log.w(TAG, "Shizuku refused to write $KEY=$target")
            return
        }
        val prevTickle = Settings.System.getInt(context.contentResolver, KEY_TICKLE, 0)
        val tickleWrote = shizuku.putSystemSetting(KEY_TICKLE, (prevTickle + 1).toString())
        if (!tickleWrote) {
            Log.w(TAG, "Shizuku refused to bump $KEY_TICKLE")
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface FocusLockEntryPoint {
        fun focusLockPrefsManager(): FocusLockPrefsManager
        fun shizukuManager(): ShizukuManager
    }
}
