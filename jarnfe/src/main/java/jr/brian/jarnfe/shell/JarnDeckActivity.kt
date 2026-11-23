package jr.brian.jarnfe.shell

import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSession

class JarnDeckActivity : JarnScreenActivity() {
    override val screen = JarnScreen.Deck

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == hostDisplayId) finish()
        }
    }

    private var hostDisplayId: Int? = null

    override fun canHostOnCurrentDisplay(): Boolean = display?.displayId != PRIMARY_DISPLAY_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        hostDisplayId = display?.displayId
        JarnSession.deckHostCreated = true
        getSystemService(DisplayManager::class.java)
            .registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!canHostOnCurrentDisplay()) finish()
    }

    override fun onDestroy() {
        if (hostDisplayId != null) {
            JarnSession.deckHostCreated = false
            getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
        }
        super.onDestroy()
    }
}
