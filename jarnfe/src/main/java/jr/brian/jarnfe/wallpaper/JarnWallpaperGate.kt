package jr.brian.jarnfe.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.splash.JarnSplash

internal data class JarnWallpaperWindow(
    val screen: JarnScreen,
    val isResumed: Boolean,
    val isTopResumed: Boolean,
)

internal data class JarnWallpaperConditions(
    val windows: List<JarnWallpaperWindow>,
    val deckDisplayAvailable: Boolean,
    val isScreenOn: Boolean,
    val isBatterySaverOn: Boolean,
    val isMotionReduced: Boolean,
    val isSplashPlaying: Boolean,
) {
    private val hasEveryExpectedWindow: Boolean
        get() = windows.any { it.screen == JarnScreen.Stage } &&
            (!deckDisplayAvailable || windows.any { it.screen == JarnScreen.Deck })

    /**
     * A game covers the Jarn window on its display, which stops only that window, so all of them
     * must be resumed. A game on a display with no Jarn window stops nothing but still takes
     * top-resumed from us, and a destroyed window must not read as a free display.
     */
    fun canAnimate(): Boolean =
        isScreenOn &&
            !isBatterySaverOn &&
            !isMotionReduced &&
            !isSplashPlaying &&
            hasEveryExpectedWindow &&
            windows.all { it.isResumed } &&
            windows.any { it.isTopResumed }
}

/** The one rule every live wallpaper obeys. All calls are on the main thread. */
internal object JarnWallpaperGate {
    private val hosts = LinkedHashMap<ComponentActivity, JarnScreen>()
    private val topResumedHosts = HashSet<ComponentActivity>()
    private var windows by mutableStateOf(emptyList<JarnWallpaperWindow>())
    private var isScreenOn by mutableStateOf(false)
    private var isBatterySaverOn by mutableStateOf(false)
    private var watchedContext: Context? = null

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = readSystemState(context)
    }

    fun track(activity: ComponentActivity, screen: JarnScreen) {
        hosts[activity] = screen
        watchSystem(activity.applicationContext)
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_DESTROY -> untrack(activity)
                    Lifecycle.Event.ON_RESUME -> {
                        readSystemState(activity)
                        publishWindows()
                    }
                    else -> publishWindows()
                }
            },
        )
        publishWindows()
    }

    fun onTopResumedChanged(activity: ComponentActivity, isTopResumed: Boolean) {
        if (activity !in hosts) return
        if (isTopResumed) topResumedHosts += activity else topResumedHosts -= activity
        publishWindows()
    }

    fun allowsAnimation(isMotionReduced: Boolean): Boolean =
        JarnWallpaperConditions(
            windows = windows,
            deckDisplayAvailable = JarnSession.state.deckDisplayAvailable,
            isScreenOn = isScreenOn,
            isBatterySaverOn = isBatterySaverOn,
            isMotionReduced = isMotionReduced,
            isSplashPlaying = JarnSplash.isPlaying,
        ).canAnimate()

    private fun untrack(activity: ComponentActivity) {
        hosts.remove(activity)
        topResumedHosts.remove(activity)
        publishWindows()
        if (hosts.isEmpty()) stopWatchingSystem()
    }

    private fun publishWindows() {
        windows = hosts.map { (activity, screen) ->
            JarnWallpaperWindow(
                screen = screen,
                isResumed = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
                isTopResumed = activity in topResumedHosts,
            )
        }
    }

    private fun watchSystem(context: Context) {
        if (watchedContext != null) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(systemReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        watchedContext = context
        readSystemState(context)
    }

    private fun stopWatchingSystem() {
        watchedContext?.unregisterReceiver(systemReceiver)
        watchedContext = null
    }

    private fun readSystemState(context: Context) {
        val power = context.getSystemService(PowerManager::class.java)
        isScreenOn = power.isInteractive
        isBatterySaverOn = power.isPowerSaveMode
    }
}

/** True while wallpapers may move. When false they hold their last frame. */
@Composable
internal fun rememberJarnWallpaperCanAnimate(): State<Boolean> {
    val isMotionReduced by rememberUpdatedState(JarnTheme.motion.reduced)
    return remember { derivedStateOf { JarnWallpaperGate.allowsAnimation(isMotionReduced) } }
}
