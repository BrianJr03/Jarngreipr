package jr.brian.home.ui.components.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.esde.ui.components.CollapsibleSection
import jr.brian.home.ui.components.settings.ScreenSwapSettingItem

@Composable
fun DualScreenSectionContent(
    onSectionHeaderTap: () -> Unit = {},
) {
    CollapsibleSection(
        title = stringResource(R.string.esde_settings_section_dual_screen),
        onHeaderTap = onSectionHeaderTap,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenSwapSettingItem()
        }
    }
}
