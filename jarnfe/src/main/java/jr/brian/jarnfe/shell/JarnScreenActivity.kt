package jr.brian.jarnfe.shell

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import jr.brian.core.ui.focus.JarnHaptics
import jr.brian.frontend.look.JarnTapControl
import jr.brian.jarnfe.audio.JarnAudio
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibraryLaunchFocus
import jr.brian.jarnfe.library.connectJarnLibrary
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.search.JarnSearch
import jr.brian.jarnfe.search.openJarnSearch
import jr.brian.jarnfe.session.JarnInput
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.rememberedDeckTab
import jr.brian.jarnfe.session.JarnOsFocusPull
import jr.brian.jarnfe.session.onScreenTapped
import jr.brian.jarnfe.session.onWindowCovered
import jr.brian.jarnfe.splash.JarnSplash
import jr.brian.jarnfe.deck.JarnNowPlaying
import jr.brian.jarnfe.launch.jarnPlaySessionsOrNull
import jr.brian.jarnfe.wallpaper.JarnWallpaperGate

abstract class JarnScreenActivity : ComponentActivity() {
    /** The role this window shows when screens are not swapped. */
    protected abstract val screen: JarnScreen

    /** Set before a finish Jarn chose, so it is never mistaken for Android closing the window. */
    protected var closingDeliberately = false

    private var swap: JarnScreenSwap? = null
    private val buttonHaptic: () -> Unit = { JarnHaptics.tick(window.decorView) }

    protected open fun canHostOnCurrentDisplay(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext
        JarnHaptics.isEnabled = { app.jarnLookStoreOrNull()?.display?.buttonHaptics != false }
        if (savedInstanceState != null) JarnSplash.finish() else JarnSplash.armDeadline()
        JarnSession.restoreIfFresh(savedInstanceState?.getString(KEY_SESSION), rememberedDeckTab())
        JarnLibrary.restoreIfFresh(savedInstanceState?.getString(KEY_OPEN_SYSTEM))
        connectJarnLibrary(this)
        swap = jarnScreenSwapOrNull()
        swap?.let { saved -> JarnSession.update { withSwapRequested(saved.isSwapped) } }
        installGlobalBindings(JarnSession.state.deckWindowLost)
        if (!canHostOnCurrentDisplay()) {
            finish()
            return
        }
        liveWindows += this
        JarnAudio.attach(this)
        JarnWallpaperGate.track(this, screen)
        onBackPressedDispatcher.addCallback(this) { JarnInput.dispatchSystemBack() }
        setContent {
            val role = JarnSession.state.roleOn(screen)
            val needsHandoff = JarnSession.state.needsFocusHandoff(role)
            val isActive = JarnSession.state.activeScreen == role
            val focusManager = LocalFocusManager.current
            val deckMissing = JarnSession.state.deckWindowLost
            LaunchedEffect(deckMissing) { installGlobalBindings(deckMissing) }
            LaunchedEffect(needsHandoff) {
                if (needsHandoff && !JarnHomeEntry.isCoveredByGame(screen)) bringToFront()
            }
            JarnGameFocusEffect(window, screen, isActive)
            LaunchedEffect(isActive) { if (isActive) takeContentFocus(focusManager) }
            LaunchedEffect(swap) {
                swap?.swapped?.collect { JarnSession.update { withSwapRequested(it) } }
            }
            JarnTapControlEffects(role)
            JarnShellRoot(role)
        }
        enterImmersive()
    }

    private fun installGlobalBindings(deckMissing: Boolean) {
        JarnInput.shellTabTarget = { pair ->
            shellTabTarget(pair, JarnSession, JarnLibrary, JarnSearch, JarnInput)
        }
        JarnInput.setGlobalBindings(
            jarnGlobalBindings(
                session = JarnSession,
                onSearch = { openJarnSearch(JarnSession, JarnLibrary) },
                onSwap = swap?.let { it::requestSwap },
                onScreenTab = { pair, delta ->
                    stepScreenTabs(pair, delta, JarnSession, JarnLibrary, JarnSearch, JarnInput)
                },
                deckMissing = deckMissing,
                onBackAtRoot = JarnHomeEntry::yieldToGame,
            ),
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SESSION, JarnSession.state.encode())
        outState.putString(KEY_OPEN_SYSTEM, JarnLibrary.state.openSystemId)
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        val before = JarnSession.state.activeScreen
        JarnSession.update { onTopResumed(screen, isTopResumedActivity) }
        osFocusPull.record(before, JarnSession.state.activeScreen, SystemClock.uptimeMillis())
        JarnWallpaperGate.onTopResumedChanged(this, isTopResumedActivity)
        JarnAudio.onTopResumed(this, isTopResumedActivity)
    }

    override fun onResume() {
        super.onResume()
        JarnHomeEntry.onWindowResumed(screen)
        JarnNowPlaying.onWindowResumed(screen)
        jarnPlaySessionsOrNull()?.onWindowResumed(screen.name)
    }

    /** A covered window cannot take keys, so game launch and return follow Android as usual. */
    override fun onStop() {
        super.onStop()
        JarnSession.update { onWindowCovered() }
        if (!isChangingConfigurations && !isFinishing && display?.state == Display.STATE_ON) {
            JarnHomeEntry.onWindowCovered(screen)
            JarnNowPlaying.onWindowStopped(screen)
        }
        if (!isChangingConfigurations) jarnPlaySessionsOrNull()?.onWindowStopped(screen.name)
    }

    override fun onDestroy() {
        liveWindows.remove(this)
        JarnAudio.detach(this)
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_HOME &&
            jarnExperienceSwitchOrNull()?.handleHomeKey(this, event) == true
        ) {
            return true
        }
        if (JarnSplash.isPlaying && event.isControllerNavigation()) {
            if (event.action == KeyEvent.ACTION_DOWN) JarnSplash.finish()
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) LibraryLaunchFocus.release()
        if (JarnDeckWatch.consumeSwitchScreen(event)) return true
        JarnInput.onButtonPressed = buttonHaptic
        JarnInput.onDirectionPressed = JarnHaptics::armNavigation
        if (JarnInput.dispatch(event)) return true
        val target = activeWindowFor(event)
        return if (target == null || target === this) {
            super.dispatchKeyEvent(event)
        } else {
            target.dispatchToContent(event)
        }
    }

    /**
     * Thor's Focus Lock can pin the controller to one display whichever screen is active, so
     * navigation keys are handed to the active screen's window.
     */
    private fun activeWindowFor(event: KeyEvent): JarnScreenActivity? {
        if (!event.isControllerNavigation()) return null
        val state = JarnSession.state
        if (!state.deckDisplayAvailable) return null
        return liveWindows.firstOrNull { state.roleOn(it.screen) == state.activeScreen }
    }

    @SuppressLint("RestrictedApi")
    private fun dispatchToContent(event: KeyEvent): Boolean {
        if (currentFocus == null) requestContentViewFocus()
        return super.dispatchKeyEvent(event)
    }

    private fun requestContentViewFocus() {
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0)?.requestFocus()
    }

    /** Gives a newly active screen a focused item, so its ring has something to land on. */
    @OptIn(ExperimentalComposeUiApi::class)
    private suspend fun takeContentFocus(focusManager: FocusManager) {
        withFrameNanos { }
        withFrameNanos { }
        requestContentViewFocus()
        focusManager.moveFocus(FocusDirection.Enter)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (JarnSplash.isPlaying) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) JarnSplash.finish()
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN) onTouchDown()
        return super.dispatchTouchEvent(event)
    }

    private fun onTouchDown() {
        LibraryLaunchFocus.release()
        if (JarnHomeEntry.reclaim(screen)) {
            window.setFocusable(true)
            bringToFront()
        }
        val choice = jarnLookStoreOrNull()?.display?.tapControl ?: JarnTapControl.FollowTap
        val bothVisible = JarnScreen.entries.all { role ->
            liveWindows.any { it.screen == role && it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        }
        val before = osFocusPull.take(SystemClock.uptimeMillis())
        JarnSession.update { onScreenTapped(screen, choice, bothVisible, before, JarnInput::hasModalLayer) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) JarnInput.cancelPresses()
    }

    private fun bringToFront() {
        val displayId = display?.displayId ?: return
        val intent = Intent(this, javaClass)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle())
    }

    private fun enterImmersive() {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    internal companion object {
        private const val KEY_SESSION = "jarnfe_session"
        private const val KEY_OPEN_SYSTEM = "jarnfe_open_system"
        private val liveWindows = LinkedHashSet<JarnScreenActivity>()
        private val osFocusPull = JarnOsFocusPull()

        fun finishAllWindows() {
            JarnDeckWatch.onLeavingJarn()
            liveWindows.toList().forEach {
                it.closingDeliberately = true
                it.finish()
            }
        }
    }
}

private fun KeyEvent.isControllerNavigation(): Boolean =
    KeyEvent.isGamepadButton(keyCode) || when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        -> true
        else -> false
    }
