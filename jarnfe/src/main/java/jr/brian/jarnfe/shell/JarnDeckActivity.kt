package jr.brian.jarnfe.shell

import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.jarnDeckCloseKind

class JarnDeckActivity : JarnScreenActivity() {
    override val screen = JarnScreen.Deck

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == hostDisplayId) finishDeliberately()
        }
    }

    private var hostDisplayId: Int? = null
    private var wasCovered = false

    override fun canHostOnCurrentDisplay(): Boolean = display?.displayId != PRIMARY_DISPLAY_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        hostDisplayId = display?.displayId
        JarnDeckWatch.onDeckUp()
        getSystemService(DisplayManager::class.java)
            .registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
    }

    override fun onStart() {
        super.onStart()
        wasCovered = false
    }

    /** A stop with the display still on means something else is now showing there. */
    override fun onStop() {
        super.onStop()
        wasCovered = !isFinishing && !isChangingConfigurations && display?.state == Display.STATE_ON
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!canHostOnCurrentDisplay()) finishDeliberately()
    }

    override fun onDestroy() {
        if (hostDisplayId != null) {
            JarnDeckWatch.onDeckClosed(
                jarnDeckCloseKind(closingDeliberately, isChangingConfigurations, wasCovered),
            )
            getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
        }
        super.onDestroy()
    }

    private fun finishDeliberately() {
        closingDeliberately = true
        finish()
    }
}
