package jr.brian.home.ui.components.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Support
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem

@Composable
fun SupportSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNavigateToFAQ: () -> Unit
) {
    CollapsibleSettingsSection(
        title = stringResource(id = R.string.settings_section_support),
        icon = Icons.Default.Support,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        SettingItem(
            title = stringResource(id = R.string.settings_faq_title),
            description = stringResource(id = R.string.settings_faq_description),
            icon = Icons.AutoMirrored.Filled.Help,
            onClick = onNavigateToFAQ
        )
    }
}
