package jr.brian.home.esde.ui.frontend.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import jr.brian.home.R

enum class FrontendSettingsCategory(
    override val id: String,
    @StringRes override val titleRes: Int,
    @StringRes override val summaryRes: Int,
    override val icon: ImageVector
) : RailCategory {
    LAYOUT(
        id = "layout",
        titleRes = R.string.frontend_settings_category_layout_title,
        summaryRes = R.string.frontend_settings_category_layout_summary,
        icon = Icons.Rounded.GridView
    ),
    MEDIA(
        id = "media",
        titleRes = R.string.frontend_settings_category_media_title,
        summaryRes = R.string.frontend_settings_category_media_summary,
        icon = Icons.Rounded.Image
    ),
    FEEL(
        id = "feel",
        titleRes = R.string.frontend_settings_category_feel_title,
        summaryRes = R.string.frontend_settings_category_feel_summary,
        icon = Icons.Rounded.Animation
    ),
    SYSTEMS(
        id = "systems",
        titleRes = R.string.frontend_settings_category_systems_title,
        summaryRes = R.string.frontend_settings_category_systems_summary,
        icon = Icons.Rounded.Tune
    )
}
