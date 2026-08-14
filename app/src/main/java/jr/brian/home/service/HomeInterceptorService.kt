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
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.routeHome
import javax.inject.Inject

/**
 * Optional accessibility-based override for the hardware Home button.
 *
 * Additive to [jr.brian.home.HomeRouterActivity] — never a replacement. On stock
 * AOSP, `PhoneWindowManager` consumes `KEYCODE_HOME` before input dispatch reaches
 * accessibility key filtering, so this service's `onKeyEvent` is never invoked
 * and Home falls through to the router activity as normal. On the AYN Thor the
 * firmware surfaces the hardware Home button as a plain key event, giving this
 * service a chance to run and route [jr.brian.home.MainActivity] to the external
 * display even when Jarngreipr is not the current default home.
 *
 * See [HomeKeyEventFilter] for the pure filtering logic. This class layers Hilt
 * injection, the ESDE frontend gate, and main-thread dispatch on top.
 */
@AndroidEntryPoint
class HomeInterceptorService : AccessibilityService() {

    @Inject
    lateinit var homeButtonManager: HomeButtonManager

    @Inject
    lateinit var esdePreferencesManager: ESDEPreferencesManager

    private val filter = HomeKeyEventFilter()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
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
        routeHome(
            context = applicationContext,
            frontendEnabled = esdePreferencesManager.state.value.frontendEnabled
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
