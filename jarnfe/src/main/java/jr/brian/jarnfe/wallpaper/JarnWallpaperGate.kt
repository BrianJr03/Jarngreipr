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
import jr.brian.core.model.JarnGameCover
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.NowVideoVisibility
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.shell.JarnHomeEntry
import jr.brian.jarnfe.splash.JarnSplash

internal data class JarnWallpaperWindow(
    val screen: JarnScreen,
    val isResumed: Boolean,
    val isTopResumed: Boolean,
)

internal enum class JarnWallpaperPause { BatterySaver, ReduceMotion }

internal data class JarnWallpaperConditions(
    val windows: List<JarnWallpaperWindow>,
    val deckDisplayAvailable: Boolean,
    val isScreenOn: Boolean,
    val isBatterySaverOn: Boolean,
    val isMotionReduced: Boolean,
    val isSplashPlaying: Boolean,
    val isGameRunning: Boolean = false,
    val pausesOnBatterySaver: Boolean = true,
    val isVideoPreviewShowing: Boolean = false,
) {
    /**
     * A Deck display without a Deck window (lost, or never opened) cannot prove it is free, so
     * Stage alone counts only while it holds top-resumed and no game is up. Otherwise a window
     * that never comes back would hold every wallpaper still.
     */
    private val hasEveryExpectedWindow: Boolean
        get() {
            val stage = windows.firstOrNull { it.screen == JarnScreen.Stage } ?: return false
            val hasDeck = windows.any { it.screen == JarnScreen.Deck }
            return hasDeck || !deckDisplayAvailable || (stage.isTopResumed && !isGameRunning)
        }

    /**
     * A game covers the Jarn window on its display, which stops only that window, so all of them
     * must be resumed. A game on a display with no Jarn window stops nothing but still takes
     * top-resumed from us.
     */
    fun canAnimate(): Boolean = canPlayVideoPreview() && !isVideoPreviewShowing

    /**
     * The same rule without the preview itself, so a showing preview never stops its own player.
     * A showing preview holds wallpapers still instead, which is not a pause the note explains.
     */
    fun canPlayVideoPreview(): Boolean = pause() == null && !isSplashPlaying && isJarnInFront()

    /**
     * Why wallpapers hold still by the player's own choice or the system's, if they do. Battery
     * Saver counts only while [pausesOnBatterySaver] is on.
     */
    fun pause(): JarnWallpaperPause? = when {
        isBatterySaverOn && pausesOnBatterySaver -> JarnWallpaperPause.BatterySaver
        isMotionReduced -> JarnWallpaperPause.ReduceMotion
        else -> null
    }

    /** False while a game is up, so nothing decodes art behind it. */
    fun isJarnInFront(): Boolean =
        isScreenOn &&
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

    /** Android's Battery Saver, read by the same receiver that gates wallpapers. */
    val batterySaverOn: Boolean get() = isBatterySaverOn

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

    fun allowsAnimation(isMotionReduced: Boolean, pausesOnBatterySaver: Boolean = true): Boolean =
        conditions(isMotionReduced, pausesOnBatterySaver).canAnimate()

    fun allowsVideoPreview(isMotionReduced: Boolean, pausesOnBatterySaver: Boolean = true): Boolean =
        conditions(isMotionReduced, pausesOnBatterySaver).canPlayVideoPreview()

    fun allowsArtWork(): Boolean = conditions(isMotionReduced = false).isJarnInFront()

    fun pause(isMotionReduced: Boolean, pausesOnBatterySaver: Boolean = true): JarnWallpaperPause? =
        conditions(isMotionReduced, pausesOnBatterySaver).pause()

    private fun conditions(isMotionReduced: Boolean, pausesOnBatterySaver: Boolean = true) = JarnWallpaperConditions(
        windows = windows,
        deckDisplayAvailable = JarnSession.state.run { deckDisplayAvailable || deckWindowLost },
        isScreenOn = isScreenOn,
        isBatterySaverOn = isBatterySaverOn,
        isMotionReduced = isMotionReduced,
        isSplashPlaying = JarnSplash.isPlaying,
        isGameRunning = JarnHomeEntry.gameCover != JarnGameCover.None,
        pausesOnBatterySaver = pausesOnBatterySaver,
        isVideoPreviewShowing = NowVideoVisibility.showing,
    )

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

/**
 * True while wallpapers may move. When false they hold their last frame. Only Jarn's own Reduce
 * motion stops them, not Android animations being off. A showing video preview holds them too.
 */
@Composable
internal fun rememberJarnWallpaperCanAnimate(): State<Boolean> = rememberJarnGateState(JarnWallpaperGate::allowsAnimation)

/** For the video preview and the focus float, which keep moving while a preview shows. */
@Composable
internal fun rememberJarnVideoPreviewCanPlay(): State<Boolean> = rememberJarnGateState(JarnWallpaperGate::allowsVideoPreview)

@Composable
private fun rememberJarnGateState(allows: (isMotionReduced: Boolean, pausesOnBatterySaver: Boolean) -> Boolean): State<Boolean> {
    val isMotionReduced by rememberUpdatedState(JarnTheme.motion.ambientReduced)
    val pausesOnBatterySaver by rememberUpdatedState(rememberJarnDisplay().pauseOnBatterySaver)
    return remember { derivedStateOf { allows(isMotionReduced, pausesOnBatterySaver) } }
}

/** The focus float is UI movement, so it also stops when Android animations are off. */
@Composable
internal fun rememberJarnFocusFloatAllowed(): State<Boolean> {
    val canAnimate = rememberJarnVideoPreviewCanPlay()
    val isMotionReduced by rememberUpdatedState(JarnTheme.motion.reduced)
    return remember(canAnimate) { derivedStateOf { canAnimate.value && !isMotionReduced } }
}

/** Why a moving wallpaper is held still right now, for Settings to explain. */
@Composable
internal fun rememberJarnWallpaperPause(): JarnWallpaperPause? {
    val isMotionReduced = JarnTheme.motion.ambientReduced
    return JarnWallpaperGate.pause(isMotionReduced, rememberJarnDisplay().pauseOnBatterySaver)
}
