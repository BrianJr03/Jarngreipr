package jr.brian.home.esde.ui.frontend.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import jr.brian.home.R

enum class SystemCustomizationCategory(
    override val id: String,
    @StringRes override val titleRes: Int,
    @StringRes override val summaryRes: Int,
    override val icon: ImageVector
) : RailCategory {
    ACTIONS(
        id = "actions",
        titleRes = R.string.system_customize_category_actions_title,
        summaryRes = R.string.system_customize_category_actions_summary,
        icon = Icons.Rounded.Tune
    ),
    BACKGROUND(
        id = "background",
        titleRes = R.string.system_customize_category_background_title,
        summaryRes = R.string.system_customize_category_background_summary,
        icon = Icons.Rounded.Image
    ),
    COLOR(
        id = "color",
        titleRes = R.string.system_customize_category_color_title,
        summaryRes = R.string.system_customize_category_color_summary,
        icon = Icons.Rounded.Palette
    ),
    EMULATOR(
        id = "emulator",
        titleRes = R.string.system_customize_category_emulator_title,
        summaryRes = R.string.system_customize_category_emulator_summary,
        icon = Icons.Rounded.SportsEsports
    ),
    STORAGE(
        id = "storage",
        titleRes = R.string.system_customize_category_storage_title,
        summaryRes = R.string.system_customize_category_storage_summary,
        icon = Icons.Rounded.FolderOpen
    )
}
