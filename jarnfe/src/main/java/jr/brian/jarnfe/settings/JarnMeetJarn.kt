package jr.brian.jarnfe.settings

import android.content.Context
import androidx.compose.runtime.Composable

/**
 * Shows Classic's Meet Jarn screen from inside Jarn, view only. The screen lives with Classic in
 * the host app, so the app draws it and Jarn only asks for it.
 */
interface JarnMeetJarn {
    /** Shows the screen until Close or B, then calls [onClose]. It never changes any state. */
    @Composable
    fun Replay(onClose: () -> Unit)
}

interface JarnMeetJarnProvider {
    val jarnMeetJarn: JarnMeetJarn
}

fun Context.jarnMeetJarnOrNull(): JarnMeetJarn? =
    (applicationContext as? JarnMeetJarnProvider)?.jarnMeetJarn
