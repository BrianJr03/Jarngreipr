package jr.brian.home.ui.components.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem

@Composable
fun JinglesSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onLaunchJingles: () -> Unit = {}
) {
    CollapsibleSettingsSection(
        title = stringResource(R.string.jingles_screen_title),
        icon = Icons.Default.MusicNote,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingItem(
                title = stringResource(R.string.jingles_launch_title),
                description = stringResource(R.string.jingles_launch_description),
                icon = Icons.Default.MusicNote,
                onClick = onLaunchJingles
            )
        }
    }
}
