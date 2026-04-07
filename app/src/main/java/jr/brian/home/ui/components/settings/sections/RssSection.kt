package jr.brian.home.ui.components.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem

@Composable
fun RssSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNavigateToRssSettings: () -> Unit = {}
) {
    CollapsibleSettingsSection(
        title = "RSS",
        icon = Icons.Default.RssFeed,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingItem(
                title = "Manage RSS Feeds",
                description = "Add feeds, set refresh intervals, and browse articles",
                icon = Icons.Default.RssFeed,
                onClick = onNavigateToRssSettings
            )
        }
    }
}
