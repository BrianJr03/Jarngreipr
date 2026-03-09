package jr.brian.home.ui.components.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem

@Composable
fun JinglesSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onLaunchJingles: () -> Unit = {}
) {
    CollapsibleSettingsSection(
        title = "Jingles",
        icon = Icons.Default.MusicNote,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingItem(
                title = "Launch Jingles",
                description = "Build your own collection of game jingles",
                icon = Icons.Default.MusicNote,
                onClick = onLaunchJingles
            )
        }
    }
}
