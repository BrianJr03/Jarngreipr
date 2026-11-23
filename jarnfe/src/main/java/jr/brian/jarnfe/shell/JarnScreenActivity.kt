package jr.brian.jarnfe.shell

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.connectJarnLibrary
import jr.brian.jarnfe.session.JarnInput
import jr.brian.jarnfe.session.JarnSession

abstract class JarnScreenActivity : ComponentActivity() {
    protected abstract val screen: JarnScreen

    protected open fun canHostOnCurrentDisplay(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JarnSession.restoreIfFresh(savedInstanceState?.getString(KEY_SESSION))
        JarnLibrary.restoreIfFresh(savedInstanceState?.getString(KEY_OPEN_SYSTEM))
        connectJarnLibrary(this)
        JarnInput.setGlobalBindings(jarnGlobalBindings(JarnSession))
        if (!canHostOnCurrentDisplay()) {
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this) { JarnInput.dispatchSystemBack() }
        setContent {
            val needsHandoff = JarnSession.state.needsFocusHandoff(screen)
            LaunchedEffect(needsHandoff) { if (needsHandoff) bringToFront() }
            JarnShellRoot(screen)
        }
        enterImmersive()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SESSION, JarnSession.state.encode())
        outState.putString(KEY_OPEN_SYSTEM, JarnLibrary.state.openSystemId)
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        JarnSession.update { onTopResumed(screen, isTopResumedActivity) }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        JarnInput.dispatch(event) || super.dispatchKeyEvent(event)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) JarnSession.update { activate(screen) }
        return super.dispatchTouchEvent(event)
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

    private companion object {
        const val KEY_SESSION = "jarnfe_session"
        const val KEY_OPEN_SYSTEM = "jarnfe_open_system"
    }
}
