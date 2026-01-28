package jr.brian.home.ui.components.settings.sections

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.components.settings.SettingsSectionHeader

fun LazyListScope.supportSection(
    isVisible: (String?) -> Boolean,
    context: Context,
    onNavigateToFAQ: () -> Unit
) {
    item(key = "header_support") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_support)
            )
        }
    }

    item(key = "coffee") {
        val url = stringResource(R.string.settings_buy_me_coffee_url)
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingItem(
                title = stringResource(id = R.string.settings_buy_me_coffee_title),
                description = stringResource(id = R.string.settings_buy_me_coffee_description),
                icon = Icons.Default.Coffee,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        url.toUri()
                    )
                    context.startActivity(intent)
                },
            )
        }
    }

    item(key = "faq") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingItem(
                title = stringResource(id = R.string.settings_faq_title),
                description = stringResource(id = R.string.settings_faq_description),
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = onNavigateToFAQ,
            )
        }
    }
}
