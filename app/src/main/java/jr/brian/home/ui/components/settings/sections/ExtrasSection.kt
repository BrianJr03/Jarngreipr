package jr.brian.home.ui.components.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.components.settings.SettingsSectionHeader
import jr.brian.home.util.OverlayInfoUtil

fun LazyListScope.extrasSection(
    isVisible: (String?) -> Boolean
) {
    item(key = "header_extras") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_extras)
            )
        }
    }

    item(key = "thor_fact") {
        val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            InfoBox(
                label = stringResource(R.string.welcome_overlay_thor_fact_label),
                content = stringResource(randomMessage),
                isPrimary = true,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
