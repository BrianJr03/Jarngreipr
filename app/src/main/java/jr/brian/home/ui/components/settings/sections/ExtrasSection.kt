package jr.brian.home.ui.components.settings.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.util.OverlayInfoUtil

@Composable
fun ExtrasSection(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
    
    CollapsibleSettingsSection(
        title = stringResource(id = R.string.settings_section_extras),
        icon = Icons.Default.Star,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        InfoBox(
            label = stringResource(R.string.welcome_overlay_thor_fact_label),
            content = stringResource(randomMessage),
            isPrimary = true,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}
