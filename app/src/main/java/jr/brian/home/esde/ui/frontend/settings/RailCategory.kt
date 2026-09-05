package jr.brian.home.esde.ui.frontend.settings

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

interface RailCategory {
    val id: String
    @get:StringRes val titleRes: Int
    @get:StringRes val summaryRes: Int
    val icon: ImageVector
}
