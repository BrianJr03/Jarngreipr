package jr.brian.home.ui.components.settings

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.home.R
import jr.brian.home.data.ExperienceSwitcher
import jr.brian.home.data.JarnFeOffer
import jr.brian.home.ui.meetjarn.MeetJarnDialog
import jr.brian.home.ui.meetjarn.openFromSettings
import jr.brian.home.ui.meetjarn.rememberMeetJarnState
import kotlinx.coroutines.launch

/** Opens the full-screen Meet Jarn preview, whatever its counters say, while the build offers Jarn. */
@Composable
fun SwitchToJarnSettingItem() {
    val context = LocalContext.current
    val isOffered = remember(context) { JarnFeOffer.isOffered(context) }
    if (!isOffered) return

    val meetJarn = rememberMeetJarnState()
    val scope = rememberCoroutineScope()
    SettingItem(
        title = stringResource(R.string.settings_switch_to_jarn_title),
        description = stringResource(R.string.settings_switch_to_jarn_description),
        icon = Icons.Default.SwapHoriz,
        onClick = { if (!meetJarn.isOpen) scope.launch { meetJarn.openFromSettings(context) } },
    )
    MeetJarnDialog(meetJarn)
}

/** Switches through [ExperienceSwitcher], and shows a message if Jarn FE can't open. */
@Composable
fun rememberTryJarnFe(onOpened: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val switcher = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, ExperienceSwitcherEntryPoint::class.java)
            .experienceSwitcher()
    }
    val failedMessage = stringResource(R.string.settings_switch_to_jarn_failed)
    return {
        if (switcher.switchToJarnFe(context)) {
            onOpened()
        } else {
            Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ExperienceSwitcherEntryPoint {
    fun experienceSwitcher(): ExperienceSwitcher
}
