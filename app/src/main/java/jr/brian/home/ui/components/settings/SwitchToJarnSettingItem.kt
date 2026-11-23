package jr.brian.home.ui.components.settings

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.home.R
import jr.brian.home.data.ExperienceSwitcher

@Composable
fun SwitchToJarnSettingItem() {
    val context = LocalContext.current
    val switcher = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, ExperienceSwitcherEntryPoint::class.java)
            .experienceSwitcher()
    }
    val failedMessage = stringResource(R.string.settings_switch_to_jarn_failed)
    SettingItem(
        title = stringResource(R.string.settings_switch_to_jarn_title),
        description = stringResource(R.string.settings_switch_to_jarn_description),
        icon = Icons.Default.SwapHoriz,
        onClick = {
            if (!switcher.switchToJarnFe(context)) {
                Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
            }
        },
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ExperienceSwitcherEntryPoint {
    fun experienceSwitcher(): ExperienceSwitcher
}
