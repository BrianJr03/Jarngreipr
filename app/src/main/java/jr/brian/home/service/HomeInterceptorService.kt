package jr.brian.home.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.HomeButtonManager
import jr.brian.home.data.VolumeChordManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.routeHome
import jr.brian.home.util.ThorVolume
import javax.inject.Inject

/**
 * Accessibility service for two AYN Thor-specific key features:
 *
 * 1. Hardware Home-button interception — routes [jr.brian.home.MainActivity] to
 *    the external display even when Jarngreipr isn't the current default home.
 *    See [HomeKeyEventFilter] for the pure filtering logic.
 * 2. Select + Volume Up/Down chord — adjusts the bottom screen's independent
 *    volume via [ThorVolume]. See [VolumeChordKeyFilter].
 *
 * Both features are additive to [jr.brian.home.HomeRouterActivity]: on stock
 * AOSP, `PhoneWindowManager` consumes `KEYCODE_HOME` before input dispatch
 * reaches accessibility key filtering, so the Home path here is a no-op and
 * Home falls through to the router activity. The Thor firmware surfaces the
 * hardware Home button as a plain key event, giving this service a chance to
 * run.
 */
@AndroidEntryPoint
class HomeInterceptorService : AccessibilityService() {

    @Inject
    lateinit var homeButtonManager: HomeButtonManager

    @Inject
    lateinit var esdePreferencesManager: ESDEPreferencesManager

    @Inject
    lateinit var volumeChordManager: VolumeChordManager

    private val filter = HomeKeyEventFilter()
    private val volumeChordFilter = VolumeChordKeyFilter()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        volumeChordFilter.reset()
        isRunning = true
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onInterrupt() {
        // Intentionally empty — no long-running audio/haptic feedback to interrupt.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Home interception is a pure key-event feature; window-state events are
        // requested only because AccessibilityService requires *some* event type.
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (val r = volumeChordFilter.onKey(event, volumeChordManager.chordEnabled.value)) {
            is VolumeChordKeyFilter.Result.AdjustBottom -> {
                ThorVolume.adjustBottom(applicationContext, r.delta)
                return true
            }
            VolumeChordKeyFilter.Result.Consume -> return true
            VolumeChordKeyFilter.Result.PassThrough -> Unit
        }

        val enabled = homeButtonManager.interceptionEnabled.value
        if (!filter.shouldConsume(event, enabled)) {
            return super.onKeyEvent(event)
        }

        if (filter.shouldRouteHome(event, enabled)) {
            mainHandler.post { routeToLauncher() }
        }
        return true
    }

    private fun routeToLauncher() {
        val frontendEnabled = esdePreferencesManager.state.value.frontendEnabled
        routeHome(
            context = applicationContext,
            target = homeButtonManager.resolveHomeTarget(frontendEnabled),
            mainScreen = homeButtonManager.mainScreen.value,
            frontendEnabled = frontendEnabled,
        )
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * @return true iff this service appears in
         * [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES]. Cheaper and more
         * reliable than [isRunning] for pre-flight settings-screen checks, which
         * fire before the service has necessarily bound.
         */
        fun isAccessibilityEnabled(context: Context): Boolean {
            val expected = "${context.packageName}/${HomeInterceptorService::class.java.name}"
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            while (splitter.hasNext()) {
                if (splitter.next().equals(expected, ignoreCase = true)) return true
            }
            return false
        }

        /** Intent that deep-links to the system Accessibility Settings screen. */
        fun accessibilitySettingsIntent(): Intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
