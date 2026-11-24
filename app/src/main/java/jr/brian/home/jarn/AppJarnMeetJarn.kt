package jr.brian.home.jarn

import androidx.compose.runtime.Composable
import jr.brian.home.ui.meetjarn.MeetJarnReplayDialog
import jr.brian.jarnfe.settings.JarnMeetJarn
import javax.inject.Inject
import javax.inject.Singleton

/** Jarn's view-only Meet Jarn, drawn by the same Classic screen as the real offer. */
@Singleton
class AppJarnMeetJarn @Inject constructor() : JarnMeetJarn {
    @Composable
    override fun Replay(onClose: () -> Unit) = MeetJarnReplayDialog(onClose)
}
