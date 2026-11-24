package jr.brian.home.ui.components.settings

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.home.R
import jr.brian.home.data.ExperienceSwitcher
import jr.brian.home.data.JarnFeOffer
import jr.brian.home.ui.components.JarnFePreviewSheet

@Composable
fun SwitchToJarnSettingItem() {
    val context = LocalContext.current
    val isOffered = remember(context) { JarnFeOffer.isOffered(context) }
    if (!isOffered) return

    var showPreview by rememberSaveable { mutableStateOf(false) }
    SettingItem(
        title = stringResource(R.string.settings_switch_to_jarn_title),
        description = stringResource(R.string.settings_switch_to_jarn_description),
        icon = Icons.Default.SwapHoriz,
        onClick = { showPreview = true },
    )
    if (showPreview) {
        JarnFePreviewSheet(onDismiss = { showPreview = false })
    }
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
