package jr.brian.home.ui.components.settings.sections
import android.widget.Toast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.theme.managers.LocalFloatyModeManager
import jr.brian.home.util.OverlayInfoUtil

@Composable
fun ExtrasSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onWhatsNewClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
    val floatyModeManager = LocalFloatyModeManager.current

    CollapsibleSettingsSection(
        title = stringResource(id = R.string.settings_section_extras),
        icon = Icons.Default.Star,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingItem(
                title = stringResource(R.string.settings_whats_new_title),
                description = stringResource(R.string.settings_whats_new_description),
                icon = Icons.Default.NewReleases,
                onClick = onWhatsNewClick
            )

            if (floatyModeManager.isUnlocked) {
                ToggleSetting(
                    title = stringResource(R.string.floaty_mode_toggle_title),
                    description = stringResource(R.string.floaty_mode_toggle_description),
                    checked = floatyModeManager.isFloatyModeActive,
                    onCheckedChange = { floatyModeManager.setFloatyMode(it) }
                )
                SettingItem(
                    title = stringResource(R.string.floaty_mode_reset_title),
                    description = stringResource(R.string.floaty_mode_reset_description),
                    icon = Icons.Default.RestartAlt,
                    onClick = {
                        floatyModeManager.reset()
                        Toast.makeText(
                            context,
                            context.getString(R.string.floaty_mode_reset_done),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            InfoBox(
                label = stringResource(R.string.welcome_overlay_thor_fact_label),
                content = stringResource(randomMessage),
                isPrimary = true,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
